package com.shiftleft.hub.tag.domain;

import java.util.UUID;

/**
 * Thrown when attempting to delete a tag that is still in use by articles.
 */
public class TagInUseException extends RuntimeException {

    /**
     * Creates a new TagInUseException for the given tag.
     *
     * @param tagId        the tag UUID
     * @param tagName      the tag display name
     * @param articleCount the number of articles using the tag
     */
    public TagInUseException(UUID tagId, String tagName, long articleCount) {
        super("Cannot delete tag '" + tagName + "' (id: " + tagId + "): used by " + articleCount + " article(s)");
    }
}
