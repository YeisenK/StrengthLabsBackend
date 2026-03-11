def generate_plan(tsb: float) -> dict:
    if tsb < -20:
        return {
            "recommendation": "reduce_volume",
            "message": "Reduce volumen e intensidad para favorecer recuperación."
        }
    elif tsb > 10:
        return {
            "recommendation": "increase_load",
            "message": "El atleta está fresco; se puede incrementar ligeramente la carga."
        }
    else:
        return {
            "recommendation": "maintain",
            "message": "Mantener carga actual."
        }
