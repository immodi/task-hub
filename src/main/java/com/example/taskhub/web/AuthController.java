package com.example.taskhub.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskhub.dto.AuthDtos.AuthResponse;
import com.example.taskhub.dto.AuthDtos.LoginRequest;
import com.example.taskhub.dto.AuthDtos.RegisterRequest;
import com.example.taskhub.dto.CommonDtos.UserDto;
import com.example.taskhub.security.AuthUser;
import com.example.taskhub.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Auth", description = "Registration, login and the current user")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {   // constructor injection, no [FromServices] needed
        this.auth = auth;
    }

    @Operation(summary = "Register a new user",
            description = "Creates a user with the USER role and returns a token for it.")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid username, email or password")
    @ApiResponse(responseCode = "409", description = "Username or email already taken")
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @Operation(summary = "Log in", description = "Exchanges a username and password for a JWT.")
    @ApiResponse(responseCode = "200", description = "Token issued")
    @ApiResponse(responseCode = "401", description = "Invalid username or password")
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @Operation(summary = "Get the current user", description = "Resolves the caller from the bearer token.")
    @ApiResponse(responseCode = "200", description = "The authenticated user")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @GetMapping("/me")
    public UserDto me(AuthUser actor) {
        return auth.me(actor);
    }
}
