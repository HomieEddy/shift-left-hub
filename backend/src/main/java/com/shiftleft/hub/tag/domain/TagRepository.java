package com.shiftleft.hub.tag.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing {@link Tag} entities.
 */
public interface TagRepository extends JpaRepository<Tag, UUID> {

    /**
     * Finds tags by their English names.
     *
     * @param nameEn the collection of English names to search for
     * @return the list of matching tags
     */
    List<Tag> findByNameEnIn(Collection<String> nameEn);
}
