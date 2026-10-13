package com.example.taskhub.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.example.taskhub.domain.Role;
import com.example.taskhub.domain.TaskStatus;

/** Small shared DTOs. Records give you immutable value types with equals/hashCode/toString for free. */
public final class CommonDtos {
    private CommonDtos() {}

    public record UserDto(Long id, String username, String email, Role role, Instant createdAt) {}

    public record ActivityDto(Long id, Long projectId, Long actorId, String action, Long entityId,
                              String details, Instant createdAt) {}

    public record ProjectStats(Long projectId, long total, long overdue, Map<TaskStatus, Long> byStatus) {}

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
        public static <T> PageResponse<T> from(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}
