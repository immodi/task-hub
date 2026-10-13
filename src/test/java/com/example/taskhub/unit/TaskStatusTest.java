package com.example.taskhub.unit;

import static com.example.taskhub.domain.TaskStatus.BLOCKED;
import static com.example.taskhub.domain.TaskStatus.DONE;
import static com.example.taskhub.domain.TaskStatus.IN_PROGRESS;
import static com.example.taskhub.domain.TaskStatus.TODO;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.taskhub.domain.TaskStatus;

/** Pure unit tests: no Spring context, so they run in milliseconds. */
class TaskStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @CsvSource({
            "TODO,        IN_PROGRESS, true",
            "TODO,        BLOCKED,     true",
            "TODO,        DONE,        false",
            "IN_PROGRESS, TODO,        true",
            "IN_PROGRESS, BLOCKED,     true",
            "IN_PROGRESS, DONE,        true",
            "BLOCKED,     IN_PROGRESS, true",
            "BLOCKED,     TODO,        true",
            "BLOCKED,     DONE,        false",
            "DONE,        TODO,        true",
            "DONE,        IN_PROGRESS, true",
            "DONE,        BLOCKED,     false"
    })
    void transitionMatrix(TaskStatus from, TaskStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void stayingInTheSameStatusIsAlwaysFine() {
        for (TaskStatus s : new TaskStatus[] {TODO, IN_PROGRESS, BLOCKED, DONE}) {
            assertThat(s.canTransitionTo(s)).isTrue();
        }
    }
}
