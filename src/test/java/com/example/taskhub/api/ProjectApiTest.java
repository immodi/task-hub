package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProjectApiTest extends AbstractApiTest {

    @Test
    void createReturns201WithLocationAndOwner() {
        Session alice = registerUser("alice");

        Resp r = post("/api/projects", alice.token(), "{\"name\":\"Roadmap\",\"description\":\"Q4\"}");

        assertThat(r.status()).isEqualTo(201);
        assertThat((String) r.json("$.name")).isEqualTo("Roadmap");
        assertThat((String) r.json("$.owner.username")).isEqualTo(alice.username());
    }

    @Test
    void blankNameIsRejected() {
        Session alice = registerUser("alice");

        assertThat(post("/api/projects", alice.token(), "{\"name\":\"  \"}").status()).isEqualTo(400);
    }

    @Test
    void listShowsOnlyProjectsYouCanAccess() {
        Session alice = registerUser("alice");
        Session bob = registerUser("bob");
        long shared = createProject(alice, "Shared");
        createProject(alice, "Alice only");
        assertThat(put("/api/projects/" + shared + "/members/" + bob.id(), alice.token(), null).status()).isEqualTo(200);

        Resp bobList = get("/api/projects", bob.token());

        assertThat(bobList.status()).isEqualTo(200);
        List<String> names = bobList.json("$.items[*].name");
        assertThat(names).containsExactly("Shared");
        assertThat(get("/api/projects", alice.token()).<Integer>json("$.totalItems")).isEqualTo(2);
    }

    @Test
    void outsidersGet403AndUnknownIdsGet404() {
        Session alice = registerUser("alice");
        Session eve = registerUser("eve");
        long id = createProject(alice, "Private");

        assertThat(get("/api/projects/" + id, eve.token()).status()).isEqualTo(403);
        assertThat(get("/api/projects/999999", alice.token()).status()).isEqualTo(404);
    }

    @Test
    void onlyOwnerCanUpdateAndStaleVersionConflicts() {
        Session alice = registerUser("alice");
        Session bob = registerUser("bob");
        long id = createProject(alice, "Before");
        put("/api/projects/" + id + "/members/" + bob.id(), alice.token(), null);

        assertThat(put("/api/projects/" + id, bob.token(), "{\"name\":\"Hijacked\"}").status()).isEqualTo(403);

        Resp ok = put("/api/projects/" + id, alice.token(), "{\"name\":\"After\"}");
        assertThat(ok.status()).isEqualTo(200);
        assertThat((String) ok.json("$.name")).isEqualTo("After");

        Resp stale = put("/api/projects/" + id, alice.token(), "{\"name\":\"Again\",\"version\":0}");
        assertThat(stale.status()).isEqualTo(409);
    }

    @Test
    void archivedProjectsRejectNewTasks() {
        Session alice = registerUser("alice");
        long id = createProject(alice, "Old");
        assertThat(post("/api/projects/" + id + "/archive", alice.token(), null).status()).isEqualTo(200);

        Resp r = post("/api/projects/" + id + "/tasks", alice.token(), "{\"title\":\"Too late\"}");

        assertThat(r.status()).isEqualTo(409);
        assertThat(post("/api/projects/" + id + "/unarchive", alice.token(), null).status()).isEqualTo(200);
        assertThat(post("/api/projects/" + id + "/tasks", alice.token(), "{\"title\":\"Fine now\"}").status()).isEqualTo(201);
    }

    @Test
    void removingMemberUnassignsTheirTasksAndOwnerCannotBeRemoved() {
        Session alice = registerUser("alice");
        Session bob = registerUser("bob");
        long pid = createProject(alice, "Team");
        put("/api/projects/" + pid + "/members/" + bob.id(), alice.token(), null);
        long tid = createTask(alice, pid, "{\"title\":\"Bob's task\",\"assigneeId\":%d}".formatted(bob.id()));

        assertThat(delete("/api/projects/" + pid + "/members/" + alice.id(), alice.token()).status()).isEqualTo(409);
        assertThat(delete("/api/projects/" + pid + "/members/" + bob.id(), alice.token()).status()).isEqualTo(200);

        Resp task = get("/api/tasks/" + tid, alice.token());
        assertThat(task.jsonOrNull("$.assignee")).isNull();
        assertThat(get("/api/projects/" + pid, bob.token()).status()).isEqualTo(403);
    }

    @Test
    void deletingProjectRemovesItsTasksAndComments() {
        Session alice = registerUser("alice");
        long pid = createProject(alice, "Doomed");
        long tid = createTask(alice, pid, "{\"title\":\"Task\",\"tags\":[\"x\"]}");
        post("/api/tasks/" + tid + "/comments", alice.token(), "{\"body\":\"hi\"}");

        assertThat(delete("/api/projects/" + pid, alice.token()).status()).isEqualTo(204);

        assertThat(get("/api/projects/" + pid, alice.token()).status()).isEqualTo(404);
        assertThat(get("/api/tasks/" + tid, alice.token()).status()).isEqualTo(404);
    }
}
