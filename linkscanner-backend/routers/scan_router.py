# routers/scan_router.py

import re
import uuid
from datetime import datetime, timezone, date
from fastapi import APIRouter, HTTPException
from typing import List
from schemas import (
    ScanRequest, ScanResponse,
    CameraScanRequest, CameraScanResponse,
    QRScanRequest, QRScanResponse,
)
from services.scan_service import perform_scan
from database import supabase

router = APIRouter(prefix="/scan", tags=["Scan"])

# ── Change this to adjust free plan daily limit ───────────────────────────────
FREE_DAILY_LIMIT = 5


URL_REGEX = re.compile(r"https?://[^\s]+", re.IGNORECASE)

def extract_url(text: str):
    text = text.strip()
    # First try explicit http/https URL (same as ScanUrl input)
    match = re.search(r"https?://[^\s]+", text, re.IGNORECASE)
    if match:
        return match.group(0)
    # Then try bare domain like google.com or www.google.com
    match = re.search(r"(?:www\.)?[a-zA-Z0-9][a-zA-Z0-9\-]+\.[a-zA-Z]{2,}(?:/[^\s]*)?", text)
    if match:
        return match.group(0)
    return None


def check_quota_and_get_remaining(user_id: str) -> int:
    """
    Counts today's scans directly from scan_records.
    No writes — just a count query. Returns remaining scans.
    Raises 429 if limit reached.
    """
    GUEST_ID = "00000000-0000-0000-0000-000000000000"
    if user_id == GUEST_ID:
        return FREE_DAILY_LIMIT

    try:
        # Get user plan and limit
        user_result = supabase.table("users") \
            .select("plan, daily_scan_limit") \
            .eq("id", user_id) \
            .single() \
            .execute()

        if not user_result.data:
            return FREE_DAILY_LIMIT

        plan  = user_result.data.get("plan", "free")
        limit = user_result.data.get("daily_scan_limit") or FREE_DAILY_LIMIT

        # Only enforce for free plan
        if plan != "free":
            return limit

        # Count today's scans from scan_records using UTC midnight
        today_start = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0).isoformat()
        count_result = supabase.table("scan_records") \
            .select("id", count="exact") \
            .eq("user_id", user_id) \
            .gte("created_at", today_start) \
            .execute()

        scans_today = count_result.count if count_result.count is not None else 0

        if scans_today >= limit:
            raise HTTPException(
                status_code=429,
                detail=f"Daily scan limit of {limit} reached. Upgrade your plan."
            )

        remaining = limit - scans_today - 1  # -1 for the scan about to happen
        return max(remaining, 0)

    except HTTPException:
        raise
    except Exception:
        return FREE_DAILY_LIMIT


def save_scan(user_id: str, url: str, result: dict, source: str) -> str:
    scan_id = str(uuid.uuid4())
    try:
        # Write to scan_history (display) — scan_records is quota only
        supabase.table("scan_history").insert({
            "scan_id":           scan_id,
            "user_id":           user_id,
            "url":               url,
            "verdict":           result.get("verdict", "UNKNOWN"),
            "risk_level":        result.get("verdict", "UNKNOWN"),
            "threat_categories": result.get("threat_categories", []),
            "scan_source":       source,
        }).execute()
        # Also write to scan_records for quota tracking
        supabase.table("scan_records").insert({
            "id":                scan_id,
            "user_id":           user_id,
            "scanned_url":       url,
            "verdict":           result.get("verdict", "UNKNOWN"),
            "scan_source":       source,
            "threat_categories": result.get("threat_categories", []),
            "blacklist_match":   False,
        }).execute()
    except Exception as e:
        print(f"[save_scan ERROR] {e}", flush=True)
    return scan_id


