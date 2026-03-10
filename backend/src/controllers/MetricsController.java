package controllers;

import application.CalculateFatigueUseCase;
import domain.fatigue.*;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final CalculateFatigueUseCase useCase = new CalculateFatigueUseCase();

    @PostMapping("/fatigue")
    public FatigueMetrics calculateFatigue(@RequestBody List<TrainingSession> sessions) {

        return useCase.execute(sessions);
    }
}
