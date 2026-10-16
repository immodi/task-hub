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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.dto.CommonDtos.ProjectStats;
import com.example.taskhub.dto.ProjectDtos.ProjectCreateRequest;
import com.example.taskhub.dto.ProjectDtos.ProjectDto;
import com.example.taskhub.dto.ProjectDtos.ProjectUpdateRequest;
import com.example.taskhub.security.AuthUser;
import com.example.taskhub.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Projects", description = "Projects, membership and aggregated task stats")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @Operation(summary = "Create a project", description = "The caller becomes the owner.")
    @ApiResponse(responseCode = "201", description = "Project created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @PostMapping
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectCreateRequest req, AuthUser actor) {
        ProjectDto dto = projects.create(req, actor);
        return ResponseEntity.created(URI.create("/api/projects/" + dto.id())).body(dto);
    }

    @Operation(summary = "List visible projects",
            description = "Admins see every project; everyone else sees the projects they own or belong to. "
                    + "Sortable by id, name, createdAt, updatedAt.")
    @ApiResponse(responseCode = "200", description = "A page of projects")
    @GetMapping
    public PageResponse<ProjectDto> list(AuthUser actor,
            // Pageable is bound from ?page=0&size=20&sort=name,asc automatically.
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projects.list(actor, pageable);
    }

    @Operation(summary = "Get a project")
    @ApiResponse(responseCode = "200", description = "The project")
    @ApiResponse(responseCode = "403", description = "Not a member")
    @ApiResponse(responseCode = "404", description = "No such project")
    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable Long id, AuthUser actor) {
        return projects.get(id, actor);
    }

    @Operation(summary = "Update a project",
            description = "Owner only. Supplying a stale `version` returns 409.")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "403", description = "Not the owner")
    @ApiResponse(responseCode = "409", description = "Stale version")
    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest req, AuthUser actor) {
        return projects.update(id, req, actor);
    }

    @Operation(summary = "Delete a project", description = "Owner only. Removes its tasks and comments too.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "403", description = "Not the owner")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, AuthUser actor) {
        projects.delete(id, actor);
    }

    @Operation(summary = "Archive a project", description = "Archived projects reject new tasks with 409.")
    @PostMapping("/{id}/archive")
    public ProjectDto archive(@PathVariable Long id, AuthUser actor) {
        return projects.setArchived(id, true, actor);
    }

    @Operation(summary = "Unarchive a project")
    @PostMapping("/{id}/unarchive")
    public ProjectDto unarchive(@PathVariable Long id, AuthUser actor) {
        return projects.setArchived(id, false, actor);
    }

    @Operation(summary = "Add a member", description = "Owner only. Adding an existing member is a no-op.")
    @ApiResponse(responseCode = "200", description = "Member added")
    @ApiResponse(responseCode = "404", description = "No such user")
    @PutMapping("/{id}/members/{userId}")
    public ProjectDto addMember(@PathVariable Long id, @PathVariable Long userId, AuthUser actor) {
        return projects.addMember(id, userId, actor);
    }

    @Operation(summary = "Remove a member",
            description = "Owner only. The owner cannot be removed. Their tasks are unassigned.")
    @ApiResponse(responseCode = "200", description = "Member removed")
    @ApiResponse(responseCode = "409", description = "Attempted to remove the owner")
    @DeleteMapping("/{id}/members/{userId}")
    public ProjectDto removeMember(@PathVariable Long id, @PathVariable Long userId, AuthUser actor) {
        return projects.removeMember(id, userId, actor);
    }

    @Operation(summary = "Project stats",
            description = "Total, overdue and per-status counts. Cached and evicted after task changes commit.")
    @ApiResponse(responseCode = "200", description = "The stats")
    @GetMapping("/{id}/stats")
    public ProjectStats stats(@PathVariable Long id, AuthUser actor) {
        return projects.stats(id, actor);
    }
}
