package com.example.taskhub.security;

/** The authenticated caller, extracted from the JWT. Like a slim ClaimsPrincipal. */
public record AuthUser(Long id, String username, boolean admin) {
}
