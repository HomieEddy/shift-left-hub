package com.shiftleft.hub.category.service;

import java.util.UUID;

/**
 * Thrown when attempting to delete a category that still has associated content or children.
 */
public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(UUID id, String name, long count) {
        super("Category '" + name + "' (" + id + ") is used by " + count + " items. Reassign before deleting.");
    }
}