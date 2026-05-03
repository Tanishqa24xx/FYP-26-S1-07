# routers/scan_router.py

import re
import csv
import io
import uuid
from datetime import datetime, timezone, date, timedelta
from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import StreamingResponse
from typing import List
from schemas import (
    ScanRequest, ScanResponse,
    CameraScanRequest, CameraScanResponse,
    QRScanRequest, QRScanResponse,
)
from services.scan_service import perform_scan
from database import supabase

router = APIRouter(prefix="/scan", tags=["Scan"])

# Defines how many scans each plan gets per day.
PLAN_DAILY_LIMITS = {
    "free":     5,
    "standard": 30,
    "premium":  999999,  # effectively unlimited
}

URL_REGEX = re.compile(r"https?://[^\s]+", re.IGNORECASE)


# Pulls a valid URL out of a block of text, supporting both full links and bare domains.
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


# Checks if the user has reached their daily scan limit based on their subscription tier.
def check_quota_and_get_plan(user_id: str) -> tuple:
    GUEST_ID = "00000000-0000-0000-0000-000000000000"
    if user_id == GUEST_ID:
        return PLAN_DAILY_LIMITS["free"], "free"

    try:
        rows = supabase.table("users") \
                   .select("plan") \
                   .eq("id", user_id) \
                   .execute().data or []

        if not rows:
            return PLAN_DAILY_LIMITS["free"], "free"

        plan = rows[0].get("plan", "free").lower()
        limit = PLAN_DAILY_LIMITS.get(plan, PLAN_DAILY_LIMITS["free"])

        if plan == "premium":
            return limit, plan  # no quota check for premium

        today_start = datetime.now(timezone.utc).replace(
            hour=0, minute=0, second=0, microsecond=0
        ).isoformat()
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

        remaining = limit - scans_today - 1
        return max(remaining, 0), plan

    except HTTPException:
        raise
    except Exception:
        return PLAN_DAILY_LIMITS["free"], "free"


# Logs scan details to history for the user and records usage for quota tracking.
def save_scan(user_id: str, url: str, result: dict, source: str) -> str:
    scan_id = str(uuid.uuid4())
    try:
        # Write to scan_history (display). scan_records is quota only
        supabase.table("scan_history").insert({
            "scan_id": scan_id,
            "user_id": user_id,
            "url": url,
            "verdict": result.get("verdict", "UNKNOWN"),
            "risk_level": result.get("verdict", "UNKNOWN"),
            "threat_categories": result.get("threat_categories", []),
            "scan_source": source,
        }).execute()
        # Also write to scan_records for quota tracking
        supabase.table("scan_records").insert({
            "id": scan_id,
            "user_id": user_id,
            "scanned_url": url,
            "verdict": result.get("verdict", "UNKNOWN"),
            "scan_source": source,
            "threat_categories": result.get("threat_categories", []),
            "blacklist_match": False,
        }).execute()
    except Exception as e:
        print(f"[save_scan ERROR] {e}", flush=True)
    return scan_id


# --- /scan/url ---
# Processes a manual URL scan request after verifying permissions and remaining quota.
@router.post("/url", response_model=ScanResponse)
async def scan_url(body: ScanRequest):
    user_id = body.user_id or "00000000-0000-0000-0000-000000000000"
    check_user_permission(user_id, "scan_url")
    remaining, plan = check_quota_and_get_plan(user_id)
    result = await perform_scan(body.url)
    scan_id = save_scan(user_id, body.url, result, "manual")
    return ScanResponse(
        scan_id = scan_id,
        url = body.url,
        scanned_at = datetime.now(timezone.utc),
        verdict = result["verdict"],
        risk_score = result["risk_score"],
        reason_1 = result.get("reason_1"),
        reason_2 = result.get("reason_2"),
        reason_3 = result.get("reason_3"),
        threat_categories = result.get("threat_categories", []),
        scans_remaining = remaining,
    )


# --- /scan/camera ---
# Analyzes text extracted from a camera to find a URL and perform a security scan.
@router.post("/camera", response_model=CameraScanResponse)
async def scan_camera(body: CameraScanRequest):
    extracted_url = extract_url(body.extracted_text)
    if not extracted_url:
        return CameraScanResponse(extracted_url=None, is_url=False, scan_result=None)
    user_id = body.user_id or "00000000-0000-0000-0000-000000000000"
    check_user_permission(user_id, "scan_camera")
    remaining, plan = check_quota_and_get_plan(user_id)
    result = await perform_scan(extracted_url)
    scan_id = save_scan(user_id, extracted_url, result, "camera")
    scan_response = ScanResponse(
        scan_id = scan_id,
        url = extracted_url,
        scanned_at = datetime.now(timezone.utc),
        verdict = result["verdict"],
        risk_score = result["risk_score"],
        reason_1 = result.get("reason_1"),
        reason_2 = result.get("reason_2"),
        reason_3 = result.get("reason_3"),
        threat_categories = result.get("threat_categories", []),
        scans_remaining = remaining,
    )
    return CameraScanResponse(extracted_url=extracted_url, is_url=True, scan_result=scan_response)


