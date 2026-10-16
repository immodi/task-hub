package com.example.taskhub.web;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.dto.TaskDtos.AssignRequest;
import com.example.taskhub.dto.TaskDtos.StatusChangeRequest;
import com.example.taskhub.dto.TaskDtos.TaskCreateRequest;
import com.example.taskhub.dto.TaskDtos.TaskDto;
import com.example.taskhub.dto.TaskDtos.TaskFilter;
import com.example.taskhub.dto.TaskDtos.TaskUpdateRequest;
import com.example.taskhub.security.AuthUser;
import com.example.taskhub.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Tasks", description = "Tasks, search, the status workflow and assignment")
@RestController
public class TaskController {

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @Operation(summary = "Create a task",
            description = "The caller becomes the reporter. An assignee must be a member of the project.")
    @ApiResponse(responseCode = "201", description = "Task created")
    @ApiResponse(responseCode = "400", description = "Validation failed or the assignee is not a member")
    @ApiResponse(responseCode = "409", description = "The project is archived")
    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskDto> create(@PathVariable Long projectId, @Valid @RequestBody TaskCreateRequest req,
                                          AuthUser actor) {
        TaskDto dto = tasks.create(projectId, req, actor);
        return ResponseEntity.created(URI.create("/api/tasks/" + dto.id())).body(dto);
    }

    @Operation(summary = "Search tasks",
            description = "Filters by status, priority, assignee, tag, free text (`q`) and `overdue`. "
                    + "Sortable by id, title, status, priority, dueDate, createdAt, updatedAt, assignee.username.")
    @ApiResponse(responseCode = "200", description = "A page of tasks")
    @ApiResponse(responseCode = "400", description = "Unknown sort field")
    @GetMapping("/api/projects/{projectId}/tasks")
    public PageResponse<TaskDto> search(@PathVariable Long projectId, @ParameterObject @ModelAttribute TaskFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            AuthUser actor) {
        return tasks.search(projectId, filter, pageable, actor);
    }

    @Operation(summary = "Get a task")
    @ApiResponse(responseCode = "200", description = "The task")
    @ApiResponse(responseCode = "404", description = "No such task")
    @GetMapping("/api/tasks/{id}")
    public TaskDto get(@PathVariable Long id, AuthUser actor) {
        return tasks.get(id, actor);
    }

    @Operation(summary = "Replace a task",
            description = "Full replace of the editable fields. Supplying a stale `version` returns 409.")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "409", description = "Stale version")
    @PutMapping("/api/tasks/{id}")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest req, AuthUser actor) {
        return tasks.update(id, req, actor);
    }

    @Operation(summary = "Change status",
            description = "Must follow the workflow (TODO -> IN_PROGRESS/BLOCKED, ..., DONE -> TODO/IN_PROGRESS). "
                    + "An illegal move returns 422.")
    @ApiResponse(responseCode = "200", description = "Status changed")
    @ApiResponse(responseCode = "422", description = "Illegal transition")
    @PatchMapping("/api/tasks/{id}/status")
    public TaskDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest req, AuthUser actor) {
        return tasks.changeStatus(id, req.status(), actor);
    }

    @Operation(summary = "Assign or unassign", description = "Send `{\"assigneeId\": null}` to unassign.")
    @ApiResponse(responseCode = "200", description = "Assignee updated")
    @PutMapping("/api/tasks/{id}/assignee")
    public TaskDto assign(@PathVariable Long id, @RequestBody AssignRequest req, AuthUser actor) {
        return tasks.assign(id, req.assigneeId(), actor);
    }

    @Operation(summary = "Delete a task",
            description = "Allowed for the reporter, the project owner or an admin. Comments go with it.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "403", description = "Not the reporter or owner")
    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, AuthUser actor) {
        tasks.delete(id, actor);
    }
}
