-- ============================================================
-- V4 — Spanish translations for existing catalogue data
-- Runs after the seeder on an existing server; on a fresh DB
-- these UPDATE statements affect 0 rows (seeder provides data).
-- ============================================================

-- ── exercises ────────────────────────────────────────────────

UPDATE exercises SET name_es = 'Press de banca con barra'         WHERE name = 'Barbell Bench Press'          AND is_custom = false;
UPDATE exercises SET name_es = 'Press inclinado con barra'        WHERE name = 'Incline Barbell Press'        AND is_custom = false;
UPDATE exercises SET name_es = 'Press inclinado con mancuernas'   WHERE name = 'Incline Dumbbell Press'       AND is_custom = false;
UPDATE exercises SET name_es = 'Press de banca con mancuernas'    WHERE name = 'Dumbbell Bench Press'         AND is_custom = false;
UPDATE exercises SET name_es = 'Apertura en polea'                WHERE name = 'Cable Fly'                    AND is_custom = false;
UPDATE exercises SET name_es = 'Flexión de pecho'                 WHERE name = 'Push-up'                      AND is_custom = false;
UPDATE exercises SET name_es = 'Fondos en paralelas'              WHERE name = 'Dip'                          AND is_custom = false;
UPDATE exercises SET name_es = 'Peso muerto con barra'            WHERE name = 'Barbell Deadlift'             AND is_custom = false;
UPDATE exercises SET name_es = 'Remo con barra'                   WHERE name = 'Barbell Row'                  AND is_custom = false;
UPDATE exercises SET name_es = 'Remo Pendlay'                     WHERE name = 'Pendlay Row'                  AND is_custom = false;
UPDATE exercises SET name_es = 'Remo con mancuerna'               WHERE name = 'Dumbbell Row'                 AND is_custom = false;
UPDATE exercises SET name_es = 'Jalón al pecho'                   WHERE name = 'Lat Pulldown'                 AND is_custom = false;
UPDATE exercises SET name_es = 'Remo en polea sentado'            WHERE name = 'Seated Cable Row'             AND is_custom = false;
UPDATE exercises SET name_es = 'Dominada'                         WHERE name = 'Pull-up'                      AND is_custom = false;
UPDATE exercises SET name_es = 'Dominada con peso'                WHERE name = 'Weighted Pull-up'             AND is_custom = false;
UPDATE exercises SET name_es = 'Remo en T'                        WHERE name = 'T-Bar Row'                    AND is_custom = false;
UPDATE exercises SET name_es = 'Peso muerto rumano'               WHERE name = 'Romanian Deadlift'            AND is_custom = false;
UPDATE exercises SET name_es = 'Press militar'                    WHERE name = 'Overhead Press'               AND is_custom = false;
UPDATE exercises SET name_es = 'Press de hombro con mancuernas'   WHERE name = 'Dumbbell Shoulder Press'      AND is_custom = false;
UPDATE exercises SET name_es = 'Press Arnold'                     WHERE name = 'Arnold Press'                 AND is_custom = false;
UPDATE exercises SET name_es = 'Elevación lateral'                WHERE name = 'Lateral Raise'                AND is_custom = false;
UPDATE exercises SET name_es = 'Elevación frontal'                WHERE name = 'Front Raise'                  AND is_custom = false;
UPDATE exercises SET name_es = 'Jalón facial'                     WHERE name = 'Face Pull'                    AND is_custom = false;
UPDATE exercises SET name_es = 'Apertura deltoides posterior'     WHERE name = 'Rear Delt Fly'                AND is_custom = false;
UPDATE exercises SET name_es = 'Curl con barra'                   WHERE name = 'Barbell Curl'                 AND is_custom = false;
UPDATE exercises SET name_es = 'Curl con mancuerna'               WHERE name = 'Dumbbell Curl'                AND is_custom = false;
UPDATE exercises SET name_es = 'Curl con barra EZ'                WHERE name = 'EZ Bar Curl'                  AND is_custom = false;
UPDATE exercises SET name_es = 'Curl martillo'                    WHERE name = 'Hammer Curl'                  AND is_custom = false;
UPDATE exercises SET name_es = 'Curl en predicador'               WHERE name = 'Preacher Curl'                AND is_custom = false;
UPDATE exercises SET name_es = 'Curl inclinado con mancuerna'     WHERE name = 'Incline Dumbbell Curl'        AND is_custom = false;
UPDATE exercises SET name_es = 'Fondos para tríceps'              WHERE name = 'Tricep Dip'                   AND is_custom = false;
UPDATE exercises SET name_es = 'Extensión de tríceps en polea'    WHERE name = 'Tricep Pushdown'              AND is_custom = false;
UPDATE exercises SET name_es = 'Rompecráneos'                     WHERE name = 'Skull Crusher'                AND is_custom = false;
UPDATE exercises SET name_es = 'Press agarre cerrado'             WHERE name = 'Close-Grip Bench Press'       AND is_custom = false;
UPDATE exercises SET name_es = 'Extensión de tríceps sobre la cabeza' WHERE name = 'Overhead Tricep Extension' AND is_custom = false;
UPDATE exercises SET name_es = 'Sentadilla trasera con barra'     WHERE name = 'Barbell Back Squat'           AND is_custom = false;
UPDATE exercises SET name_es = 'Sentadilla frontal'               WHERE name = 'Front Squat'                  AND is_custom = false;
UPDATE exercises SET name_es = 'Sentadilla hack'                  WHERE name = 'Hack Squat'                   AND is_custom = false;
UPDATE exercises SET name_es = 'Prensa de piernas'                WHERE name = 'Leg Press'                    AND is_custom = false;
UPDATE exercises SET name_es = 'Sentadilla búlgara'               WHERE name = 'Bulgarian Split Squat'        AND is_custom = false;
UPDATE exercises SET name_es = 'Zancada caminando'                WHERE name = 'Walking Lunge'                AND is_custom = false;
UPDATE exercises SET name_es = 'Extensión de cuádriceps'          WHERE name = 'Leg Extension'                AND is_custom = false;
UPDATE exercises SET name_es = 'Curl femoral'                     WHERE name = 'Leg Curl'                     AND is_custom = false;
UPDATE exercises SET name_es = 'Sentadilla goblet'                WHERE name = 'Goblet Squat'                 AND is_custom = false;
UPDATE exercises SET name_es = 'Empuje de cadera'                 WHERE name = 'Hip Thrust'                   AND is_custom = false;
UPDATE exercises SET name_es = 'Puente de glúteos'                WHERE name = 'Glute Bridge'                 AND is_custom = false;
UPDATE exercises SET name_es = 'Patada trasera en polea'          WHERE name = 'Cable Kickback'               AND is_custom = false;
UPDATE exercises SET name_es = 'Elevación de talones'             WHERE name = 'Calf Raise'                   AND is_custom = false;
UPDATE exercises SET name_es = 'Elevación de talones de pie'      WHERE name = 'Standing Calf Raise'          AND is_custom = false;
UPDATE exercises SET name_es = 'Elevación de talones sentado'     WHERE name = 'Seated Calf Raise'            AND is_custom = false;
UPDATE exercises SET name_es = 'Plancha'                          WHERE name = 'Plank'                        AND is_custom = false;
UPDATE exercises SET name_es = 'Rodillo abdominal'                WHERE name = 'Ab Wheel Rollout'             AND is_custom = false;
UPDATE exercises SET name_es = 'Elevación de piernas colgado'     WHERE name = 'Hanging Leg Raise'            AND is_custom = false;
UPDATE exercises SET name_es = 'Crunch en polea'                  WHERE name = 'Cable Crunch'                 AND is_custom = false;
UPDATE exercises SET name_es = 'Crunch abdominal'                 WHERE name = 'Crunch'                       AND is_custom = false;
UPDATE exercises SET name_es = 'Curl de muñeca'                   WHERE name = 'Wrist Curl'                   AND is_custom = false;
UPDATE exercises SET name_es = 'Caminata del granjero'            WHERE name = 'Farmer''s Walk'               AND is_custom = false;
UPDATE exercises SET name_es = 'Caminata/trote en cinta'          WHERE name = 'Treadmill Walk/Jog'           AND is_custom = false;
UPDATE exercises SET name_es = 'Sprint en bicicleta'              WHERE name = 'Bike Sprint'                  AND is_custom = false;
UPDATE exercises SET name_es = 'Máquina de remo'                  WHERE name = 'Rowing Machine'               AND is_custom = false;
UPDATE exercises SET name_es = 'Correr o bicicleta'               WHERE name = 'Run or Bike'                  AND is_custom = false;
UPDATE exercises SET name_es = 'Saltar la cuerda'                 WHERE name = 'Jump Rope'                    AND is_custom = false;

