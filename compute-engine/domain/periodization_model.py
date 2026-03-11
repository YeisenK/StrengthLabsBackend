from __future__ import annotations

import math
from dataclasses import dataclass, field
from datetime import date, timedelta
from enum import Enum
from typing import Literal, Optional


class TrainingPhase(str, Enum):
    ACCUMULATION   = "accumulation"
    TRANSMUTATION  = "transmutation"
    REALIZATION    = "realization"
    RECOVERY       = "recovery"
    TRANSITION     = "transition"


class MicrocycleType(str, Enum):
    SHOCK         = "shock"
    LOADING       = "loading"
    MAINTENANCE   = "maintenance"
    RECOVERY      = "recovery"
    TAPER         = "taper"
    COMPETITION   = "competition"


class IntensityZone(str, Enum):
    # Modelo de 5 zonas, mapeadas a RPE CR-10
    Z1 = "z1_recovery"
    Z2 = "z2_aerobic"
    Z3 = "z3_tempo"
    Z4 = "z4_threshold"
    Z5 = "z5_neuromuscular"


@dataclass
class SessionPrescription:
    day_offset: int
    session_type: str
    intensity_zone: IntensityZone
    duration_minutes: int
    rpe_target: float
    description: str
    is_rest: bool = False
    key_session: bool = False


@dataclass
class MicrocyclePlan:
    microcycle_type: MicrocycleType
    phase: TrainingPhase
    sessions: list[SessionPrescription] = field(default_factory=list)
    target_weekly_load: float = 0.0
    target_acwr: float = 0.0
    projected_tsb_delta: float = 0.0
    week_objective: str = ""
    coach_notes: list[str] = field(default_factory=list)


# Ventanas de bloque en días
REALIZATION_WINDOW_DAYS   = 14
TRANSMUTATION_WINDOW_DAYS = 42
ACCUMULATION_WINDOW_DAYS  = 84


def detect_phase(
    days_to_event: Optional[int],
    ctl: float,
    tsb: float,
    composite_risk: float,
) -> TrainingPhase:
    if composite_risk >= 0.70 or tsb < -35:
        return TrainingPhase.RECOVERY

    if days_to_event is None:
        return TrainingPhase.ACCUMULATION

    if days_to_event <= 0:
        return TrainingPhase.TRANSITION
    elif days_to_event <= REALIZATION_WINDOW_DAYS:
        return TrainingPhase.REALIZATION
    elif days_to_event <= TRANSMUTATION_WINDOW_DAYS:
        return TrainingPhase.TRANSMUTATION
    else:
        return TrainingPhase.ACCUMULATION


def select_microcycle_type(
    phase: TrainingPhase,
    tsb: float,
    acwr: float,
    readiness_score: float,
    weeks_in_phase: int,
    composite_risk: float,
) -> MicrocycleType:
    if composite_risk >= 0.70 or tsb < -35:
        return MicrocycleType.RECOVERY

    if phase in (TrainingPhase.RECOVERY, TrainingPhase.TRANSITION):
        return MicrocycleType.RECOVERY

    if phase == TrainingPhase.REALIZATION:
        return MicrocycleType.TAPER

    if composite_risk >= 0.45 or tsb < -20:
        return MicrocycleType.RECOVERY

    if acwr > 1.3 or readiness_score < 50:
        return MicrocycleType.MAINTENANCE

    # Semana shock cada ~4 semanas si el atleta está fresco y riesgo baj
    is_shock_week = (weeks_in_phase % 4 == 3) and tsb > 0 and readiness_score >= 70
    if is_shock_week and composite_risk < 0.20:
        return MicrocycleType.SHOCK

    return MicrocycleType.LOADING


# Multiplicadores de carga por tipo de microciclo
LOAD_MULTIPLIERS: dict[MicrocycleType, float] = {
    MicrocycleType.SHOCK:       1.30,
    MicrocycleType.LOADING:     1.05,
    MicrocycleType.MAINTENANCE: 0.85,
    MicrocycleType.RECOVERY:    0.50,
    MicrocycleType.TAPER:       0.60,
    MicrocycleType.COMPETITION: 0.45,
}

