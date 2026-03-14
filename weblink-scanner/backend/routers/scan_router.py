# WeblinkScanner\backend\routers\scan_router.py
# Handles URL scanning endpoints

import re
import uuid
from datetime import datetime, timezone
from fastapi import APIRouter
from schemas import (ScanRequest,
                    ScanResponse,
                    CameraScanRequest,
                    CameraScanResponse,
                    QRScanRequest,
                    QRScanResponse,
)
# WeblinkScanner\backend\routers\scan_router.py
# Handles URL scanning endpoints

import re
import uuid
from datetime import datetime, timezone
from fastapi import APIRouter
from schemas import (ScanRequest,
                    ScanResponse,
                    CameraScanRequest,
                    CameraScanResponse,
                    QRScanRequest,
                    QRScanResponse,
)

from services.scan_service import perform_scan
from database import get_pool

router = APIRouter(prefix="/scan", tags=["Scan"])
DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000000"

# ---------------------------------------------
# Extract URL from text (used by camera + QR)
# ---------------------------------------------
URL_REGEX = re.compile(r"https?://[^\s]+", re.IGNORECASE)

def extract_url(text: str):
    match = URL_REGEX.search(text)
    if match:
        return match.group(0)

    return None

# ---------------------------------------------
# Check blacklist table
# ---------------------------------------------
async def check_blacklist(url: str):

    pool = await get_pool()

    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            SELECT url, type FROM public.black_list
            WHERE url = $1
            """,
            url
        )

    if row:
        return {
            "verdict": "DANGEROUS",
            "risk_score": 100,
            "threat_categories": ["Blacklisted URL"],
            "reason_1": "URL exists in the Blacklist database",
            "reason_2": None,
            "reason_3": None
            #"message": "This URL is known to be malicious."
        }

    return None

# ---------------------------------------------
# Save scan record
# ---------------------------------------------
async def save_scan(user_id, url: str, result: dict, source: str):

    # remove try and except condition when merging with login so it has to work with users.
    try:
        pool = await get_pool()
        scan_id = str(uuid.uuid4())

        async with pool.acquire() as conn:
            await conn.execute(
                """
                INSERT INTO public.scan_records
                (id, user_id, scanned_url, verdict,
                 reason_1, reason_2, reason_3,
                 blacklist_match, scan_source,
                 threat_categories, created_at)
                VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,NOW())
                """,
                scan_id,
                user_id,
                url,
                result["verdict"],
                result.get("reason_1"),
                result.get("reason_2"),
                result.get("reason_3"),
                result.get("blacklist_match", False),
                source,
                result.get("threat_categories", []),
            )

        return scan_id

    except Exception:
        # allow scans to work without auth user
        return str(uuid.uuid4())


# ---------------------------------------------
# Main manual scan url function
# ---------------------------------------------

@router.post("/url", response_model=ScanResponse)
async def scan_url(body: ScanRequest):

    # Check blacklist first
    blacklist_result = await check_blacklist(body.url)

    if blacklist_result:
        result = blacklist_result
    else:
        result = await perform_scan(body.url)

    #scan_id = await save_scan(DEFAULT_USER_ID, body.url, result, "manual")
    # following statement is so that it can run on local computer without needing supabase auth
    # on merging with login, remove below and keep the above.
    scan_id = str(uuid.uuid4())

    try:
        await save_scan(DEFAULT_USER_ID, body.url, result, "manual")
    except Exception:
        # skip saving if user not available
        pass

    return ScanResponse(
        scan_id=scan_id,
        url=body.url,
        scanned_at=datetime.utcnow(),
        verdict=result["verdict"],
        risk_score=result["risk_score"],
        reason_1=result.get("reason_1"),
        reason_2=result.get("reason_2"),
        reason_3=result.get("reason_3"),
        threat_categories=result.get("threat_categories", [])
    )

# ---------------------------------------------
# Main camera scan url function
# ---------------------------------------------

@router.post("/camera", response_model=CameraScanResponse)
async def scan_camera(body: CameraScanRequest):

    extracted_url = extract_url(body.extracted_text)

    if not extracted_url:
        return CameraScanResponse(
            extracted_url=None,
            is_url=False,
            scan_result=None
        )

    # first check blacklist table
    blacklist_result = await check_blacklist(extracted_url)

    if blacklist_result:
        result = blacklist_result
    else:
        result = await perform_scan(extracted_url)

    # scan_id = await save_scan(DEFAULT_USER_ID, extracted_url, result, "camera")
    scan_id = str(uuid.uuid4())
    try:
        await save_scan(DEFAULT_USER_ID, extracted_url, result, "camera")
    except Exception:
        pass

    scan_response = ScanResponse(
        scan_id=scan_id,
        url=extracted_url,
        scanned_at=datetime.now(timezone.utc),
        verdict=result["verdict"],
        risk_score=result["risk_score"],
        reason_1=result.get("reason_1"),
        reason_2=result.get("reason_2"),
        reason_3=result.get("reason_3"),
        threat_categories=result.get("threat_categories", [])
    )

    return CameraScanResponse(extracted_url=extracted_url,
                            is_url=True,
                            scan_result=scan_response
    )

# ---------------------------------------------
# Main qr scan function
# ---------------------------------------------

@router.post("/qr", response_model=QRScanResponse)
async def scan_qr(body: QRScanRequest):

    extracted_url = extract_url(body.raw_qr_data)

    if not extracted_url:
        return QRScanResponse(
            extracted_url=None,
            is_url=False,
            scan_result=None
        )

    blacklist_result = await check_blacklist(extracted_url)

    if blacklist_result:
        result = blacklist_result
    else:
        result = await perform_scan(extracted_url)

    # scan_id = await save_scan(DEFAULT_USER_ID, extracted_url, result, "qr")
    scan_id = str(uuid.uuid4())
    try:
        await save_scan(DEFAULT_USER_ID, extracted_url, result, "qr")
    except Exception:
        pass

    scan_response = ScanResponse(
        scan_id=scan_id,
        url=extracted_url,
        scanned_at=datetime.now(timezone.utc),
        verdict=result["verdict"],
        risk_score=result["risk_score"],
        reason_1=result.get("reason_1"),
        reason_2=result.get("reason_2"),
        reason_3=result.get("reason_3"),
        threat_categories=result.get("threat_categories", [])
    )

    return QRScanResponse(extracted_url=extracted_url,
                        is_url=True,
                        scan_result=scan_response
    )
from services.scan_service import perform_scan
from database import get_pool

router = APIRouter(prefix="/scan", tags=["Scan"])
DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000000"

# ---------------------------------------------
# Extract URL from text (used by camera + QR)
# ---------------------------------------------
URL_REGEX = re.compile(r"https?://[^\s]+", re.IGNORECASE)

def extract_url(text: str):
    match = URL_REGEX.search(text)
    if match:
        return match.group(0)

    return None

# ---------------------------------------------
# Check blacklist table
# ---------------------------------------------
async def check_blacklist(url: str):

    pool = await get_pool()

    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            SELECT url, type FROM public.black_list
            WHERE url = $1
            """,
            url
        )

    if row:
        return {
            "verdict": "DANGEROUS",
            "risk_score": 100,
            "threat_categories": ["Blacklisted URL"],
            "reason_1": "URL exists in the Blacklist database",
            "reason_2": None,
            "reason_3": None
            #"message": "This URL is known to be malicious."
        }

    return None

