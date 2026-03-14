# WeblinkScanner\backend\routers\sandbox_router.py

from fastapi import APIRouter
from services.sandbox_service import run_sandbox
from schemas import SandboxRequest, SandboxReport
import uuid
from datetime import datetime, timezone

router = APIRouter()

@router.post("/analyse", response_model=SandboxReport)
async def analyse_sandbox(body: SandboxRequest):

    report = await run_sandbox(body.url)

    return SandboxReport(
        sandbox_id=str(uuid.uuid4()),
        url=body.url,
        status_code=report.get("status_code"),
        page_title=report.get("page_title"),
        ip_address=report.get("ip_address"),
        load_time_ms=report.get("load_time_ms"),
        redirect_chain=report.get("redirect_chain", []),
        external_links=report.get("external_links", []),
        ssl_info=report.get("ssl_info"),
        # screenshot_path=report.get("screenshot_path"),
        created_at=datetime.now(timezone.utc)
    )
