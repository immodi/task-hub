package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Admin-only behaviour that is easy to get wrong: role validation and unknown-target handling. */
class AdminApiTest extends AbstractApiTest {

    @Test
    void missingRoleIsAValidationError() {
        Session admin = adminUser();
        Session alice = registerUser("alice");

        Resp r = patch("/api/admin/users/" + alice.id() + "/role", admin.token(), "{}");

        assertThat(r.status()).isEqualTo(400);
        assertThat((String) r.json("$.errors.role")).isNotBlank();
    }

    @Test
    void unknownRoleValueIsRejected() {
        Session admin = adminUser();
        Session alice = registerUser("alice");

        Resp r = patch("/api/admin/users/" + alice.id() + "/role", admin.token(), "{\"role\":\"SUPERADMIN\"}");

        assertThat(r.status()).isEqualTo(400);
    }

    @Test
    void changingRoleOfUnknownUserIs404() {
        Session admin = adminUser();

        Resp r = patch("/api/admin/users/999999/role", admin.token(), "{\"role\":\"ADMIN\"}");

        assertThat(r.status()).isEqualTo(404);
    }
}
