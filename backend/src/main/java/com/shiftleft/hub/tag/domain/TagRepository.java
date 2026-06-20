package com.shiftleft.hub.tag.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link Tag} entities.
 */
public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByNameEnIn(Collection<String> nameEn);

    Optional<Tag> findByNameEnAndWorkspaceId(String nameEn, UUID workspaceId);
}
