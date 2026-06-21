package com.shiftleft.hub.ai.api;

import com.shiftleft.hub.ai.api.dto.AiConfigRequest;
import com.shiftleft.hub.ai.api.dto.AiConfigResponse;
import com.shiftleft.hub.ai.api.dto.TestConnectionResult;
import com.shiftleft.hub.ai.service.AiConfigService;
import com.shiftleft.hub.ai.service.EmbeddingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;
    private final EmbeddingService embeddingService;

    /**
     * Returns the current AI configuration.
     *
     * @return current AI configuration response
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AiConfigResponse getConfig() {
        return aiConfigService.getConfig();
    }

    /**
     * Updates the AI configuration.
     *
     * @param request the configuration update request
     * @return updated AI configuration response
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AiConfigResponse updateConfig(@Valid @RequestBody AiConfigRequest request) {
        return aiConfigService.updateConfig(request);
    }

    /**
     * Tests a connection to the AI provider with the given configuration.
     *
     * @param request the configuration to test
     * @return connection test result
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public TestConnectionResult testConnection(@Valid @RequestBody AiConfigRequest request) {
        return aiConfigService.testConnection(request);
    }

    /**
     * Triggers a full re-index of all embeddings.
     *
     * @return status message
     */
    @PostMapping("/embeddings/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> reindexEmbeddings() {
        embeddingService.reEmbedAll();
        return Map.of("message", "Re-embedding started");
    }
}
