# backend/schemas.py

from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime

# --- URL SCAN ---

class ScanRequest(BaseModel):
    url: str
    user_id: Optional[str] = "00000000-0000-0000-0000-000000000000"


class ScanResponse(BaseModel):
    scan_id: str
    scanned_at: datetime
    url: str
    verdict: str
    risk_score: float
    reason_1: Optional[str] = None
    reason_2: Optional[str] = None
    reason_3: Optional[str] = None
    threat_categories: List[str] = []
    scans_remaining: Optional[int] = None

# --- CAMERA OCR ---

class CameraScanRequest(BaseModel):
    extracted_text: str
    user_id: Optional[str] = "00000000-0000-0000-0000-000000000000"

class CameraScanResponse(BaseModel):
    extracted_url: Optional[str]
    is_url:        bool
    scan_result:   Optional[ScanResponse] = None

# --- QR SCAN ---

class QRScanRequest(BaseModel):
    raw_qr_data: str
    user_id: Optional[str] = "00000000-0000-0000-0000-000000000000"

class QRScanResponse(BaseModel):
    extracted_url: Optional[str]
    is_url:        bool
    scan_result:   Optional[ScanResponse] = None

# --- SANDBOX ---
class SSLInfo(BaseModel):
    valid:       Optional[bool]  = None
    issuer:      Optional[str]   = None
    valid_from:  Optional[str]   = None   # ISO-8601 from urlscan tlsValidFrom
    valid_days:  Optional[str]   = None   # validity period in days
    age_days:    Optional[str]   = None   # age of cert at scan time
    protocol:    Optional[str]   = None   # TLS version if available

class AsnInfo(BaseModel):
    asn:     Optional[str] = None
    asnname: Optional[str] = None
    country: Optional[str] = None

class SandboxRequest(BaseModel):
    url: str
    scan_id: str

class SandboxReport(BaseModel):
    sandbox_id:   str
    url:          str
    created_at:   datetime

    # Page overview
    status_code:  Optional[int]  = None
    page_title:   Optional[str]  = None
    ip_address:   Optional[str]  = None
    load_time_ms: Optional[int]  = None
    final_url:    Optional[str]  = None
    server:       Optional[str]  = None
    mime_type:    Optional[str]  = None
    ptr:          Optional[str]  = None
    country:      Optional[str]  = None
    city:         Optional[str]  = None
    apex_domain:  Optional[str]  = None

    # SSL / TLS
    ssl_info:     Optional[SSLInfo] = None

    # Network
    redirect_chain:    List[str] = []
    external_links:    List[str] = []
    domains_contacted: List[str] = []
    domain_count:      Optional[int] = None
    ips_contacted:     List[str] = []
    ip_count:          Optional[int] = None
    urls_contacted:    List[str] = []

    # Content
    tech_detected:    List[str] = []
    console_messages: List[str] = []
    total_size_kb:    Optional[int]  = None
    total_requests:   Optional[int]  = None

    # urlscan verdict
    verdict_score:      Optional[int]  = None
    verdict_categories: List[str]      = []
    malicious:          Optional[bool] = None

    # Hosting
    asn_info: Optional[AsnInfo] = None

    # Screenshot and report
    screenshot_url: Optional[str] = None
    screenshot_b64: Optional[str] = None
    report_url:     Optional[str] = None
    sandbox_uuid:   Optional[str] = None

    analysis_source: Optional[str] = None

    # Premium enrichment — ad/tracker/script analysis
    detected_ad_tech:   List[str] = []
    detected_trackers:  List[str] = []
    suspicious_scripts: List[str] = []
    ad_heavy:           bool      = False



# --- PLANS: using camelCase aliases to match Android ApiModels ---

class PlanInfo(BaseModel):
    name: str
    price: str
    scan_limit: str = Field(serialization_alias="scanLimit")
    features: List[str]

    model_config = {"populate_by_name": True}

class UserPlanResponse(BaseModel):
    current_plan: str  = Field(serialization_alias="currentPlan")
    scans_today: int  = Field(serialization_alias="scansToday")
    daily_limit: Optional[int] = Field(default=None, serialization_alias="dailyLimit")
    plan_details: PlanInfo = Field(serialization_alias="planDetails")

    model_config = {"populate_by_name": True}

class UpgradePlanRequest(BaseModel):
    new_plan: str

class UpgradePlanResponse(BaseModel):
    message:  str
    new_plan: str

# --- SAVED LINKS ---

class SaveLinkRequest(BaseModel):
    user_id: str
    url: str
    scan_id: Optional[str] = None
    risk_level: Optional[str] = None

class SavedLinkItem(BaseModel):
    id: str
    url: str
    risk_level: Optional[str]      = None
    last_checked_at: Optional[datetime] = None

class SavedLinksResponse(BaseModel):
    links: List[SavedLinkItem]


# --- Rescan saved links ---

class RescanResponse(BaseModel):
    message:       str
    quota_warning: bool = False
    remaining:     int  = 0
    total:         int  = 0
    scanned:       int  = 0
    updated:       int  = 0
    skipped:       int  = 0
