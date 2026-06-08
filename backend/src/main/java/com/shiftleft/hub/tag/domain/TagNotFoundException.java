package com.shiftleft.hub.tag.domain;

import java.util.UUID;

/**
 * Thrown when a tag is not found by its ID.
 */
public class TagNotFoundException extends RuntimeException {

    /**
     * Creates a new TagNotFoundException for the given ID.
     *
     * @param id the tag UUID that was not found
     */
    public TagNotFoundException(UUID id) {
        super("Tag not found: " + id);
    }
}