# ---------------------------------------------
# Save scan record
# ---------------------------------------------
async def save_scan(user_id, url: str, result: dict, source: str):

    # remove try and except condition when merging with login so it has to work with users.
    try:
        pool = await get_pool()
        scan_id = str(uuid.uuid4())

        async with pool.acquire() as conn:
            await conn.execute(
                """
                INSERT INTO public.scan_records
                (id, user_id, scanned_url, verdict,
                 reason_1, reason_2, reason_3,
                 blacklist_match, scan_source,
                 threat_categories, created_at)
                VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,NOW())
                """,
                scan_id,
                user_id,
                url,
                result["verdict"],
                result.get("reason_1"),
                result.get("reason_2"),
                result.get("reason_3"),
                result.get("blacklist_match", False),
                source,
                result.get("threat_categories", []),
            )

        return scan_id

    except Exception:
        # allow scans to work without auth user
        return str(uuid.uuid4())


# ---------------------------------------------
# Main manual scan url function
# ---------------------------------------------

@router.post("/url", response_model=ScanResponse)
async def scan_url(body: ScanRequest):

    # Check blacklist first
    blacklist_result = await check_blacklist(body.url)

    if blacklist_result:
        result = blacklist_result
    else:
        result = await perform_scan(body.url)

    #scan_id = await save_scan(DEFAULT_USER_ID, body.url, result, "manual")
    # following statement is so that it can run on local computer without needing supabase auth
    # on merging with login, remove below and keep the above.
    scan_id = str(uuid.uuid4())

    try:
        await save_scan(DEFAULT_USER_ID, body.url, result, "manual")
    except Exception:
        # skip saving if user not available
        pass

    return ScanResponse(
        scan_id=scan_id,
        url=body.url,
        scanned_at=datetime.utcnow(),
        verdict=result["verdict"],
        risk_score=result["risk_score"],
        reason_1=result.get("reason_1"),
        reason_2=result.get("reason_2"),
        reason_3=result.get("reason_3"),
        threat_categories=result.get("threat_categories", [])
    )

