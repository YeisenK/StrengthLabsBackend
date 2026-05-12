package com.strengthlabs.infrastructure.persistence;

import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Order(1)
public class ExerciseDataSeeder implements ApplicationRunner {

    private final ExerciseJpaRepository exerciseRepo;

    public ExerciseDataSeeder(ExerciseJpaRepository exerciseRepo) {
        this.exerciseRepo = exerciseRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!exerciseRepo.findByIsCustomFalse().isEmpty()) return;

        // {name_en, muscle_group, name_es}
        List<Object[]> exercises = List.of(
            // Chest
            new Object[]{"Barbell Bench Press",         "chest",     "Press de banca con barra"},
            new Object[]{"Incline Barbell Press",       "chest",     "Press inclinado con barra"},
            new Object[]{"Incline Dumbbell Press",      "chest",     "Press inclinado con mancuernas"},
            new Object[]{"Dumbbell Bench Press",        "chest",     "Press de banca con mancuernas"},
            new Object[]{"Cable Fly",                   "chest",     "Apertura en polea"},
            new Object[]{"Push-up",                     "chest",     "Flexión de pecho"},
            new Object[]{"Dip",                         "chest",     "Fondos en paralelas"},
            // Back
            new Object[]{"Barbell Deadlift",            "back",      "Peso muerto con barra"},
            new Object[]{"Barbell Row",                 "back",      "Remo con barra"},
            new Object[]{"Pendlay Row",                 "back",      "Remo Pendlay"},
            new Object[]{"Dumbbell Row",                "back",      "Remo con mancuerna"},
            new Object[]{"Lat Pulldown",                "back",      "Jalón al pecho"},
            new Object[]{"Seated Cable Row",            "back",      "Remo en polea sentado"},
            new Object[]{"Pull-up",                     "back",      "Dominada"},
            new Object[]{"Weighted Pull-up",            "back",      "Dominada con peso"},
            new Object[]{"T-Bar Row",                   "back",      "Remo en T"},
            new Object[]{"Romanian Deadlift",           "back",      "Peso muerto rumano"},
            // Shoulders
            new Object[]{"Overhead Press",              "shoulders", "Press militar"},
            new Object[]{"Dumbbell Shoulder Press",     "shoulders", "Press de hombro con mancuernas"},
            new Object[]{"Arnold Press",                "shoulders", "Press Arnold"},
            new Object[]{"Lateral Raise",               "shoulders", "Elevación lateral"},
            new Object[]{"Front Raise",                 "shoulders", "Elevación frontal"},
            new Object[]{"Face Pull",                   "shoulders", "Jalón facial"},
            new Object[]{"Rear Delt Fly",               "shoulders", "Apertura deltoides posterior"},
            // Biceps
            new Object[]{"Barbell Curl",                "biceps",    "Curl con barra"},
            new Object[]{"Dumbbell Curl",               "biceps",    "Curl con mancuerna"},
            new Object[]{"EZ Bar Curl",                 "biceps",    "Curl con barra EZ"},
            new Object[]{"Hammer Curl",                 "biceps",    "Curl martillo"},
            new Object[]{"Preacher Curl",               "biceps",    "Curl en predicador"},
            new Object[]{"Incline Dumbbell Curl",       "biceps",    "Curl inclinado con mancuerna"},
            // Triceps
            new Object[]{"Tricep Dip",                  "triceps",   "Fondos para tríceps"},
            new Object[]{"Tricep Pushdown",             "triceps",   "Extensión de tríceps en polea"},
            new Object[]{"Skull Crusher",               "triceps",   "Rompecráneos"},
            new Object[]{"Close-Grip Bench Press",      "triceps",   "Press agarre cerrado"},
            new Object[]{"Overhead Tricep Extension",   "triceps",   "Extensión de tríceps sobre la cabeza"},
            // Legs
            new Object[]{"Barbell Back Squat",          "legs",      "Sentadilla trasera con barra"},
            new Object[]{"Front Squat",                 "legs",      "Sentadilla frontal"},
            new Object[]{"Hack Squat",                  "legs",      "Sentadilla hack"},
            new Object[]{"Leg Press",                   "legs",      "Prensa de piernas"},
            new Object[]{"Bulgarian Split Squat",       "legs",      "Sentadilla búlgara"},
            new Object[]{"Walking Lunge",               "legs",      "Zancada caminando"},
            new Object[]{"Leg Extension",               "legs",      "Extensión de cuádriceps"},
            new Object[]{"Leg Curl",                    "legs",      "Curl femoral"},
            new Object[]{"Goblet Squat",                "legs",      "Sentadilla goblet"},
            // Glutes
            new Object[]{"Hip Thrust",                  "glutes",    "Empuje de cadera"},
            new Object[]{"Glute Bridge",                "glutes",    "Puente de glúteos"},
            new Object[]{"Cable Kickback",              "glutes",    "Patada trasera en polea"},
            // Calves
            new Object[]{"Calf Raise",                  "calves",    "Elevación de talones"},
            new Object[]{"Standing Calf Raise",         "calves",    "Elevación de talones de pie"},
            new Object[]{"Seated Calf Raise",           "calves",    "Elevación de talones sentado"},
            // Core
            new Object[]{"Plank",                       "core",      "Plancha"},
            new Object[]{"Ab Wheel Rollout",            "core",      "Rodillo abdominal"},
            new Object[]{"Hanging Leg Raise",           "core",      "Elevación de piernas colgado"},
            new Object[]{"Cable Crunch",                "core",      "Crunch en polea"},
            new Object[]{"Crunch",                      "core",      "Crunch abdominal"},
            // Forearms
            new Object[]{"Wrist Curl",                  "forearms",  "Curl de muñeca"},
            new Object[]{"Farmer's Walk",               "forearms",  "Caminata del granjero"},
            // Cardio
            new Object[]{"Treadmill Walk/Jog",          "cardio",    "Caminata/trote en cinta"},
            new Object[]{"Bike Sprint",                 "cardio",    "Sprint en bicicleta"},
            new Object[]{"Rowing Machine",              "cardio",    "Máquina de remo"},
            new Object[]{"Run or Bike",                 "cardio",    "Correr o bicicleta"},
            new Object[]{"Jump Rope",                   "cardio",    "Saltar la cuerda"}
        );

        exercises.forEach(e -> exerciseRepo.save(
                new ExerciseJpaEntity(
                        UUID.randomUUID(),
                        (String) e[0],
                        (String) e[1],
                        false,
                        null,
                        (String) e[2])));
    }
}
