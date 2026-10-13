package com.example.taskhub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Audit trail, written asynchronously by ActivityLogListener in response to domain events. */
@Entity
@Table(name = "activity_log")
public class ActivityLog extends BaseEntity {

    private Long projectId;
    private Long actorId;
    private Long entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String details;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
