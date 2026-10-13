package com.example.taskhub.seed;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.Comment;
import com.example.taskhub.domain.Priority;
import com.example.taskhub.domain.Project;
import com.example.taskhub.domain.Role;
import com.example.taskhub.domain.Tag;
import com.example.taskhub.domain.Task;
import com.example.taskhub.domain.TaskStatus;
import com.example.taskhub.domain.User;
import com.example.taskhub.repository.CommentRepository;
import com.example.taskhub.repository.ProjectRepository;
import com.example.taskhub.repository.TagRepository;
import com.example.taskhub.repository.TaskRepository;
import com.example.taskhub.repository.UserRepository;

/** Seeds demo data on startup (not in the "test" profile). Logins: admin/Password1, alice/Password1, bob/Password1. */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final TagRepository tags;
    private final CommentRepository comments;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, ProjectRepository projects, TaskRepository tasks, TagRepository tags,
                      CommentRepository comments, PasswordEncoder encoder) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.tags = tags;
        this.comments = comments;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin = user("admin", "admin@example.com", Role.ADMIN);
        User alice = user("alice", "alice@example.com", Role.USER);
        User bob = user("bob", "bob@example.com", Role.USER);

        Project site = new Project();
        site.setName("Website Redesign");
        site.setDescription("Refresh the marketing site");
        site.setOwner(alice);
        site.getMembers().add(bob);
        projects.save(site);

        Project infra = new Project();
        infra.setName("Infra Cleanup");
        infra.setDescription("Alice-only project, Bob can't see it");
        infra.setOwner(alice);
        projects.save(infra);

        Map<String, Tag> tagCache = new HashMap<>();
        Task t1 = task(site, alice, bob, "Design new landing page", TaskStatus.IN_PROGRESS, Priority.HIGH,
                LocalDate.now().plusDays(5), tagCache, "design", "frontend");
        task(site, alice, alice, "Set up CI pipeline", TaskStatus.TODO, Priority.MEDIUM,
                LocalDate.now().plusDays(10), tagCache, "devops");
        task(site, bob, null, "Fix broken contact form", TaskStatus.BLOCKED, Priority.CRITICAL,
                LocalDate.now().minusDays(2), tagCache, "bug", "frontend");
        task(site, bob, bob, "Write launch blog post", TaskStatus.DONE, Priority.LOW,
                LocalDate.now().minusDays(7), tagCache, "content");
        task(infra, alice, alice, "Rotate old credentials", TaskStatus.TODO, Priority.HIGH,
                LocalDate.now().minusDays(1), tagCache, "security");

        Comment c = new Comment();
        c.setTask(t1);
        c.setAuthor(bob);
        c.setBody("I have a first mockup ready for review.");
        comments.save(c);

        log.info("Seeded demo data. Users: admin, alice, bob (password: Password1)");
    }

    private User user(String name, String email, Role role) {
        User u = new User();
        u.setUsername(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("Password1"));
        u.setRole(role);
        return users.save(u);
    }

    private Task task(Project p, User reporter, User assignee, String title, TaskStatus status, Priority prio,
                      LocalDate due, Map<String, Tag> cache, String... tagNames) {
        Task t = new Task();
        t.setProject(p);
        t.setReporter(reporter);
        t.setAssignee(assignee);
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(prio);
        t.setDueDate(due);
        for (String n : tagNames) {
            t.getTags().add(cache.computeIfAbsent(n, k -> {
                Tag tag = new Tag();
                tag.setName(k);
                return tags.save(tag);
            }));
        }
        return tasks.save(t);
    }
}
