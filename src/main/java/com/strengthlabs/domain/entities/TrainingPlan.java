package com.strengthlabs.domain.entities;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TrainingPlan {
    private UUID id;
    private UUID userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<PlannedSession> sessions;
    private boolean active;

    public static class PlannedSession {
        private final LocalDate date;
        private final String muscleGroup;
        private final int targetSets;
        private final int targetReps;
        private final double targetWeightKg;

        public PlannedSession(LocalDate date, String muscleGroup,
                              int targetSets, int targetReps, double targetWeightKg) {
            this.date = date;
            this.muscleGroup = muscleGroup;
            this.targetSets = targetSets;
            this.targetReps = targetReps;
            this.targetWeightKg = targetWeightKg;
        }

        public LocalDate getDate() { return date; }
        public String getMuscleGroup() { return muscleGroup; }
        public int getTargetSets() { return targetSets; }
        public int getTargetReps() { return targetReps; }
        public double getTargetWeightKg() { return targetWeightKg; }
    }

    public TrainingPlan(UUID id, UUID userId, LocalDate startDate,
                        LocalDate endDate, List<PlannedSession> sessions) {
        this.id = id;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.sessions = sessions;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    public List<PlannedSession> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return active; }
}
