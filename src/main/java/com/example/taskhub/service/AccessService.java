package com.example.taskhub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.Project;
import com.example.taskhub.domain.Task;
import com.example.taskhub.exception.ApiException;
import com.example.taskhub.repository.ProjectRepository;
import com.example.taskhub.repository.TaskRepository;
import com.example.taskhub.security.AuthUser;

/** Resource-based authorization in one place (what you'd do with IAuthorizationService in .NET). */
@Service
@Transactional(readOnly = true)
public class AccessService {

    private final ProjectRepository projects;
    private final TaskRepository tasks;

    public AccessService(ProjectRepository projects, TaskRepository tasks) {
        this.projects = projects;
        this.tasks = tasks;
    }

    public Project requireProjectAccess(Long projectId, AuthUser actor) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> ApiException.notFound("Project " + projectId + " not found"));
        if (!actor.admin() && !projects.hasAccess(projectId, actor.id())) {
            throw ApiException.forbidden("You are not a member of this project");
        }
        return project;
    }

    public Project requireProjectOwner(Long projectId, AuthUser actor) {
        Project project = requireProjectAccess(projectId, actor);
        if (!actor.admin() && !project.getOwner().getId().equals(actor.id())) {
            throw ApiException.forbidden("Only the project owner can do that");
        }
        return project;
    }

    public Task requireTask(Long taskId, AuthUser actor) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> ApiException.notFound("Task " + taskId + " not found"));
        requireProjectAccess(task.getProject().getId(), actor);
        return task;
    }
}