-- ── routine_exercises — propagate from exercises table ────────

UPDATE routine_exercises re
SET    exercise_name_es = e.name_es
FROM   exercises e
WHERE  re.exercise_name = e.name
  AND  e.is_custom = false
  AND  e.name_es <> '';

-- ── routine_exercises — names not in exercises table ──────────

UPDATE routine_exercises SET exercise_name_es = 'Remo en polea'   WHERE exercise_name = 'Cable Row';
UPDATE routine_exercises SET exercise_name_es = 'Caminata en cinta' WHERE exercise_name = 'Treadmill Walk';

-- ── routines ──────────────────────────────────────────────────

UPDATE routines SET
    name_es        = 'Fuerza principiante 3 días',
    description_es = 'Programa de cuerpo completo con barra para quienes comienzan el entrenamiento de fuerza estructurado. Se centra en los levantamientos compuestos principales con progresión lineal.'
WHERE id = 'r-beg-strength';

UPDATE routines SET
    name_es        = 'Hipertrofia superior/inferior 4 días',
    description_es = 'División superior/inferior diseñada para maximizar el crecimiento muscular con mayor volumen y rangos de repeticiones variados. Requiere al menos 6 meses de entrenamiento consistente.'
WHERE id = 'r-int-hyp';

UPDATE routines SET
    name_es        = 'Powerbuilding avanzado 5 días',
    description_es = 'Programa de alta frecuencia que combina trabajo compuesto pesado con accesorios de culturismo. Para atletas con 2+ años de entrenamiento serio.'
