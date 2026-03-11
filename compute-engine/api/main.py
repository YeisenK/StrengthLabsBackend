from fastapi import FastAPI

from api.routers.fatigue import router as fatigue_router
from api.routers.risk import router as risk_router
from api.routers.plan import router as plan_router

app = FastAPI(
    title="Strength Labs Compute Engine",
    version="1.0.0"
)

app.include_router(fatigue_router)
app.include_router(risk_router)
app.include_router(plan_router)


@app.get("/health")
def health():
    return {"status": "ok"}