# Distribución de zonas por fase — modelo polarizado
ZONE_DISTRIBUTION: dict[TrainingPhase, dict[IntensityZone, float]] = {
    TrainingPhase.ACCUMULATION: {
        IntensityZone.Z1: 0.20, IntensityZone.Z2: 0.60,
        IntensityZone.Z3: 0.10, IntensityZone.Z4: 0.10, IntensityZone.Z5: 0.00,
    },
    TrainingPhase.TRANSMUTATION: {
        IntensityZone.Z1: 0.15, IntensityZone.Z2: 0.40,
        IntensityZone.Z3: 0.15, IntensityZone.Z4: 0.25, IntensityZone.Z5: 0.05,
    },
    TrainingPhase.REALIZATION: {
        IntensityZone.Z1: 0.30, IntensityZone.Z2: 0.35,
        IntensityZone.Z3: 0.05, IntensityZone.Z4: 0.20, IntensityZone.Z5: 0.10,
    },
    TrainingPhase.RECOVERY: {
        IntensityZone.Z1: 0.60, IntensityZone.Z2: 0.40,
        IntensityZone.Z3: 0.00, IntensityZone.Z4: 0.00, IntensityZone.Z5: 0.00,
    },
    TrainingPhase.TRANSITION: {
        IntensityZone.Z1: 0.70, IntensityZone.Z2: 0.30,
        IntensityZone.Z3: 0.00, IntensityZone.Z4: 0.00, IntensityZone.Z5: 0.00,
    },
}

ZONE_RPE: dict[IntensityZone, tuple[float, float]] = {
    IntensityZone.Z1: (1.0, 2.0),
    IntensityZone.Z2: (3.0, 4.0),
    IntensityZone.Z3: (5.0, 6.0),
    IntensityZone.Z4: (7.0, 8.0),
    IntensityZone.Z5: (9.0, 10.0),
}

ZONE_LABELS: dict[IntensityZone, str] = {
    IntensityZone.Z1: "Active Recovery",
    IntensityZone.Z2: "Aerobic Base",
    IntensityZone.Z3: "Tempo",
    IntensityZone.Z4: "Threshold / VO₂max Intervals",
    IntensityZone.Z5: "Maximal / Neuromuscular",
}


def _target_weekly_load(ctl: float, microcycle_type: MicrocycleType) -> float:
    # Carga semanal = CTL × 7 × multiplicador (CTL es promedio diario)
    return ctl * 7.0 * LOAD_MULTIPLIERS[microcycle_type]


def _sessions_per_week(microcycle_type: MicrocycleType, ctl: float) -> int:
    base = {
        MicrocycleType.SHOCK:       6,
        MicrocycleType.LOADING:     5,
        MicrocycleType.MAINTENANCE: 4,
        MicrocycleType.RECOVERY:    3,
        MicrocycleType.TAPER:       4,
        MicrocycleType.COMPETITION: 3,
    }[microcycle_type]

    if ctl < 40:
        base = max(3, base - 1)
    elif ctl > 80:
        base = min(7, base + 1)

    return base


_REST_DAY = SessionPrescription(
    day_offset=0,
    session_type="Rest / Complete Recovery",
    intensity_zone=IntensityZone.Z1,
    duration_minutes=0,
    rpe_target=0.0,
    description="Full rest day. Prioritize sleep (8–9 h), hydration, and nutrition. "
                "Light walking (< 20 min) is acceptable.",
    is_rest=True,
)


