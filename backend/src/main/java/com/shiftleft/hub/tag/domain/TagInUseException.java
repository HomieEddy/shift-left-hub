package com.shiftleft.hub.tag.domain;

import java.util.UUID;

public class TagInUseException extends RuntimeException {

    public TagInUseException(UUID tagId, String tagName, long articleCount) {
        super("Cannot delete tag '" + tagName + "' (id: " + tagId + "): used by " + articleCount + " article(s)");
    }
}
