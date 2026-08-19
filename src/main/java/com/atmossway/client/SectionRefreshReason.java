package com.atmossway.client;

enum SectionRefreshReason {
    DISABLED("disabled", true),
    ENABLED("enabled", true),
    SWAY_DISABLED("sway_disabled", true),
    SWAY_ENABLED("sway_enabled", true),
    REGION_CHANGED("region_changed", true),
    FIRST_VALID_SAMPLE("first_valid_sample", true),
    SUSTAINED_WIND("sustained_wind", false);

    private final String diagnosticName;
    private final boolean invalidatesTrackedSections;

    SectionRefreshReason(String diagnosticName, boolean invalidatesTrackedSections) {
        this.diagnosticName = diagnosticName;
        this.invalidatesTrackedSections = invalidatesTrackedSections;
    }

    String diagnosticName() {
        return diagnosticName;
    }

    boolean invalidatesTrackedSections() {
        return invalidatesTrackedSections;
    }
}
