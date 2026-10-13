package com.example.taskhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.taskhub.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByTaskId(Long taskId, Pageable pageable);

    void deleteByTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Comment c where c.task.id in (select t.id from Task t where t.project.id = :pid)")
    int deleteByProjectId(@Param("pid") Long projectId);
}
