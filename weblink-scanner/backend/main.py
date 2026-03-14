# backend/main.py
# Entry point of the FastAPI backend

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from routers import scan_router
from routers import sandbox_router
from routers import plan_router
#from routers import history_router

# -------------------------------------------
# Create FastAPI application
# -------------------------------------------

app = FastAPI(
    title="Weblink Scanner API",
    description="Backend API for WeblinkScanner Android application",
    version="1.0.0",
)

# -------------------------------------------
# allow Android app to call the backend
# -------------------------------------------

origins = [
    "http://10.0.0.0:8000",   # Android Phone/Emulator host
    "http://localhost:8000",
    "http://127.0.0.1:8000",
    "*"                        # or just allow all for testing
]

# -------------------------------------------
# CORS (allows Android emulator to call API)
# -------------------------------------------

app.add_middleware(CORSMiddleware,
                 allow_origins=origins,
                 allow_credentials=True,
                 allow_methods=["*"],
                 allow_headers=["*"],
)


# -------------------------------------------
# Routers
# -------------------------------------------

app.include_router(scan_router.router)
app.include_router(sandbox_router.router, prefix="/sandbox", tags=["Sandbox"])
app.include_router(plan_router.router, prefix="/plan", tags=["Plan"])
#app.include_router(history_router.router)


# -------------------------------------------
# Health check endpoint
# -------------------------------------------

@app.get("/")
async def root():
    return {"message": "WeblinkScanner API running"}
