package com.shiftleft.hub.category.service;

import java.util.UUID;

public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(UUID id, String name, long count) {
        super("Category '" + name + "' (" + id + ") is used by " + count + " items. Reassign before deleting.");
    }
}