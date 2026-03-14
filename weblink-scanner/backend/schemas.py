# backend/schemas.py
# Pydantic models for request/response payloads

from pydantic import BaseModel
from enum import Enum
from typing import Optional, List
from datetime import datetime

# ----------------------------------------
# ENUMS
# ----------------------------------------

class RiskLevel(str, Enum):
    SAFE = "safe"
    SUSPICIOUS = "suspicious"
    DANGEROUS = "dangerous"
    UNKNOWN = "unknown"

class ScanSource(str, Enum):
    MANUAL = "manual"
    CAMERA = "camera"
    QR = "qr"

class Plan(str, Enum):
    FREE = "free"
    STANDARD = "standard"
    PREMIUM = "premium"

# ----------------------------------------
# SUBSCRIPTION PLAN
# ----------------------------------------

class PlanInfo(BaseModel):
    name: str
    price: str
    scan_limit: str
    features: List[str]

class UserPlanResponse(BaseModel):
    current_plan: str
    scans_today: int
    daily_limit: Optional[int]
    plan_details: PlanInfo

class UpgradePlanRequest(BaseModel):
    new_plan: str

class UpgradePlanResponse(BaseModel):
    message: str
    new_plan: str

# ----------------------------------------
# URL SCAN
# ----------------------------------------

class ScanRequest(BaseModel):
    url: str

class ScanResponse(BaseModel):
    scan_id: str
    scanned_at: datetime
    url: str
    verdict: str
    risk_score: float
    reason_1: Optional[str]
    reason_2: Optional[str]
    reason_3: Optional[str]
    threat_categories: List[str]

# ----------------------------------------
# CAMERA OCR
# ----------------------------------------

class CameraScanRequest(BaseModel):
    extracted_text: str

class CameraScanResponse(BaseModel):
    extracted_url: Optional[str]
    is_url: bool
    scan_result: Optional[ScanResponse] = None

# ----------------------------------------
# QR SCAN
# ----------------------------------------

class QRScanRequest(BaseModel):
    raw_qr_data: str

class QRScanResponse(BaseModel):
    extracted_url: Optional[str]
    is_url: bool
    scan_result: Optional[ScanResponse] = None

# ----------------------------------------
# SANDBOX ANALYSIS
# ----------------------------------------

class SSLInfo(BaseModel):
    valid: Optional[bool] = None
    issuer: Optional[str] = None
    expiry: Optional[str] = None

class SandboxRequest(BaseModel):
    url: str
    scan_id: str

class SandboxReport(BaseModel):
    sandbox_id: str
    url: str
    status_code: Optional[int] = None
    page_title: Optional[str] = None
    ip_address: Optional[str] = None
    load_time_ms: Optional[int] = None
    redirect_chain: List[str] = []
    external_links: List[str] = []
    ssl_info: Optional[SSLInfo] = None
    # screenshot_path: Optional[str] = None
    created_at: datetime

# ----------------------------------------
# SCAN HISTORY
# ----------------------------------------

class ScanHistoryItem(BaseModel):
    scan_id: str
    url: str
    risk_level: RiskLevel
    risk_score: float
    scan_source: ScanSource
    threat_categories: List[str]
    is_saved: bool
    scanned_at: datetime

class ScanHistoryResponse(BaseModel):
    items: List[ScanHistoryItem]
    total: int
    page: int
    per_page: int
