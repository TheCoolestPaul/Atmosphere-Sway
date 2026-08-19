package com.atmossway.client;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionRefreshReasonTest {
    @Test
    void sustainedWindDoesNotInvalidateTrackedSections() {
        assertFalse(SectionRefreshReason.SUSTAINED_WIND.invalidatesTrackedSections());
    }

    @Test
    void lifecycleChangesStillInvalidateTrackedSections() {
        EnumSet.complementOf(EnumSet.of(SectionRefreshReason.SUSTAINED_WIND))
                .forEach(reason -> assertTrue(reason.invalidatesTrackedSections(), reason.name()));
    }
}