def _build_session(
    day_offset: int,
    zone: IntensityZone,
    duration_minutes: int,
    phase: TrainingPhase,
    microcycle_type: MicrocycleType,
    is_key: bool = False,
) -> SessionPrescription:
    rpe_lo, rpe_hi = ZONE_RPE[zone]
    rpe_target = round((rpe_lo + rpe_hi) / 2, 1)
    label = ZONE_LABELS[zone]

    descriptions: dict[IntensityZone, str] = {
        IntensityZone.Z1: (
            f"Easy {duration_minutes}-min session. Conversational pace throughout. "
            "Focus on movement quality and flushing metabolic byproducts."
        ),
        IntensityZone.Z2: (
            f"{duration_minutes}-min aerobic base session. Steady effort at the "
            "lower end of ventilatory threshold. Nasal breathing preferred. "
            "Primary adaptation: mitochondrial density, fat oxidation."
        ),
        IntensityZone.Z3: (
            f"{duration_minutes}-min tempo session. Comfortably hard — "
            "'controlled discomfort'. Can speak short sentences. "
            "Primary adaptation: lactate clearance, threshold power."
        ),
        IntensityZone.Z4: (
            f"{duration_minutes}-min threshold/interval session. "
            "4–6 × (3–8 min) at target effort with equal recovery. "
            "Primary adaptation: VO₂max, stroke volume, buffering capacity."
        ),
        IntensityZone.Z5: (
            f"{duration_minutes}-min neuromuscular session. "
            "Short maximal efforts (10–30 s) with full recovery (3–5 min). "
            "Primary adaptation: peak power, recruitment, muscle fiber activation."
        ),
    }

    return SessionPrescription(
        day_offset=day_offset,
        session_type=label,
        intensity_zone=zone,
        duration_minutes=duration_minutes,
        rpe_target=rpe_target,
        description=descriptions[zone],
        is_rest=False,
        key_session=is_key,
    )


def _distribute_sessions(
    n_sessions: int,
    weekly_load: float,
    phase: TrainingPhase,
    microcycle_type: MicrocycleType,
) -> list[SessionPrescription]:
    dist = ZONE_DISTRIBUTION[phase]
    zone_pool: list[IntensityZone] = []
    for zone, fraction in dist.items():
        count = round(fraction * n_sessions)
        zone_pool.extend([zone] * count)

    while len(zone_pool) < n_sessions:
        zone_pool.append(IntensityZone.Z2)
    zone_pool = zone_pool[:n_sessions]

    zone_order = [IntensityZone.Z5, IntensityZone.Z4, IntensityZone.Z3,
                  IntensityZone.Z2, IntensityZone.Z1]
    zone_pool.sort(key=lambda z: zone_order.index(z) if z in zone_order else 99)

    hard_zones = {IntensityZone.Z4, IntensityZone.Z5}
    hard_days  = [1, 3, 5]
    easy_days  = [0, 2, 4, 6]

    hard_sessions = [z for z in zone_pool if z in hard_zones]
    easy_sessions = [z for z in zone_pool if z not in hard_zones]

    used_days: set[int] = set()
    assigned: list[tuple[int, IntensityZone]] = []

    for i, zone in enumerate(hard_sessions):
        day = hard_days[i % len(hard_days)]
        while day in used_days:
            day = (day + 1) % 7
        used_days.add(day)
        assigned.append((day, zone))

    for i, zone in enumerate(easy_sessions):
        day = easy_days[i % len(easy_days)]
        while day in used_days:
            day = (day + 1) % 7
        used_days.add(day)
        assigned.append((day, zone))

    assigned.sort(key=lambda x: x[0])

    # Peso por zona para distribuir carga: Z alta = sesiones más cortas e intensas
    zone_weight = {
        IntensityZone.Z1: 0.8, IntensityZone.Z2: 1.0, IntensityZone.Z3: 1.2,
        IntensityZone.Z4: 1.5, IntensityZone.Z5: 1.8,
    }
    total_weight = sum(zone_weight[z] for _, z in assigned)

    # duration_i = load_share_i / rpe_i  (de: load = duration × rpe)
    sessions: list[SessionPrescription] = []
    for i, (day, zone) in enumerate(assigned):
        rpe_lo, rpe_hi = ZONE_RPE[zone]
        rpe_mid = (rpe_lo + rpe_hi) / 2
        load_share = weekly_load * (zone_weight[zone] / total_weight)
        duration_min = int(round(load_share / max(rpe_mid, 1.0)))
        duration_min = max(20, min(180, duration_min))

        is_key = (zone in hard_zones and i == 0)
        sessions.append(_build_session(day, zone, duration_min, phase, microcycle_type, is_key))

    scheduled_days = {s.day_offset for s in sessions}
    for d in range(7):
        if d not in scheduled_days:
            rest = SessionPrescription(
                day_offset=d,
                session_type=_REST_DAY.session_type,
                intensity_zone=_REST_DAY.intensity_zone,
                duration_minutes=0,
                rpe_target=0.0,
                description=_REST_DAY.description,
                is_rest=True,
            )
            sessions.append(rest)

    sessions.sort(key=lambda s: s.day_offset)
    return sessions


