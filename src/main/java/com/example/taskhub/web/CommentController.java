package com.example.taskhub.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskhub.dto.CommentDtos.CommentCreateRequest;
import com.example.taskhub.dto.CommentDtos.CommentDto;
import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.security.AuthUser;
import com.example.taskhub.service.CommentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Comments")
@RestController
public class CommentController {

    private final CommentService comments;

    public CommentController(CommentService comments) {
        this.comments = comments;
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto add(@PathVariable Long taskId, @Valid @RequestBody CommentCreateRequest req, AuthUser actor) {
        return comments.add(taskId, req, actor);
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public PageResponse<CommentDto> list(@PathVariable Long taskId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            AuthUser actor) {
        return comments.list(taskId, pageable, actor);
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, AuthUser actor) {
        comments.delete(id, actor);
    }
}
