package com.example.taskhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.Priority;
import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.domain.User;
import com.example.taskhub.dto.CommonDtos.ProjectStats;
import com.example.taskhub.dto.ProjectDtos.ProjectCreateRequest;
import com.example.taskhub.dto.ProjectDtos.ProjectDto;
import com.example.taskhub.dto.TaskDtos.TaskCreateRequest;
import com.example.taskhub.dto.TaskDtos.TaskDto;
import com.example.taskhub.dto.TaskDtos.TaskFilter;
import com.example.taskhub.dto.TaskDtos.TaskUpdateRequest;
import com.example.taskhub.exception.ApiException;
import com.example.taskhub.repository.UserRepository;
import com.example.taskhub.security.AuthUser;
import com.example.taskhub.service.ProjectService;
import com.example.taskhub.service.TaskService;

/**
 * Integration tests against the real Spring context + in-memory H2 (like WebApplicationFactory).
 * @Transactional on the test class rolls back after every test, so tests stay isolated.
 * Tip: add MockMvc-based controller tests once you're comfortable (check the Spring Boot 4 test starter docs).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskServiceTest {

    @Autowired UserRepository users;
    @Autowired ProjectService projects;
    @Autowired TaskService tasks;

    AuthUser alice;
    AuthUser bob;
    ProjectDto project;

    @BeforeEach
    void setUp() {
        alice = newUser("alice");
        bob = newUser("bob");
        project = projects.create(new ProjectCreateRequest("Demo", "test project"), alice);
    }

    @Test
    void createsTaskWithTagsAndDefaults() {
        TaskDto t = tasks.create(project.id(),
                new TaskCreateRequest("Write docs", null, null, null, null, Set.of("Docs", " backend ")), alice);

        assertThat(t.status()).isEqualTo(TaskStatus.TODO);
        assertThat(t.priority()).isEqualTo(Priority.MEDIUM);
        assertThat(t.tags()).containsExactly("backend", "docs");
        assertThat(t.reporter().username()).isEqualTo("alice");
    }

    @Test
    void nonMembersAreForbiddenUntilAdded() {
        assertThatThrownBy(() -> tasks.search(project.id(), emptyFilter(), PageRequest.of(0, 10), bob))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);

        projects.addMember(project.id(), bob.id(), alice);

        assertThat(tasks.search(project.id(), emptyFilter(), PageRequest.of(0, 10), bob).items()).isEmpty();
    }

    @Test
    void rejectsInvalidStatusTransition() {
        TaskDto t = tasks.create(project.id(), simple("Ship it"), alice);

        assertThatThrownBy(() -> tasks.changeStatus(t.id(), TaskStatus.DONE, alice))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        tasks.changeStatus(t.id(), TaskStatus.IN_PROGRESS, alice);
        assertThat(tasks.changeStatus(t.id(), TaskStatus.DONE, alice).status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void filtersByTagAndText() {
        tasks.create(project.id(), new TaskCreateRequest("Fix login bug", "oauth redirect", Priority.HIGH, null, null, Set.of("bug")), alice);
        tasks.create(project.id(), new TaskCreateRequest("Write README", null, null, null, null, Set.of("docs")), alice);

        var byTag = tasks.search(project.id(), new TaskFilter(null, null, null, "BUG", null, null), PageRequest.of(0, 10), alice);
        var byText = tasks.search(project.id(), new TaskFilter(null, null, null, null, "readme", null), PageRequest.of(0, 10), alice);

        assertThat(byTag.items()).extracting(TaskDto::title).containsExactly("Fix login bug");
        assertThat(byText.items()).extracting(TaskDto::title).containsExactly("Write README");
    }

    @Test
    void staleVersionIsRejected() {
        TaskDto t = tasks.create(project.id(), simple("Concurrent edit"), alice);

        var first = new TaskUpdateRequest("Renamed", null, Priority.LOW, null, Set.of(), t.version());
        TaskDto updated = tasks.update(t.id(), first, alice);
        assertThat(updated.title()).isEqualTo("Renamed");

        var stale = new TaskUpdateRequest("Renamed again", null, Priority.LOW, null, Set.of(), t.version());
        assertThatThrownBy(() -> tasks.update(t.id(), stale, alice))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void statsCountEveryStatus() {
        tasks.create(project.id(), simple("a"), alice);
        TaskDto b = tasks.create(project.id(), simple("b"), alice);
        tasks.changeStatus(b.id(), TaskStatus.IN_PROGRESS, alice);

        ProjectStats stats = projects.stats(project.id(), alice);

        assertThat(stats.total()).isEqualTo(2);
        assertThat(stats.byStatus()).containsEntry(TaskStatus.TODO, 1L)
                .containsEntry(TaskStatus.IN_PROGRESS, 1L)
                .containsEntry(TaskStatus.DONE, 0L);
    }

    // ---- helpers ----

    private AuthUser newUser(String name) {
        User u = new User();
        u.setUsername(name);
        u.setEmail(name + "@test.dev");
        u.setPasswordHash("not-a-real-hash");
        users.saveAndFlush(u);
        return new AuthUser(u.getId(), name, false);
    }

    private static TaskCreateRequest simple(String title) {
        return new TaskCreateRequest(title, null, null, null, null, null);
    }

    private static TaskFilter emptyFilter() {
        return new TaskFilter(null, null, null, null, null, null);
    }
}
