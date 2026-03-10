package domain.fatigue;

import java.time.LocalDate;

public class TrainingSession {

    private Long userId;
    private LocalDate date;
    private int durationMinutes;
    private double rpe;

    public TrainingSession(Long userId, LocalDate date, int durationMinutes, double rpe) {
        this.userId = userId;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.rpe = rpe;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public double getRpe() {
        return rpe;
    }

    public double getLoad() {
        return durationMinutes * rpe;
    }
}
