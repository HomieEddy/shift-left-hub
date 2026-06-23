package com.shiftleft.hub.user.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfModificationExceptionTest {

    @Test
    void role_factory_includes_user_id() {
        UUID id = UUID.randomUUID();
        SelfModificationException ex = SelfModificationException.role(id);
        assertTrue(ex.getMessage().contains(id.toString()));
        assertTrue(ex.getMessage().contains("role"));
    }

    @Test
    void status_factory_includes_user_id() {
        UUID id = UUID.randomUUID();
        SelfModificationException ex = SelfModificationException.status(id);
        assertTrue(ex.getMessage().contains(id.toString()));
        assertTrue(ex.getMessage().contains("status"));
    }

    @Test
    void is_runtime_exception() {
        assertEquals(RuntimeException.class, SelfModificationException.class.getSuperclass());
    }
}
