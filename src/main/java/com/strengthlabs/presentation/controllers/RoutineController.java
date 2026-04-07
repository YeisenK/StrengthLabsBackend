package com.strengthlabs.presentation.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/routines")
public class RoutineController {

    private static final List<Map<String, Object>> ROUTINES = buildRoutines();

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRoutines(
            @RequestParam(required = false) String level) {
        List<Map<String, Object>> filtered = level == null
                ? ROUTINES
                : ROUTINES.stream()
                        .filter(r -> level.equals(r.get("level")))
                        .toList();
        return ResponseEntity.ok(Map.of("items", filtered));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRoutine(@PathVariable String id) {
        return ROUTINES.stream()
                .filter(r -> id.equals(r.get("id")))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routine not found"));
    }

    // ── Static routine data ────────────────────────────────────────────────────

    private static List<Map<String, Object>> buildRoutines() {
        return List.of(
                routine("r-beg-strength", "3-Day Beginner Strength", "beginner", "strength", 3,
                        "A full-body barbell programme for those new to structured strength training. "
                                + "Focuses on the main compound lifts with linear progression.",
                        List.of(
                                day("Day A — Full Body", List.of(
                                        re("Barbell Back Squat", "legs", 3, "5", null),
                                        re("Barbell Bench Press", "chest", 3, "5", null),
                                        re("Barbell Deadlift", "back", 1, "5", "Work up to one heavy set"),
                                        re("Overhead Press", "shoulders", 3, "5", null),
                                        re("Barbell Row", "back", 3, "5", null)
                                )),
                                day("Day B — Full Body", List.of(
                                        re("Barbell Back Squat", "legs", 3, "5", null),
                                        re("Overhead Press", "shoulders", 3, "5", null),
                                        re("Barbell Deadlift", "1", "5", "back", null),
                                        re("Barbell Bench Press", "chest", 3, "5", null),
                                        re("Barbell Row", "back", 3, "5", null)
                                )),
                                day("Day C — Full Body", List.of(
                                        re("Barbell Back Squat", "legs", 3, "5", null),
                                        re("Barbell Bench Press", "chest", 3, "5", null),
                                        re("Barbell Deadlift", "back", 1, "5", null),
                                        re("Overhead Press", "shoulders", 3, "5", null),
                                        re("Barbell Row", "back", 3, "5", null)
                                ))
                        )),

                routine("r-int-hyp", "4-Day Upper/Lower Hypertrophy", "intermediate", "hypertrophy", 4,
                        "An upper/lower split designed to maximise muscle growth through higher volume "
                                + "and varied rep ranges. Requires at least 6 months of consistent training.",
                        List.of(
                                day("Upper A — Strength Bias", List.of(
                                        re("Barbell Bench Press", "chest", 4, "4-6", "Keep rest 3-4 min"),
                                        re("Barbell Row", "back", 4, "4-6", null),
                                        re("Incline Dumbbell Press", "chest", 3, "8-12", null),
                                        re("Cable Row", "back", 3, "8-12", null),
                                        re("Lateral Raise", "shoulders", 3, "12-15", null),
                                        re("Tricep Pushdown", "triceps", 3, "10-15", null),
                                        re("Dumbbell Curl", "biceps", 3, "10-15", null)
                                )),
                                day("Lower A — Strength Bias", List.of(
                                        re("Barbell Back Squat", "legs", 4, "4-6", null),
                                        re("Romanian Deadlift", "legs", 3, "8-10", null),
                                        re("Leg Press", "legs", 3, "10-12", null),
                                        re("Leg Curl", "legs", 3, "10-12", null),
                                        re("Calf Raise", "calves", 4, "12-15", null)
                                )),
                                day("Upper B — Volume Bias", List.of(
                                        re("Incline Barbell Press", "chest", 4, "8-12", null),
                                        re("Lat Pulldown", "back", 4, "8-12", null),
                                        re("Dumbbell Shoulder Press", "shoulders", 3, "10-12", null),
                                        re("Cable Fly", "chest", 3, "12-15", null),
                                        re("Face Pull", "shoulders", 3, "15-20", "External rotation focus"),
                                        re("EZ Bar Curl", "biceps", 3, "10-12", null),
                                        re("Skull Crusher", "triceps", 3, "10-12", null)
                                )),
                                day("Lower B — Volume Bias", List.of(
                                        re("Barbell Deadlift", "back", 4, "4-6", null),
                                        re("Front Squat", "legs", 3, "6-8", null),
                                        re("Walking Lunge", "legs", 3, "10-12", "Each leg"),
                                        re("Leg Extension", "legs", 3, "12-15", null),
                                        re("Standing Calf Raise", "calves", 4, "12-15", null),
                                        re("Plank", "core", 3, "60s", null)
                                ))
                        )),

                routine("r-adv-str", "5-Day Advanced Powerbuilding", "advanced", "strength", 5,
                        "A high-frequency powerbuilding programme that combines heavy compound work "
                                + "with bodybuilding accessories. For athletes with 2+ years of serious training.",
                        List.of(
                                day("Push — Chest Dominant", List.of(
                                        re("Barbell Bench Press", "chest", 5, "3-5", "Last set AMRAP"),
                                        re("Incline Dumbbell Press", "chest", 4, "8-10", null),
                                        re("Cable Fly", "chest", 3, "12-15", null),
                                        re("Overhead Press", "shoulders", 4, "6-8", null),
                                        re("Lateral Raise", "shoulders", 4, "15-20", null),
                                        re("Tricep Dip", "triceps", 3, "8-12", "Weighted if possible"),
                                        re("Tricep Pushdown", "triceps", 3, "12-15", null)
                                )),
                                day("Pull — Back Dominant", List.of(
                                        re("Barbell Deadlift", "back", 4, "3-5", null),
                                        re("Weighted Pull-up", "back", 4, "5-8", null),
                                        re("Pendlay Row", "back", 4, "5-6", null),
                                        re("Seated Cable Row", "back", 3, "10-12", null),
                                        re("Face Pull", "shoulders", 3, "15-20", null),
                                        re("Barbell Curl", "biceps", 3, "8-10", null),
                                        re("Hammer Curl", "biceps", 3, "10-12", null)
                                )),
                                day("Legs — Quad Dominant", List.of(
                                        re("Barbell Back Squat", "legs", 5, "3-5", "Competition stance"),
                                        re("Leg Press", "legs", 4, "8-12", null),
                                        re("Hack Squat", "legs", 3, "10-12", null),
                                        re("Leg Extension", "legs", 3, "15-20", null),
                                        re("Leg Curl", "legs", 3, "10-12", null),
                                        re("Calf Raise", "calves", 5, "12-15", null)
                                )),
                                day("Push — Shoulder Dominant", List.of(
                                        re("Overhead Press", "shoulders", 5, "3-5", null),
                                        re("Dumbbell Shoulder Press", "shoulders", 4, "8-10", null),
                                        re("Incline Barbell Press", "chest", 4, "6-8", null),
                                        re("Lateral Raise", "shoulders", 4, "15-20", null),
                                        re("Tricep Dip", "triceps", 3, "8-12", null),
                                        re("Close-Grip Bench Press", "triceps", 3, "8-10", null)
                                )),
                                day("Legs — Posterior Chain", List.of(
                                        re("Romanian Deadlift", "legs", 4, "6-8", "Pause at bottom"),
                                        re("Bulgarian Split Squat", "legs", 4, "8-10", "Each leg"),
                                        re("Hip Thrust", "glutes", 4, "8-12", null),
                                        re("Leg Curl", "legs", 4, "10-12", null),
                                        re("Standing Calf Raise", "calves", 4, "12-15", null),
                                        re("Ab Wheel Rollout", "core", 3, "10-12", null)
                                ))
                        )),

                routine("r-beg-gen", "3-Day Beginner General Fitness", "beginner", "general_fitness", 3,
                        "A balanced programme for those looking to improve overall fitness, build a base of "
                                + "strength, and develop healthy movement patterns.",
                        List.of(
                                day("Day 1 — Full Body + Cardio", List.of(
                                        re("Goblet Squat", "legs", 3, "10-12", null),
                                        re("Push-up", "chest", 3, "8-15", "Scale as needed"),
                                        re("Dumbbell Row", "back", 3, "10-12", "Each side"),
                                        re("Plank", "core", 3, "30-60s", null),
                                        re("Treadmill Walk/Jog", "cardio", 1, "20min", "Zone 2 pace")
                                )),
                                day("Day 2 — Upper Body Focus", List.of(
                                        re("Dumbbell Bench Press", "chest", 3, "10-12", null),
                                        re("Lat Pulldown", "back", 3, "10-12", null),
                                        re("Dumbbell Shoulder Press", "shoulders", 3, "10-12", null),
                                        re("Dumbbell Curl", "biceps", 2, "12-15", null),
                                        re("Tricep Pushdown", "triceps", 2, "12-15", null)
                                )),
                                day("Day 3 — Lower Body Focus", List.of(
                                        re("Barbell Back Squat", "legs", 3, "8-10", null),
                                        re("Romanian Deadlift", "legs", 3, "10-12", null),
                                        re("Leg Press", "legs", 3, "12-15", null),
                                        re("Calf Raise", "calves", 3, "15-20", null),
                                        re("Plank", "core", 3, "45s", null)
                                ))
                        )),

                routine("r-int-end", "4-Day Intermediate Endurance + Strength", "intermediate", "endurance", 4,
                        "Combines strength training with cardiovascular conditioning. "
                                + "Ideal for athletes who want to maintain strength while improving aerobic capacity.",
                        List.of(
                                day("Strength — Lower Body", List.of(
                                        re("Barbell Back Squat", "legs", 4, "6-8", null),
                                        re("Romanian Deadlift", "legs", 3, "8-10", null),
                                        re("Leg Press", "legs", 3, "10-12", null),
                                        re("Calf Raise", "calves", 3, "15", null)
                                )),
                                day("Cardio — Interval Training", List.of(
                                        re("Rowing Machine", "cardio", 1, "5min", "Warm-up easy"),
                                        re("Bike Sprint", "cardio", 8, "30s on / 90s off", "Max effort sprints"),
                                        re("Treadmill Walk", "cardio", 1, "10min", "Cool-down")
                                )),
                                day("Strength — Upper Body", List.of(
                                        re("Barbell Bench Press", "chest", 4, "6-8", null),
                                        re("Barbell Row", "back", 4, "6-8", null),
                                        re("Overhead Press", "shoulders", 3, "8-10", null),
                                        re("Pull-up", "back", 3, "6-10", null),
                                        re("Dumbbell Curl", "biceps", 2, "12", null),
                                        re("Tricep Dip", "triceps", 2, "12", null)
                                )),
                                day("Cardio — Steady State", List.of(
                                        re("Run or Bike", "cardio", 1, "45-60min", "Zone 2, conversational pace")
                                ))
                        ))
        );
    }

    private static Map<String, Object> routine(String id, String name, String level, String goal,
                                                int daysPerWeek, String description,
                                                List<Map<String, Object>> days) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("name", name);
        r.put("level", level);
        r.put("goal", goal);
        r.put("days_per_week", daysPerWeek);
        r.put("description", description);
        r.put("days", days);
        return r;
    }

    private static Map<String, Object> day(String name, List<Map<String, Object>> exercises) {
        return Map.of("name", name, "exercises", exercises);
    }

    private static Map<String, Object> re(String exerciseName, String muscleGroup,
                                           int sets, String repsScheme, String notes) {
        Map<String, Object> exercise = Map.of(
                "id", "ex-" + exerciseName.toLowerCase().replace(" ", "-"),
                "name", exerciseName,
                "muscle_group", muscleGroup,
                "is_custom", false
        );
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("exercise", exercise);
        m.put("sets", sets);
        m.put("reps_scheme", repsScheme);
        m.put("notes", notes);
        return m;
    }

    /** Overload for the case where the sets param was mistakenly put as a String. */
    private static Map<String, Object> re(String exerciseName, String setsStr, String repsScheme,
                                           String muscleGroup, String notes) {
        int sets;
        try { sets = Integer.parseInt(setsStr); } catch (NumberFormatException e) { sets = 3; }
        return re(exerciseName, muscleGroup, sets, repsScheme, notes);
    }
}