def _generate_coach_notes(
    phase: TrainingPhase,
    microcycle_type: MicrocycleType,
    tsb: float,
    acwr: float,
    readiness_score: float,
    composite_risk: float,
    ctl: float,
    ramp_rate: float,
    monotony: float,
    days_to_event: Optional[int],
) -> list[str]:
    notes: list[str] = []

    phase_notes = {
        TrainingPhase.ACCUMULATION: (
            "BASE PHASE: Priority is aerobic capacity and work tolerance. "
            "Volume > intensity. Resist the urge to go hard — this phase builds "
            "the ceiling for everything that follows."
        ),
        TrainingPhase.TRANSMUTATION: (
            "BUILD PHASE: Converting the aerobic base into sport-specific fitness. "
            "Intensity rises, volume moderates. Quality over quantity on hard days."
        ),
        TrainingPhase.REALIZATION: (
            "TAPER PHASE: Reducing fatigue to express fitness built over weeks. "
            "Research (Mujika 2003) supports 40–60% volume reduction. "
            "Maintain or slightly increase intensity — do NOT go easy on everything."
        ),
        TrainingPhase.RECOVERY: (
            "RECOVERY PHASE: Metrics indicate meaningful fatigue or risk. "
            "This is not optional. Suppressing this phase accelerates the "
            "pathway to overtraining syndrome (Meeusen 2013)."
        ),
        TrainingPhase.TRANSITION: (
            "TRANSITION PHASE: Post-competition/season active rest. "
            "Maintain movement, eliminate structure. Physical and psychological "
            "regeneration are equally important."
        ),
    }
    notes.append(phase_notes[phase])

    if tsb < -30:
        notes.append(
            f"⚠ TSB is {tsb:.1f}. Accumulated fatigue is clinically significant. "
            "Sleep quality, HRV, and mood should be monitored daily."
        )
    elif tsb > 15:
        notes.append(
            f"TSB is {tsb:.1f} — athlete is well-rested. "
            "This is an optimal window for maximal quality sessions or competition."
        )

    if acwr > 1.5:
        notes.append(
            f"⚠ ACWR is {acwr:.2f}. Acute load exceeds chronic fitness. "
            "Reduce session density before adding volume or intensity."
        )

    if monotony > 2.0:
        notes.append(
            f"⚠ Monotony index is {monotony:.2f}. "
            "Vary session types, durations, and intensities this week to reduce "
            "illness and overtraining risk (Foster 1998)."
        )

    if ramp_rate > 8.0:
        notes.append(
            f"⚠ Ramp rate is +{ramp_rate:.1f} ATU/week. "
            "Connective tissue adaptation lags muscular adaptation by 4–8 weeks. "
            "Do not exceed +8 ATU/week for more than 2 consecutive weeks."
        )

    if ctl < 30:
        notes.append(
            "CTL is low — athlete is in early base building or returning from "
            "a break. Prioritize consistency over intensity."
        )
    elif ctl > 80:
        notes.append(
            f"CTL is {ctl:.1f} — high fitness base. "
            "The athlete can tolerate harder sessions but recovery management "
            "becomes more critical, not less."
        )

    if days_to_event is not None and 7 <= days_to_event <= 21:
        notes.append(
            f"Competition in {days_to_event} days. "
            "No new fitness can be built in this window — only fatigue. "
            "Every session should serve recovery or race-specific sharpening."
        )

    if readiness_score < 40:
        notes.append(
            f"Readiness score is {readiness_score:.0f}/100. "
            "Athlete is not primed for hard work. Proceed with the prescribed "
            "plan conservatively; abort hard sessions if perceived effort is "
            "unexpectedly high at warm-up."
        )

    return notes


