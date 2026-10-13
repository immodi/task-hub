package com.example.taskhub.dto;

import java.time.LocalDate;
import java.util.Comparator;

import com.example.taskhub.domain.ActivityLog;
import com.example.taskhub.domain.Comment;
import com.example.taskhub.domain.Project;
import com.example.taskhub.domain.Tag;
import com.example.taskhub.domain.Task;
import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.domain.User;
import com.example.taskhub.dto.CommentDtos.CommentDto;
import com.example.taskhub.dto.CommonDtos.ActivityDto;
import com.example.taskhub.dto.CommonDtos.UserDto;
import com.example.taskhub.dto.ProjectDtos.ProjectDto;
import com.example.taskhub.dto.TaskDtos.TaskDto;

/** Hand-written mapping (no MapStruct/ModelMapper). Must be called inside a transaction: it touches lazy fields. */
public final class Mappers {
    private Mappers() {}

    public static UserDto userDto(User u) {
        return u == null ? null : new UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }

    public static ProjectDto projectDto(Project p) {
        return new ProjectDto(p.getId(), p.getName(), p.getDescription(), p.isArchived(), userDto(p.getOwner()),
                p.getMembers().stream().map(Mappers::userDto).sorted(Comparator.comparing(UserDto::username)).toList(),
                p.getCreatedAt(), p.getUpdatedAt(), p.getVersion());
    }

    public static TaskDto taskDto(Task t) {
        boolean overdue = t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()) && t.getStatus() != TaskStatus.DONE;
        return new TaskDto(t.getId(), t.getProject().getId(), t.getTitle(), t.getDescription(), t.getStatus(),
                t.getPriority(), t.getDueDate(), overdue, userDto(t.getAssignee()), userDto(t.getReporter()),
                t.getTags().stream().map(Tag::getName).sorted().toList(),
                t.getCreatedAt(), t.getUpdatedAt(), t.getVersion());
    }

    public static CommentDto commentDto(Comment c) {
        return new CommentDto(c.getId(), c.getTask().getId(), userDto(c.getAuthor()), c.getBody(), c.getCreatedAt());
    }

    public static ActivityDto activityDto(ActivityLog a) {
        return new ActivityDto(a.getId(), a.getProjectId(), a.getActorId(), a.getAction(), a.getEntityId(),
                a.getDetails(), a.getCreatedAt());
    }
}
