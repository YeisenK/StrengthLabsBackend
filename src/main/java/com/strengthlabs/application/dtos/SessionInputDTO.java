package com.strengthlabs.application.dtos;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class SessionInputDTO {

    @NotNull
    private LocalDate date;

    @NotBlank
    private String muscleGroup;

    @Min(1) @Max(20)
    private int sets;

    @Min(1) @Max(30)
    private int repsPerSet;

    @DecimalMin("0.0")
    private double weightKg;

    @Min(0) @Max(5)
    private int rirAverage;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }
    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }
    public int getRepsPerSet() { return repsPerSet; }
    public void setRepsPerSet(int repsPerSet) { this.repsPerSet = repsPerSet; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public int getRirAverage() { return rirAverage; }
    public void setRirAverage(int rirAverage) { this.rirAverage = rirAverage; }
}
