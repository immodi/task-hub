package com.example.taskhub.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Strongly typed config, like IOptions<T>. Bound from the "taskhub.security" section. */
@ConfigurationProperties(prefix = "taskhub.security")
public record SecurityProperties(String jwtSecret, @DefaultValue("PT1H") Duration tokenTtl) {
}
