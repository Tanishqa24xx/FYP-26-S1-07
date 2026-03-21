# routers/sandbox_router.py

from fastapi import APIRouter, HTTPException
from services.sandbox_service import run_sandbox
from schemas import SandboxRequest, SandboxReport
import uuid
from datetime import datetime, timezone

router = APIRouter()

@router.post("/analyse", response_model=SandboxReport)
async def analyse_sandbox(body: SandboxRequest):
    try:
        report = await run_sandbox(body.url)
        return SandboxReport(
            sandbox_id = str(uuid.uuid4()),
            url = body.url,
            status_code = report.get("status_code"),
            page_title = report.get("page_title"),
            ip_address = report.get("ip_address"),
            load_time_ms = report.get("load_time_ms"),
            redirect_chain = report.get("redirect_chain", []),
            external_links = report.get("external_links", []),
            ssl_info = report.get("ssl_info"),
            created_at = datetime.now(timezone.utc)
        )
    except Exception as e:
        # Return a valid response even on error so the app doesn't crash
        return SandboxReport(
            sandbox_id = str(uuid.uuid4()),
            url = body.url,
            status_code = None,
            page_title = f"Analysis error: {str(e)[:100]}",
            ip_address = None,
            load_time_ms = 0,
            redirect_chain = [],
            external_links = [],
            ssl_info = None,
            created_at = datetime.now(timezone.utc)
        )
