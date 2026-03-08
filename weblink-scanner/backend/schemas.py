# backend/schemas.py

from pydantic import BaseModel
from enum import Enum
from typing import Optional, List
from datetime import datetime

# =====================================
# Risk Levels
# =====================================
class RiskLevel(str, Enum):
    SAFE = "safe"
    SUSPICIOUS = "suspicious"
    DANGEROUS = "dangerous"
    UNKNOWN = "unknown"

# =====================================
# Scan Source Types
# =====================================
class ScanSource(str, Enum):
    MANUAL = "manual"
    CAMERA = "camera"
    QR = "qr"

# =====================================
# Request: URL Scan
# =====================================
class ScanRequest(BaseModel):
    user_id: str
    url: str
    source: ScanSource = ScanSource.MANUAL

# =====================================
# Virus Summary
# =====================================
class VirusTotalSummary(BaseModel):
    malicious: int = 0
    suspicious: int = 0
    harmless: int = 0
    undetected: int = 0
    total_engines: int = 0

# =====================================
# Response: Scan Result
# =====================================
class ScanResponse(BaseModel):
    scan_id: str
    url: str
    risk_level: RiskLevel
    risk_score: float
    threat_categories: List[str]
    virustotal: Optional[VirusTotalSummary]
    scanned_at: datetime
    scan_duration_ms: int
    message: str

# =====================================
# QR Scan Request
# =====================================
class QRScanRequest(BaseModel):
    user_id: str
    qr_data: str

# =====================================
# Camera Scan Request
# =====================================
class CameraScanRequest(BaseModel):
    user_id: str
    extracted_text: str

# =====================================
# Sandbox Analysis Result
# =====================================
class SandboxReport(BaseModel):
    url: str
    page_title: Optional[str]
    status_code: Optional[int]
    external_links: List[str]
    scripts_found: List[str]
    redirect_chain: List[str]
    ip_address: Optional[str]
    hosting_country: Optional[str]
    malware_signals: List[str]
    phishing_signals: List[str]
    load_time_ms: Optional[int]
    screenshot_url: Optional[str]
    scanned_at: datetime

# =====================================
# Scan History Item
# =====================================
class ScanHistoryItem(BaseModel):
    scan_id: str
    url: str
    risk_level: RiskLevel
    source: ScanSource
    threat_categories: List[str]
    is_saved: bool
    scanned_at: datetime