# ── /scan/url ─────────────────────────────────────────────────────────────────
@router.post("/url", response_model=ScanResponse)
async def scan_url(body: ScanRequest):
    user_id   = body.user_id or "00000000-0000-0000-0000-000000000000"
    remaining = check_quota_and_get_remaining(user_id)
    result    = await perform_scan(body.url)
    scan_id   = save_scan(user_id, body.url, result, "manual")
    return ScanResponse(
        scan_id           = scan_id,
        url               = body.url,
        scanned_at        = datetime.now(timezone.utc),
        verdict           = result["verdict"],
        risk_score        = result["risk_score"],
        reason_1          = result.get("reason_1"),
        reason_2          = result.get("reason_2"),
        reason_3          = result.get("reason_3"),
        threat_categories = result.get("threat_categories", []),
        scans_remaining   = remaining,
    )


# ── /scan/camera ──────────────────────────────────────────────────────────────
@router.post("/camera", response_model=CameraScanResponse)
async def scan_camera(body: CameraScanRequest):
    extracted_url = extract_url(body.extracted_text)
    if not extracted_url:
        return CameraScanResponse(extracted_url=None, is_url=False, scan_result=None)
    user_id   = body.user_id or "00000000-0000-0000-0000-000000000000"
    remaining = check_quota_and_get_remaining(user_id)
    result    = await perform_scan(extracted_url)
    scan_id   = save_scan(user_id, extracted_url, result, "camera")
    scan_response = ScanResponse(
        scan_id           = scan_id,
        url               = extracted_url,
        scanned_at        = datetime.now(timezone.utc),
        verdict           = result["verdict"],
        risk_score        = result["risk_score"],
        reason_1          = result.get("reason_1"),
        reason_2          = result.get("reason_2"),
        reason_3          = result.get("reason_3"),
        threat_categories = result.get("threat_categories", []),
        scans_remaining   = remaining,
    )
    return CameraScanResponse(extracted_url=extracted_url, is_url=True, scan_result=scan_response)


# ── /scan/qr ──────────────────────────────────────────────────────────────────
@router.post("/qr", response_model=QRScanResponse)
async def scan_qr(body: QRScanRequest):
    extracted_url = extract_url(body.raw_qr_data)
    if not extracted_url:
        return QRScanResponse(extracted_url=None, is_url=False, scan_result=None)
    user_id   = body.user_id or "00000000-0000-0000-0000-000000000000"
    remaining = check_quota_and_get_remaining(user_id)
    result    = await perform_scan(extracted_url)
    scan_id   = save_scan(user_id, extracted_url, result, "qr")
    scan_response = ScanResponse(
        scan_id           = scan_id,
        url               = extracted_url,
        scanned_at        = datetime.now(timezone.utc),
        verdict           = result["verdict"],
        risk_score        = result["risk_score"],
        reason_1          = result.get("reason_1"),
        reason_2          = result.get("reason_2"),
        reason_3          = result.get("reason_3"),
        threat_categories = result.get("threat_categories", []),
        scans_remaining   = remaining,
    )
    return QRScanResponse(extracted_url=extracted_url, is_url=True, scan_result=scan_response)


# ── /scan/history/delete ─────────────────────────────────────────────────────
@router.post("/history/delete")
def delete_history_items(ids: List[str]):
    try:
        supabase.table("scan_history") \
            .delete() \
            .in_("scan_id", ids) \
            .execute()
        return {"message": f"Deleted {len(ids)} item(s)"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# ── /scan/history/{user_id} ───────────────────────────────────────────────────
@router.get("/history/{user_id}")
def get_scan_history(user_id: str):
    try:
        result = supabase.table("scan_history") \
            .select("scan_id, url, verdict, threat_categories, created_at, scan_source") \
            .eq("user_id", user_id) \
            .order("created_at", desc=True) \
            .limit(50) \
            .execute()
        return [
            {
                "scan_id":           r["scan_id"],
                "url":               r["url"],
                "verdict":           r["verdict"],
                "risk_score":        0,
                "threat_categories": r.get("threat_categories") or [],
                "scanned_at":        r["created_at"],
                "scans_remaining":   None,
            }
            for r in (result.data or [])
        ]
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
