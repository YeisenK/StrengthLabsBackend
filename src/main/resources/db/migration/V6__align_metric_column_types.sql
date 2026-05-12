-- ============================================================
-- V6 — Align training_metrics numeric columns with JPA `double`
-- ============================================================
-- The original schema used NUMERIC(P,S) for fatigue values which made
-- Hibernate's @Column validate-mode complain ("expected float(53) but
-- found numeric"). Convert to DOUBLE PRECISION so the entity mapping and
-- the schema agree end-to-end.

ALTER TABLE training_metrics
    ALTER COLUMN atl                     TYPE DOUBLE PRECISION USING atl::double precision,
    ALTER COLUMN ctl                     TYPE DOUBLE PRECISION USING ctl::double precision,
    ALTER COLUMN tsb                     TYPE DOUBLE PRECISION USING tsb::double precision,
    ALTER COLUMN acwr                    TYPE DOUBLE PRECISION USING acwr::double precision,
    ALTER COLUMN monotony                TYPE DOUBLE PRECISION USING monotony::double precision,
    ALTER COLUMN strain                  TYPE DOUBLE PRECISION USING strain::double precision,
    ALTER COLUMN ramp_rate               TYPE DOUBLE PRECISION USING ramp_rate::double precision,
    ALTER COLUMN readiness_score         TYPE DOUBLE PRECISION USING readiness_score::double precision,
    ALTER COLUMN injury_risk_score       TYPE DOUBLE PRECISION USING injury_risk_score::double precision,
    ALTER COLUMN overtraining_risk_score TYPE DOUBLE PRECISION USING overtraining_risk_score::double precision,
    ALTER COLUMN composite_risk_score    TYPE DOUBLE PRECISION USING composite_risk_score::double precision;

-- Same issue on workout_sets: weight_kg / rpe were NUMERIC, entity maps Double.
ALTER TABLE workout_sets
    ALTER COLUMN weight_kg TYPE DOUBLE PRECISION USING weight_kg::double precision,
    ALTER COLUMN rpe       TYPE DOUBLE PRECISION USING rpe::double precision;
