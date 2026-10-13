package com.example.taskhub.service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.dto.CommonDtos.ProjectStats;
import com.example.taskhub.event.TaskEvent;
import com.example.taskhub.repository.StatusCount;
import com.example.taskhub.repository.TaskRepository;

/**
 * Caching is proxy based, so a @Cacheable method must be called from ANOTHER bean
 * (self-invocation bypasses the proxy). That's why this lives in its own service.
 */
@Service
public class StatsService {

    private final TaskRepository tasks;

    public StatsService(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Cacheable(cacheNames = "projectStats", key = "#projectId")
    @Transactional(readOnly = true)
    public ProjectStats compute(Long projectId) {
        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s, 0L);
        }
        for (StatusCount row : tasks.countByStatus(projectId)) {
            byStatus.put(row.status(), row.count());
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long overdue = tasks.countByProjectIdAndDueDateBeforeAndStatusNot(projectId, LocalDate.now(), TaskStatus.DONE);
        return new ProjectStats(projectId, total, overdue, byStatus);
    }

    /** Invalidate the cached stats once a task change has actually committed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @CacheEvict(cacheNames = "projectStats", key = "#event.projectId()")
    public void onTaskEvent(TaskEvent event) {
        // eviction is done by the annotation
    }
}
