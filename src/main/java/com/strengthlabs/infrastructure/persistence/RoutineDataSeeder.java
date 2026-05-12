package com.strengthlabs.infrastructure.persistence;

import com.strengthlabs.infrastructure.persistence.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Order(2)
public class RoutineDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoutineDataSeeder.class);

    private final RoutineJpaRepository routineRepo;

    public RoutineDataSeeder(RoutineJpaRepository routineRepo) {
        this.routineRepo = routineRepo;
    }

    @Override
    public void run(String... args) {
        if (routineRepo.count() > 0) return;
        log.info("Seeding routine catalogue...");
        seedAll();
        log.info("Routine catalogue seeded: {} routines", routineRepo.count());
    }

    @Transactional
    public void seedAll() {
        saveRoutine(
                "r-beg-strength",
                "3-Day Beginner Strength",
                "Fuerza principiante 3 días",
                "beginner", "strength", 3,
                "A full-body barbell programme for those new to structured strength training. "
                        + "Focuses on the main compound lifts with linear progression.",
                "Programa de cuerpo completo con barra para quienes comienzan el entrenamiento de "
                        + "fuerza estructurado. Se centra en los levantamientos compuestos principales con progresión lineal.",
                List.of(
                        day("Day A — Full Body", "Día A — Cuerpo completo", List.of(
                                ex("Barbell Back Squat",  "Sentadilla trasera con barra", "legs",      3, "5",   null),
                                ex("Barbell Bench Press", "Press de banca con barra",     "chest",     3, "5",   null),
                                ex("Barbell Deadlift",    "Peso muerto con barra",         "back",      1, "5",   "Work up to one heavy set"),
                                ex("Overhead Press",      "Press militar",                 "shoulders", 3, "5",   null),
                                ex("Barbell Row",         "Remo con barra",                "back",      3, "5",   null)
                        )),
                        day("Day B — Full Body", "Día B — Cuerpo completo", List.of(
                                ex("Barbell Back Squat",  "Sentadilla trasera con barra", "legs",      3, "5",   null),
                                ex("Overhead Press",      "Press militar",                 "shoulders", 3, "5",   null),
                                ex("Barbell Deadlift",    "Peso muerto con barra",         "back",      1, "5",   null),
                                ex("Barbell Bench Press", "Press de banca con barra",     "chest",     3, "5",   null),
                                ex("Barbell Row",         "Remo con barra",                "back",      3, "5",   null)
                        )),
                        day("Day C — Full Body", "Día C — Cuerpo completo", List.of(
                                ex("Barbell Back Squat",  "Sentadilla trasera con barra", "legs",      3, "5",   null),
                                ex("Barbell Bench Press", "Press de banca con barra",     "chest",     3, "5",   null),
                                ex("Barbell Deadlift",    "Peso muerto con barra",         "back",      1, "5",   null),
                                ex("Overhead Press",      "Press militar",                 "shoulders", 3, "5",   null),
                                ex("Barbell Row",         "Remo con barra",                "back",      3, "5",   null)
                        ))
                ));

        saveRoutine(
                "r-int-hyp",
                "4-Day Upper/Lower Hypertrophy",
                "Hipertrofia superior/inferior 4 días",
                "intermediate", "hypertrophy", 4,
                "An upper/lower split designed to maximise muscle growth through higher volume "
                        + "and varied rep ranges. Requires at least 6 months of consistent training.",
                "División superior/inferior diseñada para maximizar el crecimiento muscular con mayor "
                        + "volumen y rangos de repeticiones variados. Requiere al menos 6 meses de entrenamiento consistente.",
                List.of(
                        day("Upper A — Strength Bias", "Superior A — Enfoque en fuerza", List.of(
                                ex("Barbell Bench Press",   "Press de banca con barra",     "chest",    4, "4-6",   "Keep rest 3-4 min"),
                                ex("Barbell Row",           "Remo con barra",                "back",     4, "4-6",   null),
                                ex("Incline Dumbbell Press","Press inclinado con mancuernas","chest",    3, "8-12",  null),
                                ex("Cable Row",             "Remo en polea",                 "back",     3, "8-12",  null),
                                ex("Lateral Raise",         "Elevación lateral",             "shoulders",3, "12-15", null),
                                ex("Tricep Pushdown",       "Extensión de tríceps en polea", "triceps",  3, "10-15", null),
                                ex("Dumbbell Curl",         "Curl con mancuerna",            "biceps",   3, "10-15", null)
                        )),
                        day("Lower A — Strength Bias", "Inferior A — Enfoque en fuerza", List.of(
                                ex("Barbell Back Squat",    "Sentadilla trasera con barra",  "legs",     4, "4-6",   null),
                                ex("Romanian Deadlift",     "Peso muerto rumano",             "legs",     3, "8-10",  null),
                                ex("Leg Press",             "Prensa de piernas",              "legs",     3, "10-12", null),
                                ex("Leg Curl",              "Curl femoral",                   "legs",     3, "10-12", null),
                                ex("Calf Raise",            "Elevación de talones",           "calves",   4, "12-15", null)
                        )),
                        day("Upper B — Volume Bias", "Superior B — Enfoque en volumen", List.of(
                                ex("Incline Barbell Press", "Press inclinado con barra",      "chest",    4, "8-12",  null),
                                ex("Lat Pulldown",          "Jalón al pecho",                 "back",     4, "8-12",  null),
                                ex("Dumbbell Shoulder Press","Press de hombro con mancuernas","shoulders",3, "10-12", null),
                                ex("Cable Fly",             "Apertura en polea",              "chest",    3, "12-15", null),
                                ex("Face Pull",             "Jalón facial",                   "shoulders",3, "15-20", "External rotation focus"),
                                ex("EZ Bar Curl",           "Curl con barra EZ",              "biceps",   3, "10-12", null),
                                ex("Skull Crusher",         "Rompecráneos",                   "triceps",  3, "10-12", null)
                        )),
                        day("Lower B — Volume Bias", "Inferior B — Enfoque en volumen", List.of(
                                ex("Barbell Deadlift",      "Peso muerto con barra",          "back",     4, "4-6",   null),
                                ex("Front Squat",           "Sentadilla frontal",             "legs",     3, "6-8",   null),
                                ex("Walking Lunge",         "Zancada caminando",              "legs",     3, "10-12", "Each leg"),
                                ex("Leg Extension",         "Extensión de cuádriceps",        "legs",     3, "12-15", null),
                                ex("Standing Calf Raise",   "Elevación de talones de pie",   "calves",   4, "12-15", null),
                                ex("Plank",                 "Plancha",                        "core",     3, "60s",   null)
                        ))
                ));

        saveRoutine(
                "r-adv-str",
                "5-Day Advanced Powerbuilding",
                "Powerbuilding avanzado 5 días",
                "advanced", "strength", 5,
                "A high-frequency powerbuilding programme that combines heavy compound work "
                        + "with bodybuilding accessories. For athletes with 2+ years of serious training.",
                "Programa de alta frecuencia que combina trabajo compuesto pesado con accesorios de "
                        + "culturismo. Para atletas con 2+ años de entrenamiento serio.",
                List.of(
                        day("Push — Chest Dominant", "Empuje — Dominante de pecho", List.of(
                                ex("Barbell Bench Press",       "Press de banca con barra",          "chest",    5, "3-5",   "Last set AMRAP"),
                                ex("Incline Dumbbell Press",    "Press inclinado con mancuernas",    "chest",    4, "8-10",  null),
                                ex("Cable Fly",                 "Apertura en polea",                 "chest",    3, "12-15", null),
                                ex("Overhead Press",            "Press militar",                     "shoulders",4, "6-8",   null),
                                ex("Lateral Raise",             "Elevación lateral",                 "shoulders",4, "15-20", null),
                                ex("Tricep Dip",                "Fondos para tríceps",               "triceps",  3, "8-12",  "Weighted if possible"),
                                ex("Tricep Pushdown",           "Extensión de tríceps en polea",     "triceps",  3, "12-15", null)
                        )),
                        day("Pull — Back Dominant", "Tirón — Dominante de espalda", List.of(
                                ex("Barbell Deadlift",          "Peso muerto con barra",             "back",     4, "3-5",   null),
                                ex("Weighted Pull-up",          "Dominada con peso",                 "back",     4, "5-8",   null),
                                ex("Pendlay Row",               "Remo Pendlay",                      "back",     4, "5-6",   null),
                                ex("Seated Cable Row",          "Remo en polea sentado",             "back",     3, "10-12", null),
                                ex("Face Pull",                 "Jalón facial",                      "shoulders",3, "15-20", null),
                                ex("Barbell Curl",              "Curl con barra",                    "biceps",   3, "8-10",  null),
                                ex("Hammer Curl",               "Curl martillo",                     "biceps",   3, "10-12", null)
                        )),
                        day("Legs — Quad Dominant", "Piernas — Dominante de cuádriceps", List.of(
                                ex("Barbell Back Squat",        "Sentadilla trasera con barra",      "legs",     5, "3-5",   "Competition stance"),
                                ex("Leg Press",                 "Prensa de piernas",                 "legs",     4, "8-12",  null),
                                ex("Hack Squat",                "Sentadilla hack",                   "legs",     3, "10-12", null),
                                ex("Leg Extension",             "Extensión de cuádriceps",           "legs",     3, "15-20", null),
                                ex("Leg Curl",                  "Curl femoral",                      "legs",     3, "10-12", null),
                                ex("Calf Raise",                "Elevación de talones",              "calves",   5, "12-15", null)
                        )),
                        day("Push — Shoulder Dominant", "Empuje — Dominante de hombros", List.of(
                                ex("Overhead Press",            "Press militar",                     "shoulders",5, "3-5",   null),
                                ex("Dumbbell Shoulder Press",   "Press de hombro con mancuernas",   "shoulders",4, "8-10",  null),
                                ex("Incline Barbell Press",     "Press inclinado con barra",         "chest",    4, "6-8",   null),
                                ex("Lateral Raise",             "Elevación lateral",                 "shoulders",4, "15-20", null),
                                ex("Tricep Dip",                "Fondos para tríceps",               "triceps",  3, "8-12",  null),
                                ex("Close-Grip Bench Press",    "Press agarre cerrado",              "triceps",  3, "8-10",  null)
                        )),
                        day("Legs — Posterior Chain", "Piernas — Cadena posterior", List.of(
                                ex("Romanian Deadlift",         "Peso muerto rumano",                "legs",     4, "6-8",   "Pause at bottom"),
                                ex("Bulgarian Split Squat",     "Sentadilla búlgara",                "legs",     4, "8-10",  "Each leg"),
                                ex("Hip Thrust",                "Empuje de cadera",                  "glutes",   4, "8-12",  null),
                                ex("Leg Curl",                  "Curl femoral",                      "legs",     4, "10-12", null),
                                ex("Standing Calf Raise",       "Elevación de talones de pie",      "calves",   4, "12-15", null),
                                ex("Ab Wheel Rollout",          "Rodillo abdominal",                 "core",     3, "10-12", null)
                        ))
                ));

        saveRoutine(
                "r-beg-gen",
                "3-Day Beginner General Fitness",
                "Fitness general principiante 3 días",
                "beginner", "general_fitness", 3,
                "A balanced programme for those looking to improve overall fitness, build a base of "
                        + "strength, and develop healthy movement patterns.",
                "Programa equilibrado para quienes buscan mejorar la condición física general, desarrollar "
                        + "una base de fuerza y patrones de movimiento saludables.",
                List.of(
                        day("Day 1 — Full Body + Cardio", "Día 1 — Cuerpo completo + Cardio", List.of(
                                ex("Goblet Squat",          "Sentadilla goblet",          "legs",    3, "10-12", null),
                                ex("Push-up",               "Flexión de pecho",            "chest",   3, "8-15",  "Scale as needed"),
                                ex("Dumbbell Row",          "Remo con mancuerna",          "back",    3, "10-12", "Each side"),
                                ex("Plank",                 "Plancha",                     "core",    3, "30-60s",null),
                                ex("Treadmill Walk/Jog",    "Caminata/trote en cinta",     "cardio",  1, "20min", "Zone 2 pace")
                        )),
                        day("Day 2 — Upper Body Focus", "Día 2 — Enfoque en tren superior", List.of(
                                ex("Dumbbell Bench Press",      "Press de banca con mancuernas",  "chest",    3, "10-12", null),
                                ex("Lat Pulldown",              "Jalón al pecho",                 "back",     3, "10-12", null),
                                ex("Dumbbell Shoulder Press",   "Press de hombro con mancuernas","shoulders",3, "10-12", null),
                                ex("Dumbbell Curl",             "Curl con mancuerna",             "biceps",   2, "12-15", null),
                                ex("Tricep Pushdown",           "Extensión de tríceps en polea",  "triceps",  2, "12-15", null)
                        )),
                        day("Day 3 — Lower Body Focus", "Día 3 — Enfoque en tren inferior", List.of(
                                ex("Barbell Back Squat",    "Sentadilla trasera con barra",  "legs",   3, "8-10",  null),
                                ex("Romanian Deadlift",     "Peso muerto rumano",             "legs",   3, "10-12", null),
                                ex("Leg Press",             "Prensa de piernas",              "legs",   3, "12-15", null),
                                ex("Calf Raise",            "Elevación de talones",           "calves", 3, "15-20", null),
                                ex("Plank",                 "Plancha",                        "core",   3, "45s",   null)
                        ))
                ));

        saveRoutine(
                "r-int-end",
                "4-Day Intermediate Endurance + Strength",
                "Resistencia + Fuerza intermedio 4 días",
                "intermediate", "endurance", 4,
                "Combines strength training with cardiovascular conditioning. "
                        + "Ideal for athletes who want to maintain strength while improving aerobic capacity.",
                "Combina entrenamiento de fuerza con acondicionamiento cardiovascular. Ideal para atletas "
                        + "que buscan mantener la fuerza mientras mejoran su capacidad aeróbica.",
                List.of(
                        day("Strength — Lower Body", "Fuerza — Tren inferior", List.of(
                                ex("Barbell Back Squat",    "Sentadilla trasera con barra", "legs",   4, "6-8",  null),
                                ex("Romanian Deadlift",     "Peso muerto rumano",            "legs",   3, "8-10", null),
                                ex("Leg Press",             "Prensa de piernas",             "legs",   3, "10-12",null),
                                ex("Calf Raise",            "Elevación de talones",          "calves", 3, "15",   null)
                        )),
                        day("Cardio — Interval Training", "Cardio — Entrenamiento por intervalos", List.of(
                                ex("Rowing Machine",        "Máquina de remo",               "cardio", 1, "5min",           "Warm-up easy"),
                                ex("Bike Sprint",           "Sprint en bicicleta",           "cardio", 8, "30s on / 90s off","Max effort sprints"),
                                ex("Treadmill Walk",        "Caminata en cinta",             "cardio", 1, "10min",          "Cool-down")
                        )),
                        day("Strength — Upper Body", "Fuerza — Tren superior", List.of(
                                ex("Barbell Bench Press",   "Press de banca con barra",      "chest",     4, "6-8",  null),
                                ex("Barbell Row",           "Remo con barra",                "back",      4, "6-8",  null),
                                ex("Overhead Press",        "Press militar",                 "shoulders", 3, "8-10", null),
                                ex("Pull-up",               "Dominada",                      "back",      3, "6-10", null),
                                ex("Dumbbell Curl",         "Curl con mancuerna",            "biceps",    2, "12",   null),
                                ex("Tricep Dip",            "Fondos para tríceps",           "triceps",   2, "12",   null)
                        )),
                        day("Cardio — Steady State", "Cardio — Estado estable", List.of(
                                ex("Run or Bike",           "Correr o bicicleta",            "cardio", 1, "45-60min","Zone 2, conversational pace")
                        ))
                ));
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private void saveRoutine(String id, String name, String nameEs,
                              String level, String goal, int daysPerWeek,
                              String description, String descriptionEs,
                              List<DayDef> dayDefs) {
        RoutineJpaEntity routine = new RoutineJpaEntity(
                id, name, nameEs, level, goal, daysPerWeek, description, descriptionEs);
        for (int i = 0; i < dayDefs.size(); i++) {
            DayDef dd = dayDefs.get(i);
            RoutineDayJpaEntity dayEntity =
                    new RoutineDayJpaEntity(UUID.randomUUID(), routine, dd.name, dd.nameEs, i);
            for (int j = 0; j < dd.exercises.size(); j++) {
                ExDef e = dd.exercises.get(j);
                dayEntity.addExercise(new RoutineExerciseJpaEntity(
                        UUID.randomUUID(), dayEntity,
                        e.name, e.nameEs, e.mg, e.sets, e.reps, e.notes, j));
            }
            routine.addDay(dayEntity);
        }
        routineRepo.save(routine);
    }

    private static DayDef day(String name, String nameEs, List<ExDef> exercises) {
        return new DayDef(name, nameEs, exercises);
    }

    private static ExDef ex(String name, String nameEs, String mg, int sets, String reps, String notes) {
        return new ExDef(name, nameEs, mg, sets, reps, notes);
    }

    private record DayDef(String name, String nameEs, List<ExDef> exercises) {}
    private record ExDef(String name, String nameEs, String mg, int sets, String reps, String notes) {}
}
