package com.shiftleft.hub.ai.api;

import com.shiftleft.hub.ai.api.dto.AiConfigRequest;
import com.shiftleft.hub.ai.api.dto.AiConfigResponse;
import com.shiftleft.hub.ai.api.dto.TestConnectionResult;
import com.shiftleft.hub.ai.service.AiConfigService;
import com.shiftleft.hub.ai.service.EmbeddingService;
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

    @GetMapping
    public AiConfigResponse getConfig() {
        return aiConfigService.getConfig();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AiConfigResponse updateConfig(@RequestBody AiConfigRequest request) {
        return aiConfigService.updateConfig(request);
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public TestConnectionResult testConnection(@RequestBody AiConfigRequest request) {
        return aiConfigService.testConnection(request);
    }

    @PostMapping("/embeddings/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> reindexEmbeddings() {
        embeddingService.reEmbedAll();
        return Map.of("message", "Re-embedding started");
    }
}
