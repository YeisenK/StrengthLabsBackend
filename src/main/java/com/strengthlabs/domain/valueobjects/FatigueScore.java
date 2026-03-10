package com.strengthlabs.domain.valueobjects;

public final class FatigueScore {
    private final double value;

    private FatigueScore(double value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("FatigueScore must be between 0 and 100");
        }
        this.value = value;
    }

    public static FatigueScore of(double value) {
        return new FatigueScore(value);
    }

    public boolean isCritical() {
        return value >= 80;
    }

    public boolean isModerate() {
        return value >= 50 && value < 80;
    }

    public double getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FatigueScore)) return false;
        FatigueScore that = (FatigueScore) o;
        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }
}
