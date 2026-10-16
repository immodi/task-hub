package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Error responses should be RFC 9457 {@code application/problem+json}, not ad-hoc JSON. */
class ProblemDetailsApiTest extends AbstractApiTest {

    @Test
    void notFoundIsProblemJson() {
        Session alice = registerUser("alice");

        Resp r = get("/api/projects/999999", alice.token());

        assertThat(r.status()).isEqualTo(404);
        assertThat(r.contentType()).contains("application/problem+json");
        assertThat((String) r.json("$.title")).isEqualTo("Not Found");
        assertThat((String) r.json("$.detail")).contains("999999");
        assertThat((String) r.json("$.instance")).isEqualTo("/api/projects/999999");
        assertThat((Object) r.json("$.timestamp")).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void validationFailureCarriesAFieldErrorMap() {
        Session alice = registerUser("alice");

        Resp r = post("/api/projects", alice.token(), "{\"name\":\"\"}");

        assertThat(r.status()).isEqualTo(400);
        assertThat(r.contentType()).contains("application/problem+json");
        Map<String, String> errors = r.json("$.errors");
        assertThat(errors).containsKey("name");
    }

    @Test
    void unprocessableTransitionIsProblemJson() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        long tid = createTask(alice, pid, "{\"title\":\"a\"}");

        Resp r = patch("/api/tasks/" + tid + "/status", alice.token(), "{\"status\":\"DONE\"}");

        assertThat(r.status()).isEqualTo(422);
        assertThat(r.contentType()).contains("application/problem+json");
        assertThat((String) r.json("$.detail")).contains("TODO");
    }
}
