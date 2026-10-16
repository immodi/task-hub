package com.example.taskhub.web;

import org.springdoc.core.annotations.ParameterObject;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Protected twice on purpose: URL rule in SecurityConfig and method-level @PreAuthorize. */
@Tag(name = "Admin", description = "Role-protected administration endpoints (ADMIN only)")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    public record RoleChangeRequest(@NotNull Role role) {}

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @Operation(summary = "List users", description = "Sortable by id, username, email, role, createdAt.")
    @ApiResponse(responseCode = "200", description = "A page of users")
    @ApiResponse(responseCode = "403", description = "Not an admin")
    @GetMapping("/users")
    public PageResponse<UserDto> users(@ParameterObject @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return admin.listUsers(pageable);
    }

    @Operation(summary = "Change a user's role",
            description = "Takes effect on the next login: roles are baked into the existing JWT.")
    @ApiResponse(responseCode = "200", description = "Role changed")
    @ApiResponse(responseCode = "400", description = "Missing or unknown role")
    @ApiResponse(responseCode = "404", description = "No such user")
    @PatchMapping("/users/{id}/role")
    public UserDto changeRole(@PathVariable Long id, @Valid @RequestBody RoleChangeRequest req) {
        return admin.changeRole(id, req.role());
    }

    @Operation(summary = "Activity log",
            description = "Newest first. Optionally filter by `projectId`; sortable by id, projectId, actorId, "
                    + "action, createdAt.")
    @ApiResponse(responseCode = "200", description = "A page of activity entries")
    @GetMapping("/activity")
    public PageResponse<ActivityDto> activity(@RequestParam(required = false) Long projectId,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return admin.activity(projectId, pageable);
    }
}
