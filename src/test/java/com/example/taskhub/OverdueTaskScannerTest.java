package com.example.taskhub;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.Priority;
import com.example.taskhub.domain.User;
import com.example.taskhub.dto.ProjectDtos.ProjectCreateRequest;
import com.example.taskhub.dto.ProjectDtos.ProjectDto;
import com.example.taskhub.dto.TaskDtos.TaskCreateRequest;
import com.example.taskhub.dto.TaskDtos.TaskUpdateRequest;
import com.example.taskhub.repository.UserRepository;
import com.example.taskhub.security.AuthUser;
import com.example.taskhub.service.OverdueTaskScanner;
import com.example.taskhub.service.ProjectService;
import com.example.taskhub.service.TaskService;

import io.micrometer.core.instrument.MeterRegistry;

/** The scheduled scan refreshes the {@code taskhub.overdue.tasks} gauge that Actuator exposes. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OverdueTaskScannerTest {

    @Autowired UserRepository users;
    @Autowired ProjectService projects;
    @Autowired TaskService tasks;
    @Autowired OverdueTaskScanner scanner;
    @Autowired MeterRegistry meters;

    AuthUser alice;
    ProjectDto project;

    @BeforeEach
    void setUp() {
        User u = new User();
        u.setUsername("scanner_alice");
        u.setEmail("scanner_alice@test.dev");
        u.setPasswordHash("not-a-real-hash");
        users.saveAndFlush(u);
        alice = new AuthUser(u.getId(), u.getUsername(), false);
        project = projects.create(new ProjectCreateRequest("Scanner", null), alice);
    }

    @Test
    void scanRefreshesTheOverdueGauge() {
        scanner.scan();
        double before = gaugeValue();

        var overdue = tasks.create(project.id(), new TaskCreateRequest("Late", null, Priority.HIGH, null, null, Set.of()), alice);
        // Creation rejects past dates, so make it overdue through a full update.
        tasks.update(overdue.id(), new TaskUpdateRequest("Late", null, Priority.HIGH, LocalDate.now().minusDays(1), Set.of(), overdue.version()), alice);

        scanner.scan();

        assertThat(gaugeValue()).isEqualTo(before + 1);
    }

    private double gaugeValue() {
        return meters.get(OverdueTaskScanner.METRIC).gauge().value();
    }
}
