package com.example.taskhub.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.User;
import com.example.taskhub.dto.AuthDtos.AuthResponse;
import com.example.taskhub.dto.AuthDtos.LoginRequest;
import com.example.taskhub.dto.AuthDtos.RegisterRequest;
import com.example.taskhub.dto.CommonDtos.UserDto;
import com.example.taskhub.dto.Mappers;
import com.example.taskhub.exception.ApiException;
import com.example.taskhub.repository.UserRepository;
import com.example.taskhub.security.AuthUser;
import com.example.taskhub.security.JwtService;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByUsernameIgnoreCase(req.username())) {
            throw ApiException.conflict("Username already taken");
        }
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("Email already registered");
        }
        User user = new User();
        user.setUsername(req.username().trim());
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(encoder.encode(req.password()));
        users.saveAndFlush(user);
        return issue(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByUsernameIgnoreCase(req.username())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
        return issue(user);
    }

    @Transactional(readOnly = true)
    public UserDto me(AuthUser actor) {
        return users.findById(actor.id()).map(Mappers::userDto)
                .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
    }

    private AuthResponse issue(User user) {
        JwtService.IssuedToken token = jwt.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), Mappers.userDto(user));
    }
}
