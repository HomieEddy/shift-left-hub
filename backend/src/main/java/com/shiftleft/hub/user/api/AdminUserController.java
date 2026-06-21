package com.shiftleft.hub.user.api;

import com.shiftleft.hub.user.api.dto.RoleUpdateRequest;
import com.shiftleft.hub.user.api.dto.UserResponse;
import com.shiftleft.hub.user.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Get all users, sorted by creation date descending.
     *
     * @return list of all users
     */
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return adminUserService.getAllUsers();
    }

    /**
     * Get a user by their ID.
     *
     * @param id the user UUID
     * @return the user response
     */
    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable UUID id) {
        return adminUserService.getUserById(id);
    }

    /**
     * Update a user's role.
     *
     * @param id      the user UUID
     * @param request the role update payload
     * @return the updated user response
     */
    @PutMapping("/{id}/role")
    public UserResponse updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return adminUserService.updateUserRole(id, request.role());
    }

    /**
     * Toggle a user's enabled status.
     *
     * @param id the user UUID
     * @return the updated user response
     */
    @PutMapping("/{id}/status")
    public UserResponse toggleUserStatus(@PathVariable UUID id) {
        return adminUserService.toggleUserStatus(id);
    }
}