def _project_tsb_delta(
    ctl: float,
    weekly_load: float,
    lambda_atl: float = 1.0 - math.exp(-1.0 / 7.0),
    lambda_ctl: float = 1.0 - math.exp(-1.0 / 42.0),
) -> float:
    # Propaga EWMA 7 días asumiendo carga diaria uniforme; ΔTSB = ΔCTL - ΔATL
    daily_load = weekly_load / 7.0
    atl = ctl
    ctl_val = ctl

    for _ in range(7):
        atl     = atl     + lambda_atl * (daily_load - atl)
        ctl_val = ctl_val + lambda_ctl * (daily_load - ctl_val)

    return round((ctl_val - atl) - 0.0, 1)


def generate_plan(
    ctl: float,
    atl: float,
    tsb: float,
    acwr: float,
    monotony: float,
    ramp_rate: float,
    readiness_score: float,
    composite_risk: float,
    injury_risk_score: float,
    overtraining_risk_score: float,
    days_to_event: Optional[int] = None,
    weeks_in_phase: int = 0,
    phase_override: Optional[TrainingPhase] = None,
) -> dict:
    phase = phase_override or detect_phase(days_to_event, ctl, tsb, composite_risk)

    microcycle_type = select_microcycle_type(
        phase, tsb, acwr, readiness_score, weeks_in_phase, composite_risk
    )

    target_load = _target_weekly_load(ctl, microcycle_type)
    n_sessions  = _sessions_per_week(microcycle_type, ctl)

    # Limitar incremento semanal al ramp rate máximo seguro (Hulin 2016)
    current_weekly_load = ctl * 7.0
    target_load = min(target_load, current_weekly_load + 8.0 * 7.0)

    sessions = _distribute_sessions(n_sessions, target_load, phase, microcycle_type)

    projected_tsb_delta = _project_tsb_delta(ctl, target_load)
    target_acwr  = (target_load / 7.0) / max(ctl, 1.0)
    load_pct_change = ((target_load - current_weekly_load) / max(current_weekly_load, 1.0)) * 100

    objectives = {
        (TrainingPhase.ACCUMULATION,  MicrocycleType.SHOCK):       "Maximum overload stimulus to drive supercompensation.",
        (TrainingPhase.ACCUMULATION,  MicrocycleType.LOADING):     "Progressive aerobic base development.",
        (TrainingPhase.ACCUMULATION,  MicrocycleType.MAINTENANCE): "Maintain aerobic base while managing fatigue.",
        (TrainingPhase.ACCUMULATION,  MicrocycleType.RECOVERY):    "Active recovery — restore homeostasis and readiness.",
        (TrainingPhase.TRANSMUTATION, MicrocycleType.SHOCK):       "Peak intensity overload — convert base to sport fitness.",
        (TrainingPhase.TRANSMUTATION, MicrocycleType.LOADING):     "Increase sport-specific intensity and threshold capacity.",
        (TrainingPhase.TRANSMUTATION, MicrocycleType.MAINTENANCE): "Consolidate fitness gains, reduce accumulated fatigue.",
        (TrainingPhase.TRANSMUTATION, MicrocycleType.RECOVERY):    "Deload — prepare for next intensification block.",
        (TrainingPhase.REALIZATION,   MicrocycleType.TAPER):       "Shed fatigue while preserving fitness — peak for event.",
        (TrainingPhase.RECOVERY,      MicrocycleType.RECOVERY):    "Full recovery — risk and fatigue markers require rest.",
        (TrainingPhase.TRANSITION,    MicrocycleType.RECOVERY):    "Active rest — physical and mental regeneration.",
    }
    week_objective = objectives.get(
        (phase, microcycle_type),
        f"{phase.value.capitalize()} — {microcycle_type.value} week."
    )

    rationale = _build_rationale(
        phase, microcycle_type, tsb, acwr, ctl, readiness_score,
        composite_risk, days_to_event, ramp_rate, monotony
    )

    coach_notes = _generate_coach_notes(
        phase, microcycle_type, tsb, acwr, readiness_score,
        composite_risk, ctl, ramp_rate, monotony, days_to_event
    )

    return {
        "phase":                    phase.value,
        "microcycle_type":          microcycle_type.value,
        "week_objective":           week_objective,
        "target_weekly_load":       round(target_load, 1),
        "target_acwr":              round(target_acwr, 2),
        "projected_tsb_delta":      projected_tsb_delta,
        "load_adjustment_pct":      round(load_pct_change, 1),
        "sessions": [
            {
                "day":              s.day_offset,
                "day_name":         ["Mon","Tue","Wed","Thu","Fri","Sat","Sun"][s.day_offset],
                "session_type":     s.session_type,
                "intensity_zone":   s.intensity_zone.value,
                "duration_minutes": s.duration_minutes,
                "rpe_target":       s.rpe_target,
                "description":      s.description,
                "is_rest":          s.is_rest,
                "key_session":      s.key_session,
            }
            for s in sessions
        ],
        "coach_notes":              coach_notes,
        "periodization_rationale":  rationale,
    }


