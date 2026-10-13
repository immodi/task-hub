package com.example.taskhub.service;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.example.taskhub.domain.Tag;
import com.example.taskhub.domain.Task;
import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.dto.TaskDtos.TaskFilter;

import jakarta.persistence.criteria.Join;

/** Composable query predicates: roughly "IQueryable<T> + Where(...)" built dynamically. */
public final class TaskSpecifications {
    private TaskSpecifications() {}

    public static Specification<Task> matching(Long projectId, TaskFilter f) {
        Specification<Task> spec = (root, q, cb) -> cb.equal(root.get("project").get("id"), projectId);

        if (f.status() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), f.status()));
        }
        if (f.priority() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("priority"), f.priority()));
        }
        if (f.assigneeId() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("assignee").get("id"), f.assigneeId()));
        }
        if (f.tag() != null && !f.tag().isBlank()) {
            String tag = f.tag().trim().toLowerCase();
            spec = spec.and((root, q, cb) -> {
                if (q != null) {
                    q.distinct(true);
                }
                Join<Task, Tag> tags = root.join("tags");
                return cb.equal(tags.<String>get("name"), tag);
            });
        }
        if (f.q() != null && !f.q().isBlank()) {
            String like = "%" + f.q().trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("title")), like),
                    cb.like(cb.lower(root.<String>get("description")), like)));
        }
        if (Boolean.TRUE.equals(f.overdue())) {
            spec = spec.and((root, q, cb) -> cb.and(
                    cb.lessThan(root.<LocalDate>get("dueDate"), LocalDate.now()),
                    cb.notEqual(root.get("status"), TaskStatus.DONE)));
        }
        return spec;
    }
}
