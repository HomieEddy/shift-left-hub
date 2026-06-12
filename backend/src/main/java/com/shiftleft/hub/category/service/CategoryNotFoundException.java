package com.shiftleft.hub.category.service;

import java.util.UUID;

/**
 * Thrown when a category is not found by its ID within the current workspace.
 */
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID id) {
        super("Category not found: " + id);
    }
}