# ---------------------------------------------
# Main camera scan url function
# ---------------------------------------------

@router.post("/camera", response_model=CameraScanResponse)
async def scan_camera(body: CameraScanRequest):

    extracted_url = extract_url(body.extracted_text)

    if not extracted_url:
        return CameraScanResponse(
            extracted_url=None,
            is_url=False,
            scan_result=None
        )

    # first check blacklist table
    blacklist_result = await check_blacklist(extracted_url)

    if blacklist_result:
        result = blacklist_result
    else:
        result = await perform_scan(extracted_url)

    # scan_id = await save_scan(DEFAULT_USER_ID, extracted_url, result, "camera")
    scan_id = str(uuid.uuid4())
    try:
        await save_scan(DEFAULT_USER_ID, extracted_url, result, "camera")
    except Exception:
        pass

    scan_response = ScanResponse(scan_id=scan_id,
                                scanned_at=datetime.now(timezone.utc),
                                **result
    )

    return CameraScanResponse(extracted_url=extracted_url,
                            is_url=True,
                            scan_result=scan_response
    )

# ---------------------------------------------
# Main qr scan function
# ---------------------------------------------

@router.post("/qr", response_model=QRScanResponse)
async def scan_qr(body: QRScanRequest):

    extracted_url = extract_url(body.raw_qr_data)

    if not extracted_url:
        return QRScanResponse(
            extracted_url=None,
            is_url=False,
            scan_result=None
        )

    blacklist_result = await check_blacklist(extracted_url)

    if blacklist_result:
        result = blacklist_result
    else:
        result = await perform_scan(extracted_url)

    # scan_id = await save_scan(DEFAULT_USER_ID, extracted_url, result, "qr")
    scan_id = str(uuid.uuid4())
    try:
        await save_scan(DEFAULT_USER_ID, extracted_url, result, "qr")
    except Exception:
        pass

    scan_response = ScanResponse(scan_id=scan_id,
                                scanned_at=datetime.now(timezone.utc),
                                **result
    )

    return QRScanResponse(extracted_url=extracted_url,
                        is_url=True,
                        scan_result=scan_response
    )
