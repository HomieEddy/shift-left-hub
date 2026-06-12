package com.shiftleft.hub.llmconfig.domain;

import com.shiftleft.hub.common.domain.WorkspaceAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-workspace LLM configuration entity.
 * Each workspace can have its own LLM provider, endpoint, and model settings.
 * Falls back to global AiConfig defaults if no workspace config exists (D-18).
 * API key is encrypted at rest via Spring TextEncryptor (D-20).
 */
@Entity
@Table(name = "workspace_llm_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceLlmConfig extends WorkspaceAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "llm_provider", nullable = false, length = 32)
    @Builder.Default
    private LlmProvider llmProvider = LlmProvider.OLLAMA;

    @Column(name = "endpoint_url", length = 512)
    private String endpointUrl;

    @Column(name = "api_key", length = 512)
    private String apiKey;

    @Column(name = "model_name", nullable = false, length = 128)
    @Builder.Default
    private String modelName = "llama3.2";

    @Column(name = "embedding_model_name", nullable = false, length = 128)
    @Builder.Default
    private String embeddingModelName = "nomic-embed-text";

    @Column(name = "similarity_threshold", nullable = false)
    @Builder.Default
    private Double similarityThreshold = 0.65;

    @Column(name = "embedding_dimension", nullable = false)
    @Builder.Default
    private Integer embeddingDimension = 768;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkspaceLlmConfig that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
