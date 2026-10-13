package com.example.taskhub.repository;

import com.example.taskhub.domain.TaskStatus;

/** Projection record used by a JPQL constructor expression. */
public record StatusCount(TaskStatus status, long count) {
}
