import os
os.environ["PYTHONIOENCODING"] = "utf-8"
import sys
import io

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from database import supabase
from routers import scan_router, sandbox_router, plan_router
from routers import saved_links_router
from routers import faq_router
from auth_routes import router as auth_router
from approval_router import router as approval_router
from admin_router import router as admin_router
from platform_router import router as platform_router

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdjcHFhcnJ2a2NpemVmc3pteXhpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTgwOTIyNiwiZXhwIjoyMDg3Mzg1MjI2fQ.7opGKn-G_CqP4hJXPZYROYp2rt0Bp9x-P5EWMnRGflc"
SUPABASE_URL         = "https://gcpqarrvkcizefszmyxi.supabase.co"
APPROVAL_PAGE_STORAGE_URL = f"{SUPABASE_URL}/storage/v1/object/public/approval/approve.html"

def upload_approval_page():
    """Upload approve_page.html to Supabase Storage so approval links work
    without the local server needing to be running. Uses only stdlib urllib."""
    try:
        import json
        from urllib.request import urlopen, Request
        from urllib.error import HTTPError

        html_path = os.path.join(os.path.dirname(__file__), "approve_page.html")
        if not os.path.exists(html_path):
            print("[STARTUP] approve_page.html not found, skipping upload")
            return
        with open(html_path, "rb") as f:
            content = f.read()

        base    = f"{SUPABASE_URL}/storage/v1"
        auth_h  = {"Authorization": f"Bearer {SUPABASE_SERVICE_KEY}",
                   "apikey": SUPABASE_SERVICE_KEY}

        # ── 1. Ensure bucket exists ───────────────────────────────────────────
        try:
            urlopen(Request(f"{base}/bucket/approval", headers=auth_h))
        except HTTPError as e:
            if e.code in (400, 404):
                body = json.dumps({"id": "approval", "name": "approval", "public": True}).encode()
                req  = Request(f"{base}/bucket", data=body, method="POST",
                               headers={**auth_h, "Content-Type": "application/json"})
                try:
                    urlopen(req)
                except HTTPError:
                    pass  # bucket may already exist under a different error code

        # ── 2. Upload (POST with x-upsert overwrites if already exists) ───────
        upload_h = {**auth_h,
                    "Content-Type": "text/html; charset=utf-8",
                    "x-upsert":     "true"}
        req = Request(f"{base}/object/approval/approve.html",
                      data=content, method="POST", headers=upload_h)
        try:
            urlopen(req)
            print(f"[STARTUP] ✅ Approval page live at: {APPROVAL_PAGE_STORAGE_URL}")
        except HTTPError as e:
            err = e.read().decode()
            print(f"[STARTUP] ⚠️  Storage upload failed ({e.code}): {err}")

    except Exception as e:
        print(f"[STARTUP] ⚠️  Approval page upload skipped: {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    upload_approval_page()
    yield

app = FastAPI(
    title="LinkScanner API",
    description="Backend API for LinkScanner Android application",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(approval_router)
app.include_router(admin_router)
app.include_router(platform_router)
app.include_router(scan_router.router)
app.include_router(faq_router.router)
app.include_router(sandbox_router.router,     prefix="/sandbox")
app.include_router(plan_router.router,        prefix="/plan")
app.include_router(saved_links_router.router)

@app.get("/")
def home():
    return {"message": "Backend is running"}
