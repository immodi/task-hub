package com.example.taskhub.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.taskhub.domain.Priority;
import com.example.taskhub.domain.Project;
import com.example.taskhub.domain.Tag;
import com.example.taskhub.domain.Task;
import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.domain.User;
import com.example.taskhub.dto.Mappers;

/** Pure unit tests for the hand-written mapping logic. No Spring context. */
class MappersTest {

    @Test
    void pastDueIncompleteTaskIsOverdue() {
        assertThat(Mappers.taskDto(task(TaskStatus.IN_PROGRESS, LocalDate.now().minusDays(1))).overdue()).isTrue();
    }

    @Test
    void dueTodayIsNotOverdue() {
        assertThat(Mappers.taskDto(task(TaskStatus.TODO, LocalDate.now())).overdue()).isFalse();
    }

    @Test
    void doneTaskIsNeverOverdue() {
        assertThat(Mappers.taskDto(task(TaskStatus.DONE, LocalDate.now().minusDays(5))).overdue()).isFalse();
    }

    @Test
    void taskWithoutADueDateIsNotOverdue() {
        assertThat(Mappers.taskDto(task(TaskStatus.TODO, null)).overdue()).isFalse();
    }

    @Test
    void tagsAreExposedAsASortedListOfNames() {
        Task t = task(TaskStatus.TODO, null);
        t.getTags().add(tag("frontend"));
        t.getTags().add(tag("api"));
        t.getTags().add(tag("backend"));

        assertThat(Mappers.taskDto(t).tags()).containsExactly("api", "backend", "frontend");
    }

    @Test
    void reporterIsExposedWithoutThePasswordHash() {
        assertThat(Mappers.taskDto(task(TaskStatus.TODO, null)).reporter().username()).isEqualTo("alice");
    }

    private static Task task(TaskStatus status, LocalDate dueDate) {
        Project p = new Project();
        p.setName("P");
        Task t = new Task();
        t.setProject(p);
        t.setReporter(user("alice"));
        t.setTitle("T");
        t.setStatus(status);
        t.setPriority(Priority.MEDIUM);
        t.setDueDate(dueDate);
        return t;
    }

    private static User user(String name) {
        User u = new User();
        u.setUsername(name);
        u.setEmail(name + "@test.dev");
        return u;
    }

    private static Tag tag(String name) {
        Tag t = new Tag();
        t.setName(name);
        return t;
    }
}