def _build_rationale(
    phase: TrainingPhase,
    microcycle_type: MicrocycleType,
    tsb: float,
    acwr: float,
    ctl: float,
    readiness_score: float,
    composite_risk: float,
    days_to_event: Optional[int],
    ramp_rate: float,
    monotony: float,
) -> str:
    lines = []

    lines.append(f"PHASE: {phase.value.upper()}")
    phase_rationale = {
        TrainingPhase.ACCUMULATION:  "Selected because the athlete is in base-building. "
                                     "Volume emphasis following Matveyev LP principles and "
                                     "Issurin block periodization accumulation block.",
        TrainingPhase.TRANSMUTATION: "Fitness base (CTL) is sufficient to handle increased "
                                     "intensity. Entering Issurin's transmutation block — "
                                     "sport-specific work converts base fitness to performance.",
        TrainingPhase.REALIZATION:   f"Competition is {days_to_event} days away. Entering "
                                     "Mujika taper protocol: 40–60% volume reduction, "
                                     "maintained intensity, increased recovery.",
        TrainingPhase.RECOVERY:      f"Override triggered. TSB={tsb:.1f}, "
                                     f"composite risk={composite_risk:.2f}. "
                                     "Meeusen 2013 consensus: continued overload at this "
                                     "state accelerates overtraining syndrome progression.",
        TrainingPhase.TRANSITION:    "Post-event or end-of-season. Active rest per Issurin "
                                     "transition block protocol.",
    }
    lines.append(phase_rationale.get(phase, ""))

    lines.append(f"\nMICROCYCLE: {microcycle_type.value.upper()}")
    mc_rationale = {
        MicrocycleType.SHOCK:       f"Athlete is fresh (TSB={tsb:.1f}, readiness={readiness_score:.0f}) "
                                    "and risk is low. Supercompensation theory (Yakovlev/Zatsiorsky) "
                                    "supports a shock stimulus at this point in the training cycle.",
        MicrocycleType.LOADING:     "Standard progressive overload week. "
                                    "5% above CTL maintains positive adaptation signal "
                                    "without excessive fatigue accumulation.",
        MicrocycleType.MAINTENANCE: f"ACWR={acwr:.2f} or readiness={readiness_score:.0f} suggests "
                                    "caution. Maintaining load prevents detraining while allowing "
                                    "partial recovery.",
        MicrocycleType.RECOVERY:    "Load reduced to 50% of CTL baseline. "
                                    "Recovery weeks every 3–4 weeks are mandatory, not optional "
                                    "(Issurin 2008). Supercompensation occurs during rest, not load.",
        MicrocycleType.TAPER:       "Mujika & Padilla (2003) meta-analysis: optimal taper "
                                    "reduces volume 40–60%, maintains frequency and intensity. "
                                    "Duration: 8–14 days.",
        MicrocycleType.COMPETITION: "Minimal load — preserve energy for competition.",
    }
    lines.append(mc_rationale.get(microcycle_type, ""))

    lines.append(f"\nINPUT STATE: CTL={ctl:.1f}, TSB={tsb:.1f}, "
                 f"ACWR={acwr:.2f}, Readiness={readiness_score:.0f}/100, "
                 f"Risk={composite_risk:.2f}, Ramp={ramp_rate:+.1f}, Monotony={monotony:.2f}")

    return "\n".join(lines)


