package com.example.taskhub.service;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.repository.TaskRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Background job, like a BackgroundService / Hangfire recurring job. It periodically counts the
 * tasks that are past their due date and publishes the result as a Micrometer gauge, so the number
 * is scrapeable at {@code /actuator/metrics/taskhub.overdue.tasks}.
 */
@Component
public class OverdueTaskScanner {

    private static final Logger log = LoggerFactory.getLogger(OverdueTaskScanner.class);

    /** Metric name, exposed at /actuator/metrics/taskhub.overdue.tasks. */
    public static final String METRIC = "taskhub.overdue.tasks";

    private final TaskRepository tasks;
    private final AtomicLong overdue = new AtomicLong();

    public OverdueTaskScanner(TaskRepository tasks, MeterRegistry meters) {
        this.tasks = tasks;
        // The gauge reads the cached value; the scheduled scan refreshes it, so metric scrapes stay cheap.
        Gauge.builder(METRIC, overdue, AtomicLong::get)
                .description("Tasks past their due date that are not done")
                .register(meters);
    }

    @Scheduled(fixedDelayString = "${taskhub.overdue-scan-interval:PT1M}", initialDelayString = "PT30S")
    @Transactional(readOnly = true)
    public void scan() {
        long count = tasks.countByDueDateBeforeAndStatusNot(LocalDate.now(), TaskStatus.DONE);
        overdue.set(count);
        if (count > 0) {
            log.info("Overdue scan: {} task(s) are past their due date", count);
        }
    }
}
