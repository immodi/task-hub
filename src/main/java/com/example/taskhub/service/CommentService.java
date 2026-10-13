package com.example.taskhub.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.Comment;
import com.example.taskhub.domain.Task;
import com.example.taskhub.dto.CommentDtos.CommentCreateRequest;
import com.example.taskhub.dto.CommentDtos.CommentDto;
import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.dto.Mappers;
import com.example.taskhub.event.TaskEvent;
import com.example.taskhub.exception.ApiException;
import com.example.taskhub.repository.CommentRepository;
import com.example.taskhub.repository.UserRepository;
import com.example.taskhub.security.AuthUser;

@Service
@Transactional
public class CommentService {

    private final CommentRepository comments;
    private final UserRepository users;
    private final AccessService access;
    private final ApplicationEventPublisher events;

    public CommentService(CommentRepository comments, UserRepository users, AccessService access,
                          ApplicationEventPublisher events) {
        this.comments = comments;
        this.users = users;
        this.access = access;
        this.events = events;
    }

    public CommentDto add(Long taskId, CommentCreateRequest req, AuthUser actor) {
        Task task = access.requireTask(taskId, actor);
        Comment c = new Comment();
        c.setTask(task);
        c.setAuthor(users.getReferenceById(actor.id()));
        c.setBody(req.body().trim());
        comments.saveAndFlush(c);
        events.publishEvent(new TaskEvent.Commented(task.getProject().getId(), actor.id(), taskId, c.getId()));
        return Mappers.commentDto(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentDto> list(Long taskId, Pageable pageable, AuthUser actor) {
        access.requireTask(taskId, actor);
        return PageResponse.from(comments.findByTaskId(taskId, pageable).map(Mappers::commentDto));
    }

    public void delete(Long commentId, AuthUser actor) {
        Comment c = comments.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comment " + commentId + " not found"));
        Task task = access.requireTask(c.getTask().getId(), actor);
        boolean isAuthor = c.getAuthor().getId().equals(actor.id());
        boolean isOwner = task.getProject().getOwner().getId().equals(actor.id());
        if (!actor.admin() && !isAuthor && !isOwner) {
            throw ApiException.forbidden("Only the author or the project owner can delete a comment");
        }
        comments.delete(c);
    }
}
