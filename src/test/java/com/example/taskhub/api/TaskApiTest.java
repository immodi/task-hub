package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class TaskApiTest extends AbstractApiTest {

    @Test
    void createAppliesDefaultsNormalizesTagsAndSetsReporter() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");

        Resp r = post("/api/projects/" + pid + "/tasks", alice.token(),
                "{\"title\":\"  Write docs \",\"tags\":[\"Docs\",\" backend \"]}");

        assertThat(r.status()).isEqualTo(201);
        assertThat((String) r.json("$.title")).isEqualTo("Write docs");
        assertThat((String) r.json("$.status")).isEqualTo("TODO");
        assertThat((String) r.json("$.priority")).isEqualTo("MEDIUM");
        assertThat(r.<List<String>>json("$.tags")).containsExactly("backend", "docs");
        assertThat((String) r.json("$.reporter.username")).isEqualTo(alice.username());
    }

    @Test
    void validationRejectsBadInput() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        String yesterday = LocalDate.now().minusDays(1).toString();

        assertThat(post("/api/projects/" + pid + "/tasks", alice.token(), "{\"title\":\"\"}").status()).isEqualTo(400);
        assertThat(post("/api/projects/" + pid + "/tasks", alice.token(),
                "{\"title\":\"x\",\"dueDate\":\"%s\"}".formatted(yesterday)).status()).isEqualTo(400);
        assertThat(post("/api/projects/" + pid + "/tasks", alice.token(),
                "{\"title\":\"x\",\"priority\":\"URGENT\"}").status()).isEqualTo(400);
    }

    @Test
    void assigneeMustBeProjectMember() {
        Session alice = registerUser("alice");
        Session stranger = registerUser("stranger");
        long pid = createProject(alice, "P");

        Resp r = post("/api/projects/" + pid + "/tasks", alice.token(),
                "{\"title\":\"x\",\"assigneeId\":%d}".formatted(stranger.id()));

        assertThat(r.status()).isEqualTo(400);
    }

    @Test
    void statusWorkflowIsEnforced() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        long tid = createTask(alice, pid, "{\"title\":\"Ship\"}");
        String url = "/api/tasks/" + tid + "/status";

        assertThat(patch(url, alice.token(), "{\"status\":\"DONE\"}").status()).isEqualTo(422);
        assertThat(patch(url, alice.token(), "{\"status\":\"IN_PROGRESS\"}").status()).isEqualTo(200);
        Resp done = patch(url, alice.token(), "{\"status\":\"DONE\"}");
        assertThat(done.status()).isEqualTo(200);
        assertThat((String) done.json("$.status")).isEqualTo("DONE");
        assertThat(patch(url, alice.token(), "{\"status\":\"BLOCKED\"}").status()).isEqualTo(422);
        assertThat(patch(url, alice.token(), "{}").status()).isEqualTo(400);
    }

    @Test
    void searchFiltersPagesAndSorts() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        createTask(alice, pid, "{\"title\":\"Fix login bug\",\"description\":\"oauth redirect\",\"priority\":\"HIGH\",\"tags\":[\"bug\"]}");
        createTask(alice, pid, "{\"title\":\"Write README\",\"priority\":\"LOW\",\"tags\":[\"docs\"]}");
        long third = createTask(alice, pid, "{\"title\":\"Beta release\",\"priority\":\"HIGH\",\"tags\":[\"bug\",\"docs\"]}");
        patch("/api/tasks/" + third + "/status", alice.token(), "{\"status\":\"IN_PROGRESS\"}");
        String base = "/api/projects/" + pid + "/tasks";

        assertThat(get(base + "?tag=BUG", alice.token()).<Integer>json("$.totalItems")).isEqualTo(2);
        assertThat(get(base + "?priority=HIGH&status=IN_PROGRESS", alice.token()).<List<String>>json("$.items[*].title"))
                .containsExactly("Beta release");
        assertThat(get(base + "?q=oauth", alice.token()).<List<String>>json("$.items[*].title"))
                .containsExactly("Fix login bug");
        assertThat(get(base + "?q=readme", alice.token()).<List<String>>json("$.items[*].title"))
                .containsExactly("Write README");

        Resp page = get(base + "?size=2&page=1&sort=title,asc", alice.token());
        assertThat(page.<Integer>json("$.totalItems")).isEqualTo(3);
        assertThat(page.<Integer>json("$.totalPages")).isEqualTo(2);
        assertThat(page.<List<String>>json("$.items[*].title")).containsExactly("Write README");
    }

    @Test
    void overdueFilterIgnoresDoneTasksAndFutureDates() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        long overdue = createTask(alice, pid, "{\"title\":\"Late\"}");
        createTask(alice, pid, "{\"title\":\"On time\",\"dueDate\":\"%s\"}".formatted(LocalDate.now().plusDays(3)));
        // creation rejects past dates, so make one overdue through a full update (PUT has no future-date rule)
        Resp upd = put("/api/tasks/" + overdue, alice.token(),
                "{\"title\":\"Late\",\"priority\":\"HIGH\",\"dueDate\":\"%s\"}".formatted(LocalDate.now().minusDays(2)));
        assertThat(upd.status()).isEqualTo(200);
        assertThat(upd.<Boolean>json("$.overdue")).isTrue();

        Resp r = get("/api/projects/" + pid + "/tasks?overdue=true", alice.token());

        assertThat(r.<List<String>>json("$.items[*].title")).containsExactly("Late");

        patch("/api/tasks/" + overdue + "/status", alice.token(), "{\"status\":\"IN_PROGRESS\"}");
        patch("/api/tasks/" + overdue + "/status", alice.token(), "{\"status\":\"DONE\"}");
        assertThat(get("/api/projects/" + pid + "/tasks?overdue=true", alice.token()).<Integer>json("$.totalItems")).isEqualTo(0);
    }

    @Test
    void updateWithStaleVersionIsConflict() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        long tid = createTask(alice, pid, "{\"title\":\"Edit me\"}");
        String body = "{\"title\":\"%s\",\"priority\":\"LOW\",\"version\":0}";

        Resp first = put("/api/tasks/" + tid, alice.token(), body.formatted("First"));
        Resp second = put("/api/tasks/" + tid, alice.token(), body.formatted("Second"));

        assertThat(first.status()).isEqualTo(200);
        assertThat(first.<Integer>json("$.version")).isGreaterThan(0);
        assertThat(second.status()).isEqualTo(409);
    }

    @Test
    void assignAndUnassign() {
        Session alice = registerUser("alice");
        Session bob = registerUser("bob");
        long pid = createProject(alice, "P");
        put("/api/projects/" + pid + "/members/" + bob.id(), alice.token(), null);
        long tid = createTask(alice, pid, "{\"title\":\"Pass the baton\"}");

        Resp assigned = put("/api/tasks/" + tid + "/assignee", alice.token(), "{\"assigneeId\":%d}".formatted(bob.id()));
        assertThat((String) assigned.json("$.assignee.username")).isEqualTo(bob.username());

        Resp cleared = put("/api/tasks/" + tid + "/assignee", alice.token(), "{\"assigneeId\":null}");
        assertThat(cleared.jsonOrNull("$.assignee")).isNull();
    }

    @Test
    void deleteIsRestrictedToReporterOrOwner() {
        Session alice = registerUser("alice");
        Session bob = registerUser("bob");
        Session carol = registerUser("carol");
        long pid = createProject(alice, "P");
        put("/api/projects/" + pid + "/members/" + bob.id(), alice.token(), null);
        put("/api/projects/" + pid + "/members/" + carol.id(), alice.token(), null);
        long bobsTask = createTask(bob, pid, "{\"title\":\"Bob's\"}");

        assertThat(delete("/api/tasks/" + bobsTask, carol.token()).status()).isEqualTo(403);
        assertThat(delete("/api/tasks/" + bobsTask, bob.token()).status()).isEqualTo(204);
        assertThat(get("/api/tasks/" + bobsTask, alice.token()).status()).isEqualTo(404);

        long alicesTask = createTask(alice, pid, "{\"title\":\"Alice's\"}");
        assertThat(delete("/api/tasks/" + alicesTask, bob.token()).status()).isEqualTo(403);
        assertThat(delete("/api/tasks/" + alicesTask, alice.token()).status()).isEqualTo(204);
    }

    @Test
    void statsReflectChangesAfterCacheEviction() throws Exception {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "P");
        createTask(alice, pid, "{\"title\":\"a\"}");
        long b = createTask(alice, pid, "{\"title\":\"b\"}");
        assertThat(get("/api/projects/" + pid + "/stats", alice.token()).<Integer>json("$.total")).isEqualTo(2);

        patch("/api/tasks/" + b + "/status", alice.token(), "{\"status\":\"IN_PROGRESS\"}");

        // eviction happens after commit, so poll briefly instead of asserting instantly
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            Resp stats = get("/api/projects/" + pid + "/stats", alice.token());
            assertThat(stats.<Integer>json("$.byStatus.IN_PROGRESS")).isEqualTo(1);
            assertThat(stats.<Integer>json("$.byStatus.TODO")).isEqualTo(1);
            assertThat(stats.<Integer>json("$.byStatus.DONE")).isEqualTo(0);
        });
    }
}
