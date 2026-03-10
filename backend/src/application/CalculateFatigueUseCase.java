package application;

import domain.fatigue.*;

import java.time.LocalDate;
import java.util.List;

public class CalculateFatigueUseCase {

    private final MotorFatiga motorFatiga;

    public CalculateFatigueUseCase() {
        this.motorFatiga = new MotorFatiga();
    }

    public FatigueMetrics execute(List<TrainingSession> sessions) {

        LocalDate today = LocalDate.now();

        return motorFatiga.calculate(sessions, today);
    }
}
