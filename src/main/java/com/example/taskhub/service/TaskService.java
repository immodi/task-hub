package com.example.taskhub.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.Priority;
import com.example.taskhub.domain.Project;
import com.example.taskhub.domain.Tag;
import com.example.taskhub.domain.Task;
import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.domain.User;
import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.dto.Mappers;
import com.example.taskhub.dto.TaskDtos.TaskCreateRequest;
import com.example.taskhub.dto.TaskDtos.TaskDto;
import com.example.taskhub.dto.TaskDtos.TaskFilter;
import com.example.taskhub.dto.TaskDtos.TaskUpdateRequest;
import com.example.taskhub.event.TaskEvent;
import com.example.taskhub.exception.ApiException;
import com.example.taskhub.repository.CommentRepository;
import com.example.taskhub.repository.ProjectRepository;
import com.example.taskhub.repository.TagRepository;
import com.example.taskhub.repository.TaskRepository;
import com.example.taskhub.repository.UserRepository;
import com.example.taskhub.security.AuthUser;

@Service
@Transactional
public class TaskService {

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final TagRepository tags;
    private final CommentRepository comments;
    private final AccessService access;
    private final ApplicationEventPublisher events;

    public TaskService(TaskRepository tasks, ProjectRepository projects, UserRepository users, TagRepository tags,
                       CommentRepository comments, AccessService access, ApplicationEventPublisher events) {
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
        this.tags = tags;
        this.comments = comments;
        this.access = access;
        this.events = events;
    }

    public TaskDto create(Long projectId, TaskCreateRequest req, AuthUser actor) {
        Project project = access.requireProjectAccess(projectId, actor);
        if (project.isArchived()) {
            throw ApiException.conflict("Project is archived");
        }
        Task task = new Task();
        task.setProject(project);
        task.setReporter(users.getReferenceById(actor.id()));
        task.setTitle(req.title().trim());
        task.setDescription(req.description());
        task.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        task.setDueDate(req.dueDate());
        task.setAssignee(resolveAssignee(project, req.assigneeId()));
        task.setTags(resolveTags(req.tags()));
        tasks.saveAndFlush(task);

        events.publishEvent(new TaskEvent.Created(projectId, actor.id(), task.getId(), task.getTitle()));
        return Mappers.taskDto(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskDto> search(Long projectId, TaskFilter filter, Pageable pageable, AuthUser actor) {
        access.requireProjectAccess(projectId, actor);
        Page<Task> page = tasks.findAll(TaskSpecifications.matching(projectId, filter), pageable);
        return PageResponse.from(page.map(Mappers::taskDto));
    }

    @Transactional(readOnly = true)
    public TaskDto get(Long taskId, AuthUser actor) {
        return Mappers.taskDto(access.requireTask(taskId, actor));
    }

    public TaskDto update(Long taskId, TaskUpdateRequest req, AuthUser actor) {
        Task task = access.requireTask(taskId, actor);
        if (req.version() != null && req.version() != task.getVersion()) {
            throw ApiException.conflict("Task was modified by someone else (current version " + task.getVersion() + ")");
        }
        task.setTitle(req.title().trim());
        task.setDescription(req.description());
        task.setPriority(req.priority());
        task.setDueDate(req.dueDate());
        task.setTags(resolveTags(req.tags()));
        return Mappers.taskDto(tasks.saveAndFlush(task));
    }

    public TaskDto changeStatus(Long taskId, TaskStatus next, AuthUser actor) {
        Task task = access.requireTask(taskId, actor);
        TaskStatus current = task.getStatus();
        if (!current.canTransitionTo(next)) {
            throw ApiException.unprocessable("Cannot move a task from " + current + " to " + next);
        }
        if (current != next) {
            task.setStatus(next);
            tasks.saveAndFlush(task);
            events.publishEvent(new TaskEvent.StatusChanged(task.getProject().getId(), actor.id(), taskId, current, next));
        }
        return Mappers.taskDto(task);
    }

    public TaskDto assign(Long taskId, Long assigneeId, AuthUser actor) {
        Task task = access.requireTask(taskId, actor);
        task.setAssignee(resolveAssignee(task.getProject(), assigneeId));
        tasks.saveAndFlush(task);
        events.publishEvent(new TaskEvent.Assigned(task.getProject().getId(), actor.id(), taskId, assigneeId));
        return Mappers.taskDto(task);
    }

    public void delete(Long taskId, AuthUser actor) {
        Task task = access.requireTask(taskId, actor);
        boolean isReporter = task.getReporter().getId().equals(actor.id());
        boolean isOwner = task.getProject().getOwner().getId().equals(actor.id());
        if (!actor.admin() && !isReporter && !isOwner) {
            throw ApiException.forbidden("Only the reporter or the project owner can delete a task");
        }
        comments.deleteByTaskId(taskId);
        tasks.delete(task);
        events.publishEvent(new TaskEvent.Deleted(task.getProject().getId(), actor.id(), taskId, task.getTitle()));
    }

    // ---- helpers ----

    private User resolveAssignee(Project project, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        if (!projects.hasAccess(project.getId(), assigneeId)) {
            throw ApiException.badRequest("Assignee must be a member of the project");
        }
        return users.getReferenceById(assigneeId);
    }

    /** Find-or-create tags by (lower-cased) name. */
    private Set<Tag> resolveTags(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> normalized = names.stream()
                .map(n -> n.trim().toLowerCase(Locale.ROOT))
                .filter(n -> !n.isEmpty())
                .collect(Collectors.toSet());
        List<Tag> existing = tags.findByNameIn(normalized);
        Set<String> known = existing.stream().map(Tag::getName).collect(Collectors.toSet());
        Set<Tag> result = new HashSet<>(existing);
        for (String name : normalized) {
            if (!known.contains(name)) {
                Tag t = new Tag();
                t.setName(name);
                result.add(tags.save(t));
            }
        }
        return result;
    }
}
