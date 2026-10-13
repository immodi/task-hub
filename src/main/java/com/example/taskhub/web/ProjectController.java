package com.example.taskhub.web;

import java.net.URI;

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

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Projects")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @PostMapping
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectCreateRequest req, AuthUser actor) {
        ProjectDto dto = projects.create(req, actor);
        return ResponseEntity.created(URI.create("/api/projects/" + dto.id())).body(dto);
    }

    /** Pageable is bound from ?page=0&size=20&sort=name,asc automatically. */
    @GetMapping
    public PageResponse<ProjectDto> list(AuthUser actor,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projects.list(actor, pageable);
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable Long id, AuthUser actor) {
        return projects.get(id, actor);
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest req, AuthUser actor) {
        return projects.update(id, req, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, AuthUser actor) {
        projects.delete(id, actor);
    }

    @PostMapping("/{id}/archive")
    public ProjectDto archive(@PathVariable Long id, AuthUser actor) {
        return projects.setArchived(id, true, actor);
    }

    @PostMapping("/{id}/unarchive")
    public ProjectDto unarchive(@PathVariable Long id, AuthUser actor) {
        return projects.setArchived(id, false, actor);
    }

    @PutMapping("/{id}/members/{userId}")
    public ProjectDto addMember(@PathVariable Long id, @PathVariable Long userId, AuthUser actor) {
        return projects.addMember(id, userId, actor);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ProjectDto removeMember(@PathVariable Long id, @PathVariable Long userId, AuthUser actor) {
        return projects.removeMember(id, userId, actor);
    }

    @GetMapping("/{id}/stats")
    public ProjectStats stats(@PathVariable Long id, AuthUser actor) {
        return projects.stats(id, actor);
    }
}
