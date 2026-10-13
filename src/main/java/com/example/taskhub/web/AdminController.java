package com.example.taskhub.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskhub.domain.Role;
import com.example.taskhub.dto.CommonDtos.ActivityDto;
import com.example.taskhub.dto.CommonDtos.PageResponse;
import com.example.taskhub.dto.CommonDtos.UserDto;
import com.example.taskhub.service.AdminService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;

/** Protected twice on purpose: URL rule in SecurityConfig and method-level @PreAuthorize. */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    public record RoleChangeRequest(@NotNull Role role) {}

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    public PageResponse<UserDto> users(@PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return admin.listUsers(pageable);
    }

    @PatchMapping("/users/{id}/role")
    public UserDto changeRole(@PathVariable Long id, @RequestBody RoleChangeRequest req) {
        return admin.changeRole(id, req.role());
    }

    @GetMapping("/activity")
    public PageResponse<ActivityDto> activity(@RequestParam(required = false) Long projectId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return admin.activity(projectId, pageable);
    }
}