WHERE id = 'r-adv-str';

UPDATE routines SET
    name_es        = 'Fitness general principiante 3 días',
    description_es = 'Programa equilibrado para quienes buscan mejorar la condición física general, desarrollar una base de fuerza y patrones de movimiento saludables.'
WHERE id = 'r-beg-gen';

UPDATE routines SET
    name_es        = 'Resistencia + Fuerza intermedio 4 días',
    description_es = 'Combina entrenamiento de fuerza con acondicionamiento cardiovascular. Ideal para atletas que buscan mantener la fuerza mientras mejoran su capacidad aeróbica.'
WHERE id = 'r-int-end';

-- ── routine_days ──────────────────────────────────────────────

-- r-beg-strength
UPDATE routine_days SET name_es = 'Día A — Cuerpo completo'
WHERE name = 'Day A — Full Body' AND routine_id = 'r-beg-strength';
UPDATE routine_days SET name_es = 'Día B — Cuerpo completo'
WHERE name = 'Day B — Full Body' AND routine_id = 'r-beg-strength';
UPDATE routine_days SET name_es = 'Día C — Cuerpo completo'
WHERE name = 'Day C — Full Body' AND routine_id = 'r-beg-strength';

-- r-int-hyp
UPDATE routine_days SET name_es = 'Superior A — Enfoque en fuerza'
WHERE name = 'Upper A — Strength Bias' AND routine_id = 'r-int-hyp';
UPDATE routine_days SET name_es = 'Inferior A — Enfoque en fuerza'
WHERE name = 'Lower A — Strength Bias' AND routine_id = 'r-int-hyp';
UPDATE routine_days SET name_es = 'Superior B — Enfoque en volumen'
WHERE name = 'Upper B — Volume Bias' AND routine_id = 'r-int-hyp';
UPDATE routine_days SET name_es = 'Inferior B — Enfoque en volumen'
WHERE name = 'Lower B — Volume Bias' AND routine_id = 'r-int-hyp';

-- r-adv-str
UPDATE routine_days SET name_es = 'Empuje — Dominante de pecho'
WHERE name = 'Push — Chest Dominant' AND routine_id = 'r-adv-str';
UPDATE routine_days SET name_es = 'Tirón — Dominante de espalda'
WHERE name = 'Pull — Back Dominant' AND routine_id = 'r-adv-str';
UPDATE routine_days SET name_es = 'Piernas — Dominante de cuádriceps'
WHERE name = 'Legs — Quad Dominant' AND routine_id = 'r-adv-str';
UPDATE routine_days SET name_es = 'Empuje — Dominante de hombros'
WHERE name = 'Push — Shoulder Dominant' AND routine_id = 'r-adv-str';
UPDATE routine_days SET name_es = 'Piernas — Cadena posterior'
WHERE name = 'Legs — Posterior Chain' AND routine_id = 'r-adv-str';

-- r-beg-gen
UPDATE routine_days SET name_es = 'Día 1 — Cuerpo completo + Cardio'
WHERE name = 'Day 1 — Full Body + Cardio' AND routine_id = 'r-beg-gen';
UPDATE routine_days SET name_es = 'Día 2 — Enfoque en tren superior'
WHERE name = 'Day 2 — Upper Body Focus' AND routine_id = 'r-beg-gen';
UPDATE routine_days SET name_es = 'Día 3 — Enfoque en tren inferior'
WHERE name = 'Day 3 — Lower Body Focus' AND routine_id = 'r-beg-gen';

-- r-int-end
UPDATE routine_days SET name_es = 'Fuerza — Tren inferior'
WHERE name = 'Strength — Lower Body' AND routine_id = 'r-int-end';
UPDATE routine_days SET name_es = 'Cardio — Entrenamiento por intervalos'
WHERE name = 'Cardio — Interval Training' AND routine_id = 'r-int-end';
UPDATE routine_days SET name_es = 'Fuerza — Tren superior'
WHERE name = 'Strength — Upper Body' AND routine_id = 'r-int-end';
UPDATE routine_days SET name_es = 'Cardio — Estado estable'
WHERE name = 'Cardio — Steady State' AND routine_id = 'r-int-end';