if __name__ == "__main__":
    from pprint import pprint

    scenarios = [
        {
            "label": "Base build — mid-accumulation, healthy athlete",
            "inputs": dict(ctl=55, atl=58, tsb=-3, acwr=1.05, monotony=1.2,
                           ramp_rate=4.0, readiness_score=72,
                           composite_risk=0.15, injury_risk_score=0.10,
                           overtraining_risk_score=0.18,
                           days_to_event=70, weeks_in_phase=2),
        },
        {
            "label": "Pre-competition taper — 10 days out",
            "inputs": dict(ctl=75, atl=65, tsb=10, acwr=0.87, monotony=1.0,
                           ramp_rate=-2.0, readiness_score=84,
                           composite_risk=0.10, injury_risk_score=0.08,
                           overtraining_risk_score=0.12,
                           days_to_event=10, weeks_in_phase=5),
        },
        {
            "label": "Overreaching — recovery override needed",
            "inputs": dict(ctl=68, atl=95, tsb=-27, acwr=1.40, monotony=2.5,
                           ramp_rate=12.0, readiness_score=35,
                           composite_risk=0.72, injury_risk_score=0.65,
                           overtraining_risk_score=0.74,
                           days_to_event=30, weeks_in_phase=3),
        },
        {
            "label": "Fresh athlete — shock week opportunity",
            "inputs": dict(ctl=60, atl=52, tsb=8, acwr=0.87, monotony=0.9,
                           ramp_rate=2.0, readiness_score=88,
                           composite_risk=0.12, injury_risk_score=0.09,
                           overtraining_risk_score=0.14,
                           days_to_event=None, weeks_in_phase=3),
        },
    ]

    for scenario in scenarios:
        print(f"\n{'═'*65}")
        print(f"  {scenario['label']}")
        print('═'*65)
        plan = generate_plan(**scenario["inputs"])

        print(f"  Phase            : {plan['phase']}")
        print(f"  Microcycle       : {plan['microcycle_type']}")
        print(f"  Objective        : {plan['week_objective']}")
        print(f"  Target load      : {plan['target_weekly_load']} ATU  "
              f"({plan['load_adjustment_pct']:+.1f}% vs baseline)")
        print(f"  Projected ΔTSB   : {plan['projected_tsb_delta']:+.1f}")
        print(f"  Target ACWR      : {plan['target_acwr']:.2f}")
        print()
        print("  WEEKLY SCHEDULE:")
        for s in plan["sessions"]:
            marker = " ★" if s["key_session"] else ""
            if s["is_rest"]:
                print(f"    {s['day_name']}: REST")
            else:
                print(f"    {s['day_name']}: {s['session_type']:30s} "
                      f"{s['duration_minutes']:3d} min  RPE {s['rpe_target']:.0f}{marker}")
        print()
        print("  COACH NOTES:")
        for note in plan["coach_notes"]:
            print(f"    • {note}")