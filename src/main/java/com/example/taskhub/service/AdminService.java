package com.example.taskhub.service;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskhub.domain.Role;
import com.example.taskhub.domain.User;
import com.example.taskhub.dto.CommonDtos.ActivityDto;
import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.dto.CommonDtos.UserDto;
import com.example.taskhub.dto.Mappers;
import com.example.taskhub.exception.ApiException;
import com.example.taskhub.repository.ActivityLogRepository;
import com.example.taskhub.repository.UserRepository;

@Service
@Transactional
public class AdminService {

    private static final Set<String> USER_SORTABLE = Set.of("id", "username", "email", "role", "createdAt");
    private static final Set<String> ACTIVITY_SORTABLE = Set.of("id", "projectId", "actorId", "action", "createdAt");

    private final UserRepository users;
    private final ActivityLogRepository activity;

    public AdminService(UserRepository users, ActivityLogRepository activity) {
        this.users = users;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(Pageable pageable) {
        Sorting.validate(pageable, USER_SORTABLE);
        return PageResponse.from(users.findAll(pageable).map(Mappers::userDto));
    }

    /** Note: existing JWTs keep the old role until they expire (roles are baked into the token). */
    public UserDto changeRole(Long userId, Role role) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User " + userId + " not found"));
        u.setRole(role);
        return Mappers.userDto(users.saveAndFlush(u));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityDto> activity(Long projectId, Pageable pageable) {
        Sorting.validate(pageable, ACTIVITY_SORTABLE);
        var page = projectId == null ? activity.findAll(pageable) : activity.findByProjectId(projectId, pageable);
        return PageResponse.from(page.map(Mappers::activityDto));
    }
}
