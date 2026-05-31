package com.shiftleft.hub.user.service;

import com.shiftleft.hub.user.api.dto.UserResponse;
import com.shiftleft.hub.user.domain.UserNotFoundException;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(UserResponse::from)
            .sorted(Comparator.comparing(UserResponse::createdAt).reversed())
            .toList();
    }

    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
            .map(UserResponse::from)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserResponse updateUserRole(UUID id, UserRole newRole) {
        var currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        if (user.getEmail().equals(currentUserEmail)) {
            throw new IllegalStateException("Cannot modify your own role");
        }
        user.setRole(newRole);
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse toggleUserStatus(UUID id) {
        var currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        if (user.getEmail().equals(currentUserEmail)) {
            throw new IllegalStateException("Cannot modify your own account status");
        }
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return UserResponse.from(user);
    }
}
