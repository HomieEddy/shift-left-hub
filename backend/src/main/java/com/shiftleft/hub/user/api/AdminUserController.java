package com.shiftleft.hub.user.api;

import com.shiftleft.hub.user.api.dto.RoleUpdateRequest;
import com.shiftleft.hub.user.api.dto.UserResponse;
import com.shiftleft.hub.user.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return adminUserService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable UUID id) {
        return adminUserService.getUserById(id);
    }

    @PutMapping("/{id}/role")
    public UserResponse updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return adminUserService.updateUserRole(id, request.role());
    }

    @PutMapping("/{id}/status")
    public UserResponse toggleUserStatus(@PathVariable UUID id) {
        return adminUserService.toggleUserStatus(id);
    }
}
