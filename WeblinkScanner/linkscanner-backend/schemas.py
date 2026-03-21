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
    valid:  Optional[bool]  = None
    issuer: Optional[str]   = None
    expiry: Optional[str]   = None

class SandboxRequest(BaseModel):
    url:     str
    scan_id: str

class SandboxReport(BaseModel):
    sandbox_id:     str
    url:            str
    status_code:    Optional[int]      = None
    page_title:     Optional[str]      = None
    ip_address:     Optional[str]      = None
    load_time_ms:   Optional[int]      = None
    redirect_chain: List[str]          = []
    external_links: List[str]          = []
    ssl_info:       Optional[SSLInfo]  = None
    created_at:     datetime

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
