package com.example.taskhub.dto;

import java.time.Instant;

import com.example.taskhub.dto.CommonDtos.UserDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CommentDtos {
    private CommentDtos() {}

    public record CommentCreateRequest(@NotBlank @Size(max = 2000) String body) {}

    public record CommentDto(Long id, Long taskId, UserDto author, String body, Instant createdAt) {}
}
