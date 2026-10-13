package com.example.taskhub.dto;

import java.time.Instant;

import com.example.taskhub.security.StrongPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "only letters, digits, _ . - allowed") String username,
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @StrongPassword String password) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record AuthResponse(String token, Instant expiresAt, CommonDtos.UserDto user) {}
}
