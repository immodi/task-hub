package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Actuator endpoints: health is public, info is filled in from the build. */
class ActuatorApiTest extends AbstractApiTest {

    @Test
    void healthIsPublic() {
        Resp r = get("/actuator/health", null);

        assertThat(r.status()).isEqualTo(200);
        assertThat((String) r.json("$.status")).isEqualTo("UP");
    }

    @Test
    void infoIsPopulatedFromMavenProperties() {
        Resp r = get("/actuator/info", null);

        assertThat(r.status()).isEqualTo(200);
        assertThat((String) r.json("$.app.name")).isEqualTo("TaskHub");
        // Proves resource filtering of application.yml works (@project.version@).
        assertThat((String) r.json("$.app.version")).isEqualTo("0.0.1-SNAPSHOT");
    }

    @Test
    void swaggerUiIsServed() {
        Resp r = get("/swagger-ui/index.html", null);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).contains("Swagger");
    }
}
