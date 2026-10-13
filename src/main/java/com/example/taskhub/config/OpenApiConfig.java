package com.example.taskhub.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.taskhub.security.AuthUser;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Swagger UI lives at /swagger-ui.html. Use the "Authorize" button with the token from POST /api/auth/login. */
@Configuration
public class OpenApiConfig {

    static {
        // AuthUser is filled from the JWT by our argument resolver, so it is not a request parameter.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(AuthUser.class);
    }

    @Bean
    OpenAPI taskHubOpenApi() {
        return new OpenAPI()
                .info(new Info().title("TaskHub API").version("v1")
                        .description("Log in as alice / bob / admin (password Password1), copy the token, click Authorize."))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
