package com.example.taskhub.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.taskhub.security.AuthUser;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;

/**
 * OpenAPI/Swagger metadata. The UI lives at {@code /swagger-ui.html}; the raw document is
 * served at {@code /v3/api-docs}. Use the "Authorize" button with the token from
 * {@code POST /api/auth/login}.
 */
@Configuration
public class OpenApiConfig {

    /** Name of the security scheme, referenced by the global requirement below. */
    public static final String BEARER_SCHEME = "bearer-jwt";

    static {
        // AuthUser is filled from the JWT by our argument resolver, so it is not a request parameter.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(AuthUser.class);
    }

    @Bean
    OpenAPI taskHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskHub API")
                        .version("v1")
                        .description("""
                                A task and project management REST API: projects with members, tasks with tags
                                and a status workflow, comments, and an asynchronous activity log.

                                **Try it out**

                                1. `POST /api/auth/login` with `{"username":"alice","password":"Password1"}`.
                                2. Copy the `token` from the response.
                                3. Click **Authorize** and paste the token.

                                Seeded users (password `Password1`): `admin` (ADMIN), `alice` and `bob` (USER).
                                """)
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .addServersItem(new Server()
                        .url("http://localhost:{port}")
                        .description("Local development")
                        .variables(new ServerVariables().addServerVariable("port",
                                new ServerVariable()._default("9090").description("Server port"))))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the `token` returned by POST /api/auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
