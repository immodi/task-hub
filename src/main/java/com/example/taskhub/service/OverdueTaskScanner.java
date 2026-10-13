package com.example.taskhub.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.repository.TaskRepository;

/** Background job, like a BackgroundService / Hangfire recurring job. */
@Component
public class OverdueTaskScanner {

    private static final Logger log = LoggerFactory.getLogger(OverdueTaskScanner.class);

    private final TaskRepository tasks;

    public OverdueTaskScanner(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Scheduled(fixedDelayString = "${taskhub.overdue-scan-interval:PT1M}", initialDelayString = "PT30S")
    @Transactional(readOnly = true)
    public void scan() {
        long overdue = tasks.countByDueDateBeforeAndStatusNot(LocalDate.now(), TaskStatus.DONE);
        if (overdue > 0) {
            log.info("Overdue scan: {} task(s) are past their due date", overdue);
        }
    }
}
