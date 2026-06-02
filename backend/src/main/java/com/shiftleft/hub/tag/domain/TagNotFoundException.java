package com.shiftleft.hub.tag.domain;

import java.util.UUID;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(UUID id) {
        super("Tag not found: " + id);
    }
}
