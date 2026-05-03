# backend/main.py

import sys
import asyncio

if sys.platform == "win32":
    try:
        if not isinstance(asyncio.get_event_loop_policy(), asyncio.WindowsSelectorEventLoopPolicy):
            asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    except Exception:
        pass

import os
os.environ["PYTHONIOENCODING"] = "utf-8"
import io

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from database import supabase
from routers import (
    auth_routes,
    admin_router,
    approval_router,
    platform_router,
    faq_router,
    plan_router,
    sandbox_router,
    saved_links_router,
    scan_router
)

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

app = FastAPI(
    title="WeblinkScanner API",
    description="Backend API for Weblink Scanner Android application",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_routes.router)
app.include_router(admin_router.router)
app.include_router(approval_router.router)
app.include_router(platform_router.router)
app.include_router(faq_router.router)
app.include_router(plan_router.router, prefix="/plan", tags=["Plans"])
app.include_router(sandbox_router.router, prefix="/sandbox", tags=["Sandbox"])
app.include_router(saved_links_router.router)
app.include_router(scan_router.router)

@app.get("/")
def home():
    return {"status": "online", "message": "Backend is running"}