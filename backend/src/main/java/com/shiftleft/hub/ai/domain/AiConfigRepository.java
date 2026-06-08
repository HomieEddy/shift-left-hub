package com.shiftleft.hub.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AiConfig} entities.
 */
public interface AiConfigRepository extends JpaRepository<AiConfig, UUID> {

    @Query("SELECT c FROM AiConfig c ORDER BY c.id LIMIT 1")
    Optional<AiConfig> findSingleConfig();

    @Modifying
    @Query("DELETE FROM AiConfig c")
    void deleteAll();
}
