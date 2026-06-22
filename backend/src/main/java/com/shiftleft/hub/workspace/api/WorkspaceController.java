package com.shiftleft.hub.workspace.api;

import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.user.domain.UserNotFoundException;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.workspace.api.dto.WorkspaceResponse;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    /**
     * Lists workspaces the current user belongs to.
     *
     * @param userDetails the authenticated user
     * @return list of workspace responses
     */
    @GetMapping("/mine")
    public ResponseEntity<List<WorkspaceResponse>> listMyWorkspaces(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UserNotFoundException());
        List<Workspace> workspaces = workspaceService.findWorkspacesByUserId(user.getId());
        List<WorkspaceResponse> responses = workspaces.stream()
            .map(w -> new WorkspaceResponse(
                w.getId(), w.getName(), w.getSlug(),
                w.getDescription(), w.getLogoUrl(), w.getIcon(),
                workspaceService.getMemberCount(w.getId()),
                w.getCreatedBy(), w.getCreatedAt(), w.getUpdatedAt()))
            .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Returns the current user's role in the active workspace.
     *
     * @param userDetails the authenticated user
     * @return role response with role string
     */
    @GetMapping("/current/role")
    public ResponseEntity<Map<String, String>> getCurrentRole(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UserNotFoundException());
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        String role = workspaceService.getUserRole(workspaceId, user.getId()).orElse("NONE");
        return ResponseEntity.ok(Map.of("role", role));
    }

    /**
     * Leaves the specified workspace. Cannot leave Default Workspace.
     *
     * @param id the workspace UUID
     * @param userDetails the authenticated user
     * @return HTTP 200 on success
     */
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveWorkspace(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UserNotFoundException());
        workspaceService.leaveWorkspace(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
