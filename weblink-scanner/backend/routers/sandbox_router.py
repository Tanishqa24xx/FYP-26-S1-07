# sandbox_router.py

from fastapi import APIRouter
from services.sandbox_service import run_sandbox
from schemas import SandboxRequest, SandboxReport, SSLInfo, AsnInfo
import uuid as uuid_lib
from datetime import datetime, timezone

router = APIRouter()


def build_report(report: dict, original_url: str) -> SandboxReport:
    ssl_raw = report.get("ssl_info")
    ssl_info = SSLInfo(
        valid      = ssl_raw.get("valid"),
        issuer     = ssl_raw.get("issuer"),
        valid_from = ssl_raw.get("valid_from"),
        valid_days = ssl_raw.get("valid_days"),
        age_days   = ssl_raw.get("age_days"),
        protocol   = ssl_raw.get("protocol"),
    ) if ssl_raw else None

    asn_raw  = report.get("asn_info")
    asn_info = AsnInfo(
        asn     = asn_raw.get("asn"),
        asnname = asn_raw.get("asnname"),
        country = asn_raw.get("country"),
    ) if asn_raw else None

    return SandboxReport(
        sandbox_id   = report.get("sandbox_uuid") or str(uuid_lib.uuid4()),
        url          = original_url,
        created_at   = datetime.now(timezone.utc),

        status_code  = report.get("status_code"),
        page_title   = report.get("page_title"),
        ip_address   = report.get("ip_address"),
        load_time_ms = report.get("load_time_ms"),
        final_url    = report.get("final_url"),
        server       = report.get("server"),
        mime_type    = report.get("mime_type"),
        ptr          = report.get("ptr"),
        country      = report.get("country"),
        city         = report.get("city"),
        apex_domain  = report.get("apex_domain"),

        ssl_info     = ssl_info,

        redirect_chain    = report.get("redirect_chain",    []),
        external_links    = report.get("external_links",    []),
        domains_contacted = report.get("domains_contacted", []),
        domain_count      = report.get("domain_count"),
        ips_contacted     = report.get("ips_contacted",     []),
        ip_count          = report.get("ip_count"),
        urls_contacted    = report.get("urls_contacted",    []),

        tech_detected    = report.get("tech_detected",    []),
        console_messages = report.get("console_messages", []),
        total_size_kb    = report.get("total_size_kb"),
        total_requests   = report.get("total_requests"),

        verdict_score      = report.get("verdict_score"),
        verdict_categories = report.get("verdict_categories", []),
        malicious          = report.get("malicious"),

        asn_info = asn_info,

        screenshot_url = report.get("screenshot_url"),
        screenshot_b64 = report.get("screenshot_b64"),
        report_url     = report.get("report_url"),
        sandbox_uuid   = report.get("sandbox_uuid"),

        analysis_source = report.get("analysis_source"),

        # Premium: ad/tracker detection
        detected_ad_tech   = report.get("detected_ad_tech",   []),
        detected_trackers  = report.get("detected_trackers",  []),
        suspicious_scripts = report.get("suspicious_scripts", []),
        ad_heavy           = report.get("ad_heavy",           False),
    )


@router.post("/analyse", response_model=SandboxReport)
async def analyse_sandbox(body: SandboxRequest):
    try:
        report = await run_sandbox(body.url)
        return build_report(report, body.url)
    except Exception as e:
        return SandboxReport(
            sandbox_id      = str(uuid_lib.uuid4()),
            url             = body.url,
            page_title      = f"Analysis error: {str(e)[:100]}",
            analysis_source = "urlscan.io",
            redirect_chain  = [],
            external_links  = [],
            created_at      = datetime.now(timezone.utc),
        )