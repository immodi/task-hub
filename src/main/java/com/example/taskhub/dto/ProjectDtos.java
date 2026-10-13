package com.example.taskhub.dto;

import java.time.Instant;
import java.util.List;

import com.example.taskhub.dto.CommonDtos.UserDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record ProjectCreateRequest(@NotBlank @Size(max = 120) String name, @Size(max = 2000) String description) {}

    /** "version" is optional; when supplied it must match the current version (optimistic concurrency). */
    public record ProjectUpdateRequest(@NotBlank @Size(max = 120) String name, @Size(max = 2000) String description, Long version) {}

    public record ProjectDto(Long id, String name, String description, boolean archived, UserDto owner,
                             List<UserDto> members, Instant createdAt, Instant updatedAt, long version) {}
}
