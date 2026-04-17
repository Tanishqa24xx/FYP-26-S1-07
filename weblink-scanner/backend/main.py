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
from routers import scan_router, sandbox_router, plan_router
from routers import saved_links_router
from routers import faq_router
from auth_routes import router as auth_router

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

app.include_router(auth_router)
app.include_router(scan_router.router)
app.include_router(faq_router.router)
app.include_router(sandbox_router.router, prefix="/sandbox")
app.include_router(plan_router.router, prefix="/plan")
app.include_router(saved_links_router.router)

@app.get("/")
def home():
    return {"message": "Backend is running"}