# --- /scan/qr ---
# Extracts data from a QR code and scans it if it contains a valid web link.
@router.post("/qr", response_model=QRScanResponse)
async def scan_qr(body: QRScanRequest):
    extracted_url = extract_url(body.raw_qr_data)
    if not extracted_url:
        return QRScanResponse(extracted_url=None, is_url=False, scan_result=None)
    user_id = body.user_id or "00000000-0000-0000-0000-000000000000"
    check_user_permission(user_id, "scan_qr")
    remaining, plan = check_quota_and_get_plan(user_id)
    result = await perform_scan(extracted_url)
    scan_id = save_scan(user_id, extracted_url, result, "qr")
    scan_response = ScanResponse(
        scan_id = scan_id,
        url = extracted_url,
        scanned_at = datetime.now(timezone.utc),
        verdict = result["verdict"],
        risk_score = result["risk_score"],
        reason_1 = result.get("reason_1"),
        reason_2 = result.get("reason_2"),
        reason_3 = result.get("reason_3"),
        threat_categories = result.get("threat_categories", []),
        scans_remaining = remaining,
    )
    return QRScanResponse(extracted_url=extracted_url, is_url=True, scan_result=scan_response)


# --- /scan/history/delete ---
# Deletes specific entries from the user's scan history based on provided IDs.
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


