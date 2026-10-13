package com.example.taskhub.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.taskhub.domain.ActivityLog;
import com.example.taskhub.repository.ActivityLogRepository;

/**
 * Runs only after the publishing transaction committed, on a separate (virtual) thread,
 * in its own new transaction. Similar to a MediatR notification handler / domain event dispatcher.
 */
@Component
public class ActivityLogListener {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogListener.class);

    private final ActivityLogRepository repository;

    public ActivityLogListener(ActivityLogRepository repository) {
        this.repository = repository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(TaskEvent event) {
        ActivityLog entry = switch (event) {
            case TaskEvent.Created e -> entry(e, "TASK_CREATED", e.taskId(), "Created '" + e.title() + "'");
            case TaskEvent.StatusChanged e -> entry(e, "TASK_STATUS_CHANGED", e.taskId(), e.from() + " -> " + e.to());
            case TaskEvent.Assigned e -> entry(e, "TASK_ASSIGNED", e.taskId(),
                    e.assigneeId() == null ? "Unassigned" : "Assigned to user " + e.assigneeId());
            case TaskEvent.Deleted e -> entry(e, "TASK_DELETED", e.taskId(), "Deleted '" + e.title() + "'");
            case TaskEvent.Commented e -> entry(e, "COMMENT_ADDED", e.taskId(), "Comment " + e.commentId());
        };
        repository.save(entry);
        log.debug("Activity recorded: {} on task {}", entry.getAction(), entry.getEntityId());
    }

    private static ActivityLog entry(TaskEvent e, String action, Long entityId, String details) {
        ActivityLog a = new ActivityLog();
        a.setProjectId(e.projectId());
        a.setActorId(e.actorId());
        a.setAction(action);
        a.setEntityId(entityId);
        a.setDetails(details);
        return a;
    }
}
