package com.example.taskhub.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.example.taskhub.config.SecurityProperties;
import com.example.taskhub.domain.User;

@Service
public class JwtService {

    public record IssuedToken(String value, Instant expiresAt) {}

    private final JwtEncoder encoder;
    private final SecurityProperties props;

    public JwtService(JwtEncoder encoder, SecurityProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expires = now.plus(props.tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("taskhub")
                .issuedAt(now)
                .expiresAt(expires)
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expires);
    }
}
