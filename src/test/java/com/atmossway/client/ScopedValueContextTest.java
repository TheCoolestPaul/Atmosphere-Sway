package com.atmossway.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopedValueContextTest {
    private final ScopedValueContext<String> context = new ScopedValueContext<>();

    @AfterEach
    void resetContext() {
        context.reset();
    }

    @Test
    void publishesAndClearsAReplacement() {
        var entered = context.begin(null, "render-position");
        assertTrue(entered.changed());
        assertEquals("render-position", entered.value());

        var exited = context.end(entered.value());
        assertTrue(exited.changed());
        assertNull(exited.value());
    }

    @Test
    void restoresAPreexistingValue() {
        var entered = context.begin("previous", "render-position");
        var exited = context.end(entered.value());

        assertTrue(exited.changed());
        assertEquals("previous", exited.value());
    }

    @Test
    void nestedScopesRestoreEachValueInOrder() {
        var outer = context.begin(null, "outer");
        var inner = context.begin(outer.value(), "inner");
        assertEquals("inner", inner.value());

        var afterInner = context.end(inner.value());
        assertEquals("outer", afterInner.value());

        var afterOuter = context.end(afterInner.value());
        assertNull(afterOuter.value());
    }

    @Test
    void missingReplacementLeavesTheCurrentValueUntouched() {
        var entered = context.begin("previous", null);
        assertFalse(entered.changed());
        assertEquals("previous", entered.value());

        var exited = context.end("current");
        assertFalse(exited.changed());
        assertEquals("current", exited.value());
    }
}
