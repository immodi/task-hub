package com.example.taskhub.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.Project;
import com.example.taskhub.domain.User;
import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.dto.CommonDtos.ProjectStats;
import com.example.taskhub.dto.Mappers;
import com.example.taskhub.dto.ProjectDtos.ProjectCreateRequest;
import com.example.taskhub.dto.ProjectDtos.ProjectDto;
import com.example.taskhub.dto.ProjectDtos.ProjectUpdateRequest;
import com.example.taskhub.exception.ApiException;
import com.example.taskhub.repository.CommentRepository;
import com.example.taskhub.repository.ProjectRepository;
import com.example.taskhub.repository.TaskRepository;
import com.example.taskhub.repository.UserRepository;
import com.example.taskhub.security.AuthUser;

@Service
@Transactional
public class ProjectService {

    private static final Set<String> SORTABLE = Set.of("id", "name", "createdAt", "updatedAt");

    private final ProjectRepository projects;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final CommentRepository comments;
    private final AccessService access;
    private final StatsService stats;

    public ProjectService(ProjectRepository projects, UserRepository users, TaskRepository tasks,
                          CommentRepository comments, AccessService access, StatsService stats) {
        this.projects = projects;
        this.users = users;
        this.tasks = tasks;
        this.comments = comments;
        this.access = access;
        this.stats = stats;
    }

    public ProjectDto create(ProjectCreateRequest req, AuthUser actor) {
        Project p = new Project();
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setOwner(users.getReferenceById(actor.id()));
        return Mappers.projectDto(projects.saveAndFlush(p));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectDto> list(AuthUser actor, Pageable pageable) {
        Sorting.validate(pageable, SORTABLE);
        Page<Project> page = actor.admin()
                ? projects.findAll(pageable)
                : projects.findAccessible(actor.id(), pageable);
        return PageResponse.from(page.map(Mappers::projectDto));
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long id, AuthUser actor) {
        return Mappers.projectDto(access.requireProjectAccess(id, actor));
    }

    public ProjectDto update(Long id, ProjectUpdateRequest req, AuthUser actor) {
        Project p = access.requireProjectOwner(id, actor);
        if (req.version() != null && req.version() != p.getVersion()) {
            throw ApiException.conflict("Project was modified by someone else (current version " + p.getVersion() + ")");
        }
        p.setName(req.name().trim());
        p.setDescription(req.description());
        return Mappers.projectDto(projects.saveAndFlush(p));
    }

    public ProjectDto setArchived(Long id, boolean archived, AuthUser actor) {
        Project p = access.requireProjectOwner(id, actor);
        p.setArchived(archived);
        return Mappers.projectDto(projects.saveAndFlush(p));
    }

    public void delete(Long id, AuthUser actor) {
        Project p = access.requireProjectOwner(id, actor);
        comments.deleteByProjectId(id);
        tasks.deleteAll(tasks.findByProjectId(id));   // entity deletes also clean the task_tags join table
        projects.delete(p);
    }

    public ProjectDto addMember(Long projectId, Long userId, AuthUser actor) {
        Project p = access.requireProjectOwner(projectId, actor);
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User " + userId + " not found"));
        p.getMembers().add(user);
        return Mappers.projectDto(projects.saveAndFlush(p));
    }

    public ProjectDto removeMember(Long projectId, Long userId, AuthUser actor) {
        Project p = access.requireProjectOwner(projectId, actor);
        if (p.getOwner().getId().equals(userId)) {
            throw ApiException.conflict("The owner cannot be removed from the project");
        }
        p.getMembers().removeIf(m -> m.getId().equals(userId));
        projects.saveAndFlush(p);
        tasks.clearAssignee(projectId, userId);
        return Mappers.projectDto(projects.findById(projectId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public ProjectStats stats(Long projectId, AuthUser actor) {
        access.requireProjectAccess(projectId, actor);
        return stats.compute(projectId);
    }
}
