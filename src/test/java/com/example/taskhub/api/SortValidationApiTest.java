package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The sort property in {@code ?sort=...} used to be passed straight to Spring Data, so a typo
 * blew up the query and returned a 500. It is now validated up front and returns a clear 400.
 */
class SortValidationApiTest extends AbstractApiTest {

    @Test
    void unknownTaskSortFieldReturns400AndKnownOneWorks() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        createTask(alice, pid, "{\"title\":\"a\"}");
        String base = "/api/projects/" + pid + "/tasks";

        Resp bad = get(base + "?sort=bogus", alice.token());
        assertThat(bad.status()).isEqualTo(400);
        assertThat((String) bad.json("$.detail")).contains("bogus");

        assertThat(get(base + "?sort=title,asc", alice.token()).status()).isEqualTo(200);
        assertThat(get(base + "?sort=assignee.username,asc", alice.token()).status()).isEqualTo(200);
    }

    @Test
    void unknownSortFieldIsRejectedAcrossListEndpoints() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        long tid = createTask(alice, pid, "{\"title\":\"a\"}");

        assertThat(get("/api/projects?sort=owner", alice.token()).status()).isEqualTo(400);
        assertThat(get("/api/tasks/" + tid + "/comments?sort=body", alice.token()).status()).isEqualTo(400);
    }

    @Test
    void unknownSortFieldOnAdminEndpointsReturns400() {
        Session admin = adminUser();

        assertThat(get("/api/admin/users?sort=passwordHash", admin.token()).status()).isEqualTo(400);
        assertThat(get("/api/admin/activity?sort=secret", admin.token()).status()).isEqualTo(400);
    }
}
