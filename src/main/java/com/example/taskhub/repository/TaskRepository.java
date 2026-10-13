package com.example.taskhub.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.taskhub.domain.Task;
import com.example.taskhub.domain.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByProjectId(Long projectId);

    long countByProjectIdAndDueDateBeforeAndStatusNot(Long projectId, LocalDate date, TaskStatus status);

    long countByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);

    @Query("select new com.example.taskhub.repository.StatusCount(t.status, count(t)) "
            + "from Task t where t.project.id = :pid group by t.status")
    List<StatusCount> countByStatus(@Param("pid") Long projectId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Task t set t.assignee = null where t.project.id = :pid and t.assignee.id = :uid")
    int clearAssignee(@Param("pid") Long projectId, @Param("uid") Long userId);
}
