# Fatigue Engine

Implementación del motor de fatiga para el sistema Adaptive Strength Intelligence Platform.

## Métricas calculadas

ATL (Acute Training Load)
Promedio de carga de los últimos 7 días.

CTL (Chronic Training Load)
Promedio de carga de los últimos 28 días.

ACWR (Acute Chronic Workload Ratio)
ATL / CTL.

TSB (Training Stress Balance)
CTL - ATL.

## Carga diaria

load = durationMinutes * rpe

## Endpoint

POST /api/v1/metrics/fatigue

## Ejemplo respuesta

{
  "atl": 400,
  "ctl": 350,
  "acwr": 1.14,
  "tsb": -50
}
