package com.example.taskhub.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.example.taskhub.domain.Priority;
import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.dto.CommonDtos.UserDto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TaskDtos {
    private TaskDtos() {}

    public record TaskCreateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            Priority priority,
            @FutureOrPresent LocalDate dueDate,
            Long assigneeId,
            @Size(max = 10) Set<@NotBlank @Size(max = 30) String> tags) {}

    /** PUT = full replace of the editable fields. */
    public record TaskUpdateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @NotNull Priority priority,
            LocalDate dueDate,
            @Size(max = 10) Set<@NotBlank @Size(max = 30) String> tags,
            Long version) {}

    public record StatusChangeRequest(@NotNull TaskStatus status) {}

    public record AssignRequest(Long assigneeId) {}   // null = unassign

    /** Bound from query-string parameters via @ModelAttribute. */
    public record TaskFilter(TaskStatus status, Priority priority, Long assigneeId, String tag, String q, Boolean overdue) {}

    public record TaskDto(Long id, Long projectId, String title, String description, TaskStatus status,
                          Priority priority, LocalDate dueDate, boolean overdue, UserDto assignee, UserDto reporter,
                          List<String> tags, Instant createdAt, Instant updatedAt, long version) {}
}
