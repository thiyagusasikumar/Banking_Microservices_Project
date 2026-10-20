package com.banking.authservice.controller;


import com.banking.authservice.Service.AuthService;
import com.banking.authservice.dto.AdminUserResponse;
import com.banking.authservice.dto.RegisterEmployeeRequest;
import com.banking.authservice.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/admin")
public class AdminUserController {
    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(@RequestParam(required = false)String query,@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20")int size,@RequestParam(defaultValue = "id") String sortBy,
                                                            @RequestParam(defaultValue = "asc") String direction){

        return ResponseEntity.ok(authService.searchUsers(query, page, size, sortBy, direction));
    }
    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long id){
        authService.deactivateUserByAdmin(id);
        return ResponseEntity.ok("Deactivated");
    }
    @PostMapping("/users/{id}/activate")
    public ResponseEntity<String> activateUser(@PathVariable Long id){
        authService.activateUserByAdmin(id);
        return ResponseEntity.ok("Activated");
    }
    @GetMapping("/users/{id}")
    public AdminUserResponse getUser(@PathVariable Long id) {
        return authService.getUser(id);
    }

    @PostMapping("/employees")
    public ResponseEntity<RegisterResponse> createEmployee(@Valid @RequestBody RegisterEmployeeRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEmployee(request));
    }

}
