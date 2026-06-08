package com.shiftleft.hub.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA entity representing the singleton AI configuration.
 */
@Entity
@Table(name = "ai_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String llmProvider;

    @Column(nullable = false)
    private String ollamaEndpointUrl;

    private String openaiApiKey;

    @Column(nullable = false)
    private String chatModelName;

    @Column(nullable = false)
    private String embeddingModelName;

    @Column(nullable = false)
    private double similarityThreshold;

    @Column(nullable = false)
    private int embeddingDimension;
}