# --- /scan/history/{user_id} ---
# Retrieves previous scan records for a user, applying limits based on their plan.
@router.get("/history/{user_id}")
def get_scan_history(user_id: str):
    try:
        # Default history limit for free users
        history_limit = 5

        # check user plan to adjust history limit
        try:
            rows = supabase.table("users") \
                .select("plan") \
                .eq("id", user_id) \
                .execute().data or []

            if rows:
                plan = rows[0].get("plan", "free")
                if plan == "premium":
                    history_limit = 2000
                elif plan == "standard":
                    history_limit = 500
        except Exception as e:
            print(f"[History Plan Check Error] {e}")
            # Fallback to history_limit = 5 already set above

        # Fetch history records
        if plan == "standard":
            # Standard: last 30 days only
            since = (datetime.now(timezone.utc) - timedelta(days=30)).isoformat()
            result = supabase.table("scan_history") \
                .select("scan_id, url, verdict, threat_categories, created_at, scan_source") \
                .eq("user_id", user_id) \
                .gte("created_at", since) \
                .order("created_at", desc=True) \
                .limit(history_limit) \
                .execute()
        else:
            result = supabase.table("scan_history") \
                .select("scan_id, url, verdict, threat_categories, created_at, scan_source") \
                .eq("user_id", user_id) \
                .order("created_at", desc=True) \
                .limit(history_limit) \
                .execute()

        return [
            {
                "scan_id": r["scan_id"],
                "url": r["url"],
                "verdict": r["verdict"],
                "risk_score": 0,
                "threat_categories": r.get("threat_categories") or [],
                "scanned_at": r["created_at"],
                "scans_remaining": None,
            }
            for r in (result.data or [])
        ]
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# --- /scan/export ---
# Generates a CSV or PDF report of the user's scan history for Standard and Premium tiers.
@router.get("/export")
def export_scan_history(
    user_id: str = Query(...),
    fmt: str = Query(default="csv"),  # "csv" or "pdf"
):
    try:
        # get user plan
        rows = supabase.table("users") \
            .select("plan") \
            .eq("id", user_id) \
            .execute().data or []

        plan = rows[0].get("plan", "free") if rows else "free"

        if plan == "free":
            raise HTTPException(status_code=403, detail="Export is available for Standard and Premium plans only.")

        # Determine date range
        now = datetime.now(timezone.utc)
        if plan == "standard":
            since = (now - timedelta(days=30)).isoformat()
            query = supabase.table("scan_history") \
                .select("url, verdict, threat_categories, created_at, scan_source") \
                .eq("user_id", user_id) \
                .gte("created_at", since) \
                .order("created_at", desc=True) \
                .limit(500) \
                .execute()
        else:  # premium
            query = supabase.table("scan_history") \
                .select("url, verdict, threat_categories, created_at, scan_source") \
                .eq("user_id", user_id) \
                .order("created_at", desc=True) \
                .limit(2000) \
                .execute()

        rows = query.data or []

        # --- CSV export ---
        if fmt == "csv":
            output = io.StringIO()
            writer = csv.writer(output)
            writer.writerow(["URL", "Verdict", "Threat Categories", "Scanned At", "Source"])
            for r in rows:
                cats = ", ".join(r.get("threat_categories") or [])
                writer.writerow([
                    r.get("url", ""),
                    r.get("verdict", ""),
                    cats,
                    r.get("created_at", ""),
                    r.get("scan_source", ""),
                ])
            output.seek(0)
            filename = f"weblinkscanner_history_{now.strftime('%Y%m%d')}.csv"
            return StreamingResponse(
                iter([output.getvalue()]),
                media_type="text/csv",
                headers={"Content-Disposition": f"attachment; filename={filename}"}
            )

        # --- PDF export (Standard + Premium) ---
        elif fmt == "pdf":
            try:
                from reportlab.lib.pagesizes import A4
                from reportlab.lib import colors
                from reportlab.lib.units import cm
                from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer
                from reportlab.lib.styles import getSampleStyleSheet
            except ImportError:
                raise HTTPException(status_code=500, detail="PDF export requires reportlab. Install with: pip install reportlab")

            buf = io.BytesIO()
            doc = SimpleDocTemplate(buf, pagesize=A4,
                                    leftMargin=1.5*cm, rightMargin=1.5*cm,
                                    topMargin=2*cm, bottomMargin=2*cm)
            styles = getSampleStyleSheet()
            elements = []

            # Title
            elements.append(Paragraph("Weblink Scanner — Scan History Report", styles["Title"]))
            elements.append(Paragraph(
                f"User: {user_id}   |   Generated: {now.strftime('%Y-%m-%d %H:%M UTC')}   |   Plan: {plan.capitalize()}",
                styles["Normal"]
            ))
            elements.append(Spacer(1, 0.5*cm))

            # Table header
            data = [["URL", "Verdict", "Threat Categories", "Scanned At", "Source"]]
            VERDICT_COLORS = {
                "SAFE":       colors.HexColor("#16A34A"),
                "SUSPICIOUS": colors.HexColor("#D97706"),
                "DANGEROUS":  colors.HexColor("#DC2626"),
            }
            row_colors = {}
            for i, r in enumerate(rows, start=1):
                cats = ", ".join(r.get("threat_categories") or []) or "-"
                url_truncated = r.get("url", "")[:60] + ("…" if len(r.get("url", "")) > 60 else "")
                data.append([
                    url_truncated,
                    r.get("verdict", ""),
                    cats[:50] + ("…" if len(cats) > 50 else ""),
                    r.get("created_at", "")[:10],
                    r.get("scan_source", ""),
                ])
                verdict = r.get("verdict", "").upper()
                if verdict in VERDICT_COLORS:
                    row_colors[i] = VERDICT_COLORS[verdict]

            table = Table(data, colWidths=[7*cm, 2.5*cm, 5*cm, 2.5*cm, 2*cm])
            style_cmds = [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1E40AF")),
                ("TEXTCOLOR",  (0, 0), (-1, 0), colors.white),
                ("FONTNAME",   (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE",   (0, 0), (-1, -1), 8),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F8FAFC")]),
                ("GRID",       (0, 0), (-1, -1), 0.25, colors.HexColor("#E2E8F0")),
                ("VALIGN",     (0, 0), (-1, -1), "MIDDLE"),
                ("PADDING",    (0, 0), (-1, -1), 5),
            ]
            for row_idx, col in row_colors.items():
                style_cmds.append(("TEXTCOLOR", (1, row_idx), (1, row_idx), col))
                style_cmds.append(("FONTNAME",  (1, row_idx), (1, row_idx), "Helvetica-Bold"))

            table.setStyle(TableStyle(style_cmds))
            elements.append(table)
            doc.build(elements)
            buf.seek(0)

            filename = f"weblinkscanner_history_{now.strftime('%Y%m%d')}.pdf"
            return StreamingResponse(
                buf,
                media_type="application/pdf",
                headers={"Content-Disposition": f"attachment; filename={filename}"}
            )

        else:
            raise HTTPException(status_code=400, detail="Invalid format. Use 'csv' or 'pdf'.")

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Internal check to ensure the user's assigned profile allows for the requested action.
def check_user_permission(user_id: str, permission: str):
    GUEST_ID = "00000000-0000-0000-0000-000000000000"
    if user_id == GUEST_ID:
        return  # guests bypass profile check
    try:
        rows = supabase.table("users").select("profile_id").eq("id", user_id).execute().data or []
        if not rows or not rows[0].get("profile_id"):
            return  # no profile assigned = no restrictions
        profile_id = rows[0]["profile_id"]
        profile = supabase.table("user_profiles").select("permissions, status") \
                      .eq("id", profile_id).execute().data or []
        if not profile:
            return
        if profile[0].get("status") == "suspended":
            raise HTTPException(status_code=403, detail="Your access profile has been suspended.")
        permissions = profile[0].get("permissions") or []
        if permission not in permissions:
            raise HTTPException(status_code=403,
                                detail=f"Your access profile does not allow '{permission}'. Contact your administrator.")
    except HTTPException:
        raise
    except Exception:
        pass  # on error, don't block the user