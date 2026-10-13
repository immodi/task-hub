package com.example.taskhub.web;

import java.net.URI;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.dto.TaskDtos.AssignRequest;
import com.example.taskhub.dto.TaskDtos.StatusChangeRequest;
import com.example.taskhub.dto.TaskDtos.TaskCreateRequest;
import com.example.taskhub.dto.TaskDtos.TaskDto;
import com.example.taskhub.dto.TaskDtos.TaskFilter;
import com.example.taskhub.dto.TaskDtos.TaskUpdateRequest;
import com.example.taskhub.security.AuthUser;
import com.example.taskhub.service.TaskService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Tasks")
@RestController
public class TaskController {

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskDto> create(@PathVariable Long projectId, @Valid @RequestBody TaskCreateRequest req,
                                          AuthUser actor) {
        TaskDto dto = tasks.create(projectId, req, actor);
        return ResponseEntity.created(URI.create("/api/tasks/" + dto.id())).body(dto);
    }

    /** e.g. ?status=IN_PROGRESS&tag=backend&q=login&overdue=true&sort=dueDate,asc&page=0&size=10 */
    @GetMapping("/api/projects/{projectId}/tasks")
    public PageResponse<TaskDto> search(@PathVariable Long projectId, @ModelAttribute TaskFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            AuthUser actor) {
        return tasks.search(projectId, filter, pageable, actor);
    }

    @GetMapping("/api/tasks/{id}")
    public TaskDto get(@PathVariable Long id, AuthUser actor) {
        return tasks.get(id, actor);
    }

    @PutMapping("/api/tasks/{id}")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest req, AuthUser actor) {
        return tasks.update(id, req, actor);
    }

    @PatchMapping("/api/tasks/{id}/status")
    public TaskDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest req, AuthUser actor) {
        return tasks.changeStatus(id, req.status(), actor);
    }

    @PutMapping("/api/tasks/{id}/assignee")
    public TaskDto assign(@PathVariable Long id, @RequestBody AssignRequest req, AuthUser actor) {
        return tasks.assign(id, req.assigneeId(), actor);
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, AuthUser actor) {
        tasks.delete(id, actor);
    }
}
