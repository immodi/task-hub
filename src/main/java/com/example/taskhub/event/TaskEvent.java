package com.example.taskhub.event;

import com.example.taskhub.domain.TaskStatus;

/**
 * Domain events. A sealed interface + records lets the listener use an exhaustive
 * pattern-matching switch (the compiler errors if you add a case and forget to handle it).
 * Events carry ids only, never entities, because they're handled after the transaction closed.
 */
public sealed interface TaskEvent {

    Long projectId();
    Long actorId();

    record Created(Long projectId, Long actorId, Long taskId, String title) implements TaskEvent {}
    record StatusChanged(Long projectId, Long actorId, Long taskId, TaskStatus from, TaskStatus to) implements TaskEvent {}
    record Assigned(Long projectId, Long actorId, Long taskId, Long assigneeId) implements TaskEvent {}
    record Deleted(Long projectId, Long actorId, Long taskId, String title) implements TaskEvent {}
    record Commented(Long projectId, Long actorId, Long taskId, Long commentId) implements TaskEvent {}
}
