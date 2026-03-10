package com.strengthlabs.domain.valueobjects;

public final class RiskLevel {
    public enum Zone { GREEN, YELLOW, RED }

    private final Zone zone;
    private final String reason;

    private RiskLevel(Zone zone, String reason) {
        this.zone = zone;
        this.reason = reason;
    }

    public static RiskLevel green() {
        return new RiskLevel(Zone.GREEN, "No significant injury risk detected");
    }

    public static RiskLevel yellow(String reason) {
        return new RiskLevel(Zone.YELLOW, reason);
    }

    public static RiskLevel red(String reason) {
        return new RiskLevel(Zone.RED, reason);
    }

    public boolean requiresAlert() {
        return zone == Zone.YELLOW || zone == Zone.RED;
    }

    public boolean isTrainingBlocked() {
        return zone == Zone.RED;
    }

    public Zone getZone() { return zone; }
    public String getReason() { return reason; }
}
