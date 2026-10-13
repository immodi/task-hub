package com.example.taskhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.taskhub.domain.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query(value = "select distinct p from Project p left join p.members m where p.owner.id = :uid or m.id = :uid",
           countQuery = "select count(distinct p) from Project p left join p.members m where p.owner.id = :uid or m.id = :uid")
    Page<Project> findAccessible(@Param("uid") Long userId, Pageable pageable);

    @Query("select count(p) > 0 from Project p left join p.members m where p.id = :pid and (p.owner.id = :uid or m.id = :uid)")
    boolean hasAccess(@Param("pid") Long projectId, @Param("uid") Long userId);
}
