package com.shiftleft.hub.common.util;

import java.util.UUID;

/**
 * Utility for slug generation: converts a title into a URL-safe slug
 * and appends a short unique fragment to disambiguate collisions.
 */
public final class SlugUtils {

    private static final int UNIQUE_FRAGMENT_LENGTH = 8;

    private SlugUtils() {
    }

    /**
     * Converts a free-form title into a URL-safe slug.
     *
     * <p>Steps: lowercase, strip non-[a-z0-9 -], collapse whitespace to "-",
     * collapse runs of "-", trim leading/trailing "-".</p>
     *
     * @param title the human title
     * @return a URL-safe slug
     */
    public static String slugify(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }

    /**
     * Appends a short random fragment to the given base slug, separated
     * by a hyphen. Used to disambiguate a slug that already exists.
     *
     * @param base the base slug
     * @return a new slug of the form "{base}-{8 hex chars}"
     */
    public static String withUniqueSuffix(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, UNIQUE_FRAGMENT_LENGTH);
    }
}

