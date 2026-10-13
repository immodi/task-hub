package com.example.taskhub.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

class CommentAndActivityApiTest extends AbstractApiTest {

    @Test
    void membersCanCommentAndListInOrder() {
        Session alice = registerUser("alice");
        Session bob = registerUser("bob");
        long pid = createProject(alice, "P");
        put("/api/projects/" + pid + "/members/" + bob.id(), alice.token(), null);
        long tid = createTask(alice, pid, "{\"title\":\"Discuss\"}");

        assertThat(post("/api/tasks/" + tid + "/comments", alice.token(), "{\"body\":\"first\"}").status()).isEqualTo(201);
        assertThat(post("/api/tasks/" + tid + "/comments", bob.token(), "{\"body\":\"second\"}").status()).isEqualTo(201);

        Resp list = get("/api/tasks/" + tid + "/comments", bob.token());
        assertThat(list.<List<String>>json("$.items[*].body")).containsExactly("first", "second");
        assertThat(list.<List<String>>json("$.items[*].author.username")).containsExactly(alice.username(), bob.username());
    }

    @Test
    void blankCommentIsRejectedAndOutsidersAreBlocked() {
        Session alice = registerUser("alice");
        Session eve = registerUser("eve");
        long pid = createProject(alice, "P");
        long tid = createTask(alice, pid, "{\"title\":\"T\"}");

        assertThat(post("/api/tasks/" + tid + "/comments", alice.token(), "{\"body\":\" \"}").status()).isEqualTo(400);
        assertThat(post("/api/tasks/" + tid + "/comments", eve.token(), "{\"body\":\"hi\"}").status()).isEqualTo(403);
        assertThat(get("/api/tasks/" + tid + "/comments", eve.token()).status()).isEqualTo(403);
    }

    @Test
    void onlyAuthorOrProjectOwnerCanDeleteAComment() {
        Session alice = registerUser("alice");
        Session bob = registerUser("bob");
        Session carol = registerUser("carol");
        long pid = createProject(alice, "P");
        put("/api/projects/" + pid + "/members/" + bob.id(), alice.token(), null);
        put("/api/projects/" + pid + "/members/" + carol.id(), alice.token(), null);
        long tid = createTask(alice, pid, "{\"title\":\"T\"}");
        long c1 = post("/api/tasks/" + tid + "/comments", bob.token(), "{\"body\":\"mine\"}").id();
        long c2 = post("/api/tasks/" + tid + "/comments", bob.token(), "{\"body\":\"also mine\"}").id();

        assertThat(delete("/api/comments/" + c1, carol.token()).status()).isEqualTo(403);
        assertThat(delete("/api/comments/" + c1, bob.token()).status()).isEqualTo(204);
        assertThat(delete("/api/comments/" + c2, alice.token()).status()).isEqualTo(204);   // project owner
        assertThat(delete("/api/comments/999999", alice.token()).status()).isEqualTo(404);
    }

    @Test
    void domainEventsEndUpInTheActivityLogAsynchronously() {
        Session alice = registerUser("alice");
        Session admin = adminUser();
        long pid = createProject(alice, "Audited");
        long tid = createTask(alice, pid, "{\"title\":\"Track me\"}");
        patch("/api/tasks/" + tid + "/status", alice.token(), "{\"status\":\"IN_PROGRESS\"}");
        post("/api/tasks/" + tid + "/comments", alice.token(), "{\"body\":\"noted\"}");

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Resp r = get("/api/admin/activity?projectId=" + pid + "&size=50", admin.token());
            assertThat(r.status()).isEqualTo(200);
            assertThat(r.<List<String>>json("$.items[*].action"))
                    .contains("TASK_CREATED", "TASK_STATUS_CHANGED", "COMMENT_ADDED");
        });
    }

    @Test
    void activityLogIsAdminOnly() {
        Session alice = registerUser("alice");

        assertThat(get("/api/admin/activity", alice.token()).status()).isEqualTo(403);
    }

    @Test
    void adminCanChangeRoles() {
        Session alice = registerUser("alice");
        Session admin = adminUser();

        Resp r = patch("/api/admin/users/" + alice.id() + "/role", admin.token(), "{\"role\":\"ADMIN\"}");

        assertThat(r.status()).isEqualTo(200);
        assertThat((String) r.json("$.role")).isEqualTo("ADMIN");
    }
}
