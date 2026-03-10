package tests;

import domain.fatigue.*;

import java.time.LocalDate;
import java.util.List;

public class MotorFatigaTest {

    public static void main(String[] args) {

        List<TrainingSession> sessions = List.of(

                new TrainingSession(1L, LocalDate.now(), 60, 7),
                new TrainingSession(1L, LocalDate.now().minusDays(1), 50, 8),
                new TrainingSession(1L, LocalDate.now().minusDays(2), 70, 6),
                new TrainingSession(1L, LocalDate.now().minusDays(4), 80, 7),
                new TrainingSession(1L, LocalDate.now().minusDays(5), 45, 7),
                new TrainingSession(1L, LocalDate.now().minusDays(6), 60, 8)

        );

        MotorFatiga motor = new MotorFatiga();

        FatigueMetrics metrics = motor.calculate(sessions, LocalDate.now());

        System.out.println("ATL: " + metrics.getAtl());
        System.out.println("CTL: " + metrics.getCtl());
        System.out.println("ACWR: " + metrics.getAcwr());
        System.out.println("TSB: " + metrics.getTsb());
    }
}
