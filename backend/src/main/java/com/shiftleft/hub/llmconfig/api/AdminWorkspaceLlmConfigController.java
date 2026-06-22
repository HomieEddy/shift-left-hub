package com.shiftleft.hub.llmconfig.api;

import com.shiftleft.hub.ai.api.dto.TestConnectionResult;
import com.shiftleft.hub.llmconfig.api.dto.WorkspaceLlmConfigRequest;
import com.shiftleft.hub.llmconfig.api.dto.WorkspaceLlmConfigResponse;
import com.shiftleft.hub.llmconfig.service.WorkspaceLlmConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin REST controller for managing per-workspace LLM configuration.
 * All endpoints require ADMIN role.
 * Uses workspace path parameter for multi-tenant isolation.
 */
@RestController
@RequestMapping("/api/admin/workspaces")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkspaceLlmConfigController {

    private final WorkspaceLlmConfigService workspaceLlmConfigService;

    /**
     * GET /api/admin/workspaces/{workspaceId}/llm-config
     * Returns the LLM configuration for a workspace, or 204 if not configured.
     *
     * @param workspaceId the workspace UUID
     * @return the LLM configuration response, or 204 No Content if not configured
     */
    @GetMapping("/{workspaceId}/llm-config")
    public ResponseEntity<WorkspaceLlmConfigResponse> getConfig(@PathVariable UUID workspaceId) {
        var config = workspaceLlmConfigService.getConfig(workspaceId);
        if (config == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(WorkspaceLlmConfigResponse.from(config));
    }

    /**
     * PUT /api/admin/workspaces/{workspaceId}/llm-config
     * Saves or updates the LLM configuration for a workspace.
     *
     * @param workspaceId the workspace UUID
     * @param request     the LLM configuration request
     * @return the saved LLM configuration response
     */
    @PutMapping("/{workspaceId}/llm-config")
    public ResponseEntity<WorkspaceLlmConfigResponse> saveConfig(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceLlmConfigRequest request) {
        var config = workspaceLlmConfigService.saveConfig(workspaceId, request);
        return ResponseEntity.ok(WorkspaceLlmConfigResponse.from(config));
    }

    /**
     * POST /api/admin/workspaces/{workspaceId}/llm-config/test
     * Tests a connection to the LLM provider with the given configuration.
     *
     * @param workspaceId the workspace UUID
     * @param request     the LLM configuration request
     * @return the connection test result
     */
    @PostMapping("/{workspaceId}/llm-config/test")
    public ResponseEntity<TestConnectionResult> testConnection(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceLlmConfigRequest request) {
        var result = workspaceLlmConfigService.testConnection(workspaceId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/admin/workspaces/{workspaceId}/llm-config
     * Deletes the LLM configuration for a workspace.
     *
     * @param workspaceId the workspace UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{workspaceId}/llm-config")
    public ResponseEntity<Void> deleteConfig(@PathVariable UUID workspaceId) {
        workspaceLlmConfigService.deleteConfig(workspaceId);
        return ResponseEntity.noContent().build();
    }
}
