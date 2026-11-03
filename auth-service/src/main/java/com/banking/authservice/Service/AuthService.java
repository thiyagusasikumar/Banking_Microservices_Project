package com.banking.authservice.Service;


import com.banking.authservice.constants.EmployeeRoles;
import com.banking.authservice.constants.RegistrationAttemptStatus;
import com.banking.authservice.constants.RegistrationType;
import com.banking.authservice.constants.Roles;
import com.banking.authservice.constants.UserStatus;
import com.banking.authservice.dto.*;
import com.banking.authservice.entity.RegistrationAttempt;
import com.banking.authservice.entity.User;
import com.banking.authservice.exception.AccountNotActiveException;
import com.banking.authservice.exception.DuplicateUserException;
import com.banking.authservice.exception.AuthenticationFailedException;
import com.banking.authservice.exception.InvalidCredentialsException;
import com.banking.authservice.exception.KeyCloakCreationException;


import com.banking.authservice.mapper.UserMapper;
import com.banking.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id","userName","email","role","status");

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.token-url}")
    private String tokenUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final RestClient restClient;
    private final UserMapper userMapper;
    private final RegistrationAttemptService registrationAttemptService;
    private final RegistrationReconciliationService registrationReconciliationService;


    public RegisterResponse register(RegisterRequest request) {
        RegistrationAttempt attempt = findOrStartRegistrationAttempt(
                request.getUserName(),
                request.getEmail(),
                RegistrationType.CUSTOMER,
                Roles.CUSTOMER
        );

        Optional<UserRepresentation> keycloakUser=keycloakUserService.findUserByRegistrationAttempt(attempt.getAttemptId());
        if(keycloakUser.isPresent()){
           keycloakUserService.verifyCredentials(
                   request.getUserName(),
                   request.getPassword()
           );
           return registrationReconciliationService.reconcileAttempt(attempt);
        }
        String keycloakUserId;
        try {
            keycloakUserId = keycloakUserService.createUser(
                    request,
                    attempt.getAttemptId()
            );
        } catch (DuplicateUserException ex) {
            return reconcileDuplicateRegistration(
                    attempt,
                    ex,
                    request.getUserName(),
                    request.getPassword(),
                    true
            );
        }
        registrationAttemptService.updateKeycloakUserId(
                attempt.getAttemptId(),
                keycloakUserId
        );

        User user= saveUser(keycloakUserId, request.getUserName(), request.getEmail(),
                request.getFirstName(), request.getLastName(), request.getPhoneNumber(), Roles.CUSTOMER);

         updateCustomerId(user,attempt);
        return  userMapper.toRegisterResponse(user);
    }

    public RegisterResponse registerEmployee(RegisterEmployeeRequest request) {
        EmployeeRoles role;
        try {
            role = EmployeeRoles.valueOf(
                    request.getRole().trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Invalid employee role");
        }
       RegistrationAttempt attempt = findOrStartRegistrationAttempt(
               request.getUserName(),
               request.getEmail(),
               RegistrationType.EMPLOYEE,
               Roles.valueOf(role.name())
       );

       Optional<UserRepresentation> keycloakUser =
               keycloakUserService.findUserByRegistrationAttempt(attempt.getAttemptId());
       if (keycloakUser.isPresent()) {
           return registrationReconciliationService.reconcileAttempt(attempt);
       }

       String keycloakUserId;
       try {
           keycloakUserId = keycloakUserService.createEmployee(
                   request,
                   role,
                   attempt.getAttemptId()
           );
       } catch (DuplicateUserException ex) {
           return reconcileDuplicateRegistration(
                   attempt,
                   ex,
                   request.getUserName(),
                   request.getPassword(),
                   false
           );
       }
       registrationAttemptService.updateKeycloakUserId(
               attempt.getAttemptId(),
               keycloakUserId
       );
       User user=saveUser(keycloakUserId, request.getUserName(), request.getEmail(),
               request.getFirstName(), request.getLastName(), request.getPhoneNumber(),
               Roles.valueOf(role.name()));

        updateCustomerId(user,attempt);

       return userMapper.toRegisterResponse(user);
    }

    private RegistrationAttempt findOrStartRegistrationAttempt(
           String userName,
          String email,
          RegistrationType registrationType,
          Roles registrationRole
    ) {
       Optional<RegistrationAttempt> incompleteAttempt =
              registrationAttemptService.findIncompleteAttempt(userName, registrationType);
       if (incompleteAttempt.isPresent()) {
           return incompleteAttempt.get();
       }

       userIsAvailable(userName, email);
       return registrationAttemptService.startRegistrationAttempt(
               userName,
               registrationType,
               registrationRole
       );
    }

    private RegisterResponse reconcileDuplicateRegistration(
           RegistrationAttempt attempt,
           DuplicateUserException exception,
           String userName,
           String password,
           boolean verifyCredentials
    ) {
       if (keycloakUserService.findUserByRegistrationAttempt(attempt.getAttemptId()).isPresent()) {
           if (verifyCredentials) {
               keycloakUserService.verifyCredentials(userName, password);
           }
           return registrationReconciliationService.reconcileAttempt(attempt);
       }

       registrationAttemptService.updateStatus(
               attempt.getAttemptId(),
               RegistrationAttemptStatus.FAILED
       );
       throw exception;
    }

    private void userIsAvailable(String userName, String email) {
        if (userRepository.findByUserName(userName) != null) {
            throw new DuplicateUserException("Username already exists");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException("Email already exists");
        }
    }

    private User saveUser(
            String keycloakUserId,
            String username,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            Roles role) {
        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setUserName(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        return userRepository.save(user);
    }
    public void updateCustomerId(User user, RegistrationAttempt attempt) {
        Long customerId = user.getId();
        String keycloakUserId=user.getKeycloakUserId();

        registrationAttemptService.updateLocalUserId(attempt.getAttemptId(), customerId);

        keycloakUserService.updateCustomerIdAttribute(customerId,keycloakUserId);

        registrationAttemptService.updateStatus(
                attempt.getAttemptId(),
                RegistrationAttemptStatus.COMPLETED
        );
    }

    public LoginResponse login(LoginRequest loginRequest)  {
         User user=userRepository.findByUserName(loginRequest.getUserName());

         if (user == null) {
             throw new AuthenticationFailedException("Invalid username or password");
         }
         if(user.getStatus().equals(UserStatus.INACTIVE)){
             throw new AccountNotActiveException();
         }
         MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
         formData.add("grant_type", "password");
         formData.add("client_id",clientId);
         formData.add("client_secret",clientSecret);
         formData.add("username", loginRequest.getUserName());
         formData.add("password", loginRequest.getPassword());
         try{
             KeycloakTokenResponse tokenResponse=restClient.post().uri(tokenUrl).
                     contentType(MediaType.APPLICATION_FORM_URLENCODED).
                     body(formData).
                     retrieve().
                     body(KeycloakTokenResponse.class);

             if(tokenResponse==null || tokenResponse.getAccessToken()==null){
                 throw new RestClientException("Keycloak returned an invalid token response");
             }
             return userMapper.toLoginResponse(tokenResponse);

         }
         catch (HttpClientErrorException.BadRequest
                | HttpClientErrorException.Unauthorized ex) {

             throw new AuthenticationFailedException(
                     "Invalid username or password");

         }
    }
    public UserUpdateResponse userUpdate(String keycloakUserId, UserUpdateRequest updateRequest) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (updateRequest.getEmail() != null
                && !updateRequest.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.findByEmail(updateRequest.getEmail()).isPresent()) {
            throw new DuplicateUserException("Email is already in use");
        }

        keycloakUserService.updateProfile(keycloakUserId, updateRequest);

        if(updateRequest.getEmail()!=null){
            user.setEmail(updateRequest.getEmail());
        }
        if(updateRequest.getFirstName()!=null){
            user.setFirstName(updateRequest.getFirstName());
        }
        if(updateRequest.getLastName()!=null){
            user.setLastName(updateRequest.getLastName());
        }
        if(updateRequest.getPhoneNumber()!=null){
            user.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        return userMapper.toUserUpdateResponse(userRepository.save(user));
    }
    public void logout(String token) {
        String logoutUrl = serverUrl
                + "/realms/" + realm
                + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("client_id", clientId);
        form.add("refresh_token", token);

        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        try {
            restClient.post()
                    .uri(logoutUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.BadRequest
                 | HttpClientErrorException.Unauthorized ex) {
            throw new AuthenticationFailedException("Refresh token is invalid or expired");
        }
    }
    public CurrentUserResponse getCurrentUser(String keycloakUserId)
    {
        User user=userRepository.findByKeycloakUserId(keycloakUserId).orElseThrow(()->new UsernameNotFoundException("User not found"));
        return userMapper.toCurrentUserResponse(user);
    }
    public AdminUserResponse getUser(Long id){
        User user=userRepository.findById(id).orElseThrow(()->new UsernameNotFoundException("User not found"));
        return userMapper.toAdminUserResponse(user);
    }
    public void deactivateUser(String keycloakUserId)
    {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        updateUserStatus(user, UserStatus.INACTIVE);
    }

    public void activateUserByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        updateUserStatus(user, UserStatus.ACTIVE);
    }

    public void deactivateUserByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        updateUserStatus(user, UserStatus.INACTIVE);
    }

    private void updateUserStatus(User user, UserStatus newStatus) {
        UserStatus previousStatus = user.getStatus();
        if (previousStatus == newStatus) {
            throw new InvalidCredentialsException(
                    newStatus == UserStatus.ACTIVE ? "User is active" : "User is inactive"
            );
        }

        String keycloakUserId = user.getKeycloakUserId();
        try {
            setKeycloakStatus(keycloakUserId, newStatus);
        } catch (KeyCloakCreationException ex) {
            log.error(
                    "Could not change Keycloak account status; userId={}, status={}",
                    keycloakUserId,
                    newStatus,
                    ex
            );
            throw ex;
        }

        user.setStatus(newStatus);
        try {
            userRepository.save(user);
        } catch (DataAccessException ex) {
            user.setStatus(previousStatus);
            log.error(
                    "Could not persist account status; userId={}, status={}",
                    keycloakUserId,
                    newStatus,
                    ex
            );
            try {
                setKeycloakStatus(keycloakUserId, previousStatus);
            } catch (RuntimeException compensationFailure) {
                ex.addSuppressed(compensationFailure);
                log.error(
                        "Could not restore Keycloak status after local status update failed; userId={}",
                        keycloakUserId,
                        compensationFailure
                );
            }
            throw ex;
        }
    }

    private void setKeycloakStatus(String keycloakUserId, UserStatus status) {
        if (status == UserStatus.ACTIVE) {
            keycloakUserService.activateUser(keycloakUserId);
        } else {
            keycloakUserService.deactivateUser(keycloakUserId);
        }
    }
    public Page<AdminUserResponse> searchUsers(String query,int page,int size,String sortBy,String direction){
      if(page < 0)
      {
          throw new IllegalArgumentException("page must not be negative");
      }
      if(size < 1 || size> 100)
      {
          throw new IllegalArgumentException("size must be between 1 and 100");
      }
      if(!ALLOWED_SORT_FIELDS.contains(sortBy))
      {
          throw new IllegalArgumentException("sort fields are invalid"+sortBy);
      }
        Sort.Direction sortDirection;
      try{
          sortDirection=Sort.Direction.fromString(direction);
      }
      catch (IllegalArgumentException e){
          throw new IllegalArgumentException("Invalid sort direction");
      }
        Pageable pageable= PageRequest.of(page,size,Sort.by(sortDirection,sortBy));

      Page<User> users=userRepository.searchUsers(query==null || query.isBlank()?null:query.trim(),pageable);

       return  users.map(user -> new AdminUserResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getStatus()
        ));
    }
}
