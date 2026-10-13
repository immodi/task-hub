package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AuthApiTest extends AbstractApiTest {

    @Test
    void registerThenLoginThenMe() {
        Session s = registerUser("carol");

        Resp login = post("/api/auth/login", null,
                "{\"username\":\"%s\",\"password\":\"Password1\"}".formatted(s.username()));
        assertThat(login.status()).isEqualTo(200);
        String token = login.json("$.token");

        Resp me = get("/api/auth/me", token);
        assertThat(me.status()).isEqualTo(200);
        assertThat((String) me.json("$.username")).isEqualTo(s.username());
        assertThat((String) me.json("$.role")).isEqualTo("USER");
    }

    @Test
    void weakPasswordReturnsFieldErrors() {
        Resp r = post("/api/auth/register", null,
                "{\"username\":\"%s\",\"email\":\"x@test.dev\",\"password\":\"short\"}".formatted(unique("weak")));

        assertThat(r.status()).isEqualTo(400);
        Map<String, String> errors = r.json("$.errors");
        assertThat(errors).containsKey("password");
    }

    @Test
    void invalidEmailAndBlankUsernameAreBothReported() {
        Resp r = post("/api/auth/register", null, "{\"username\":\"\",\"email\":\"nope\",\"password\":\"Password1\"}");

        assertThat(r.status()).isEqualTo(400);
        Map<String, String> errors = r.json("$.errors");
        assertThat(errors).containsKeys("username", "email");
    }

    @Test
    void duplicateUsernameIsConflict() {
        Session s = registerUser("dup");

        Resp again = post("/api/auth/register", null,
                "{\"username\":\"%s\",\"email\":\"other@test.dev\",\"password\":\"Password1\"}".formatted(s.username()));

        assertThat(again.status()).isEqualTo(409);
    }

    @Test
    void wrongPasswordIsUnauthorized() {
        Session s = registerUser("wrongpw");

        Resp r = post("/api/auth/login", null, "{\"username\":\"%s\",\"password\":\"Nope12345\"}".formatted(s.username()));

        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    void protectedEndpointsRequireAToken() {
        assertThat(get("/api/projects", null).status()).isEqualTo(401);
        assertThat(get("/api/projects", "not-a-real-jwt").status()).isEqualTo(401);
    }

    @Test
    void healthAndSwaggerAreOpen() {
        assertThat(get("/actuator/health", null).status()).isEqualTo(200);
        assertThat(get("/v3/api-docs", null).status()).isEqualTo(200);
    }

    @Test
    void adminEndpointsAreRoleProtected() {
        Session user = registerUser("plain");
        Session admin = adminUser();

        assertThat(get("/api/admin/users", user.token()).status()).isEqualTo(403);
        assertThat(get("/api/admin/users", admin.token()).status()).isEqualTo(200);
    }
}
