package com.banking.authservice.mapper;


import com.banking.authservice.dto.*;
import com.banking.authservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserUpdateResponse toUserUpdateResponse(User user);

    CurrentUserResponse toCurrentUserResponse(User user);

    LoginResponse toLoginResponse(KeycloakTokenResponse tokenResponse);

    RegisterResponse toRegisterResponse(User user);
}
