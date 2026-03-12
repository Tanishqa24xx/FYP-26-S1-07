import os
os.environ["PYTHONIOENCODING"] = "utf-8"
import sys
import io
import re
import httpx
from urllib.parse import urlparse
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from database import supabase
from auth import logout_user

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

app = FastAPI()

# ── Google Safe Browsing API Key ──────────────────────────────────────────────

GOOGLE_API_KEY = "AIzaSyATTfpZv8JVB3wAOVPg9tkaZbyMT2clNcA"


# -------- HELPER -------- #

def safe_error(e: Exception) -> str:
    return str(e).encode('ascii', 'ignore').decode('ascii')


# -------- MODELS -------- #

class URLRequest(BaseModel):
    url: str

class UserAuth(BaseModel):
    email: str
    password: str

class SignupRequest(BaseModel):
    name: str
    email: str
    password: str

class ForgotPasswordRequest(BaseModel):
    email: str

class ChangePasswordRequest(BaseModel):
    email: str
    current_password: str
    new_password: str

class ResetPasswordRequest(BaseModel):
    access_token: str
    new_password: str


# -------- RISK DETECTION -------- #

def check_heuristics(url: str) -> tuple[str, list[str]]:
    """
    Local heuristic URL analysis.
    Returns (risk_level, list of reasons found)
    """
    parsed     = urlparse(url)
    domain     = parsed.netloc.lower()
    risk_score = 0
    reasons    = []

    # ── Dangerous keywords ────────────────────────────────────────────────────
    dangerous_keywords = [
        "phishing", "malware", "ransomware", "trojan",
        "free-money", "verify-account", "login-confirm",
        "suspend", "urgent-action", "account-locked",
        "click-here-now", "limited-offer"
    ]
    for kw in dangerous_keywords:
        if kw in url.lower():
            risk_score += 3
            reasons.append(f"Dangerous keyword detected: '{kw}'")

    # ── Suspicious keywords ───────────────────────────────────────────────────
    suspicious_keywords = [
        "suspicious", "free-prize", "winner", "confirm-identity",
        "update-payment", "verify-email", "unusual-activity"
    ]
    for kw in suspicious_keywords:
        if kw in url.lower():
            risk_score += 2
            reasons.append(f"Suspicious keyword: '{kw}'")

    # ── IP address used instead of domain ─────────────────────────────────────
    if re.match(r'\d+\.\d+\.\d+\.\d+', domain):
        risk_score += 3
        reasons.append("IP address used instead of domain name")

    # ── Too many subdomains ───────────────────────────────────────────────────
    if domain.count(".") > 4:
        risk_score += 2
        reasons.append("Excessive subdomains detected")

    # ── Very long URL ─────────────────────────────────────────────────────────
    if len(url) > 100:
        risk_score += 1
        reasons.append("Unusually long URL")

    # ── @ symbol in URL ───────────────────────────────────────────────────────
    if "@" in url:
        risk_score += 3
        reasons.append("@ symbol found in URL (common phishing trick)")

    # ── Suspicious TLDs ───────────────────────────────────────────────────────
    suspicious_tlds = [".xyz", ".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".click", ".work", ".loan"]
    for tld in suspicious_tlds:
        if domain.endswith(tld):
            risk_score += 2
            reasons.append(f"Suspicious domain extension: '{tld}'")

    # ── HTTP instead of HTTPS ─────────────────────────────────────────────────
    if parsed.scheme == "http":
        risk_score += 1
        reasons.append("Not using HTTPS (insecure connection)")

    # ── Typosquatting known brands ────────────────────────────────────────────
    brands = [
        "paypal", "amazon", "google", "apple", "microsoft",
        "facebook", "netflix", "instagram", "whatsapp", "twitter",
        "steam", "ebay", "linkedin", "dropbox", "adobe"
    ]
    for brand in brands:
        if brand in domain and f"{brand}.com" not in domain and f"{brand}.net" not in domain:
            risk_score += 3
            reasons.append(f"Possible typosquatting of '{brand}'")

    # ── Excessive hyphens in domain ───────────────────────────────────────────
    if domain.count("-") > 3:
        risk_score += 1
        reasons.append("Excessive hyphens in domain name")

    # ── Numeric characters in domain (e.g. paypa1.com) ───────────────────────
    domain_name = domain.split(".")[0]
    if any(char.isdigit() for char in domain_name):
        risk_score += 1
        reasons.append("Numbers found in domain name")

    # ── Score → Risk level ────────────────────────────────────────────────────
    if risk_score >= 4:
        return "Dangerous", reasons
    elif risk_score >= 2:
        return "Suspicious", reasons
    return "Safe", reasons


async def check_google_safe_browsing(url: str) -> tuple[str, str]:
    """
    Check URL against Google Safe Browsing API.
    Returns (risk_level, threat_type or "")
    """
    if GOOGLE_API_KEY == "YOUR_GOOGLE_API_KEY_HERE":
        # Skip if API key not configured
        return "Safe", ""

    try:
        payload = {
            "client": {
                "clientId": "linkscanner",
                "clientVersion": "1.0"
            },
            "threatInfo": {
                "threatTypes": [
                    "MALWARE",
                    "SOCIAL_ENGINEERING",
                    "UNWANTED_SOFTWARE",
                    "POTENTIALLY_HARMFUL_APPLICATION"
                ],
                "platformTypes": ["ANY_PLATFORM"],
                "threatEntryTypes": ["URL"],
                "threatEntries": [{"url": url}]
            }
        }

        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.post(
                f"https://safebrowsing.googleapis.com/v4/threatMatches:find?key={GOOGLE_API_KEY}",
                json=payload
            )

        data = response.json()

        if data.get("matches"):
            threat_type = data["matches"][0].get("threatType", "UNKNOWN")
            return "Dangerous", threat_type

        return "Safe", ""

    except Exception as e:
        # If Google API fails, fall back to heuristics only
        print(f"Google Safe Browsing API error: {e}")
        return "Safe", ""


async def determine_risk(url: str) -> tuple[str, list[str]]:
    """
    Combined risk detection:
    1. Run heuristics first (fast, no API needed)
    2. If not already Dangerous, confirm with Google Safe Browsing
    Returns (risk_level, reasons)
    """
    reasons = []

    # Step 1: Local heuristics (fast)
    heuristic_risk, heuristic_reasons = check_heuristics(url)
    reasons.extend(heuristic_reasons)

    # If heuristics already say Dangerous, no need to call Google API
    if heuristic_risk == "Dangerous":
        return "Dangerous", reasons

    # Step 2: Google Safe Browsing check
    google_risk, threat_type = await check_google_safe_browsing(url)

    if google_risk == "Dangerous":
        reasons.append(f"Flagged by Google Safe Browsing: {threat_type}")
        return "Dangerous", reasons

    # Return heuristic result (could be Suspicious or Safe)
    return heuristic_risk, reasons


# -------- ROUTES -------- #

@app.get("/")
def home():
    return {"message": "Backend is running"}


# ---------- RESET PASSWORD PAGE ---------- #

@app.get("/reset-password", response_class=HTMLResponse)
async def reset_password_page(request: Request):
    html_path = os.path.join(os.path.dirname(__file__), "reset_password.html")
    with open(html_path, "r", encoding="utf-8") as f:
        return HTMLResponse(content=f.read())


@app.post("/reset-password")
def reset_password(request: ResetPasswordRequest):
    try:
        supabase.auth.set_session(request.access_token, request.access_token)
        supabase.auth.update_user({"password": request.new_password})
        return {"message": "Password updated successfully"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))


# ---------- SIGNUP ---------- #

@app.post("/signup")
def signup(request: SignupRequest):
    try:
        response = supabase.auth.sign_up({
            "email": request.email,
            "password": request.password
        })

        if response.user is None:
            raise HTTPException(status_code=400, detail="Signup failed. Email may already be in use.")

        user_id = str(response.user.id)

        supabase.table("users").insert({
            "id": user_id,
            "name": request.name,
            "email": request.email,
        }).execute()

        return {
            "message": "User created successfully",
            "name": request.name,
            "email": request.email,
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))


# ---------- LOGIN ---------- #

@app.post("/login")
def login(user: UserAuth):
    try:
        response = supabase.auth.sign_in_with_password({
            "email": user.email,
            "password": user.password
        })

        user_id = str(response.user.id) if response.user else None

        # Fetch name and plan from the users table
        name = None
        plan = "free"
        if user_id:
            result = supabase.table("users").select("name, plan").eq("id", user_id).single().execute()
            if result.data:
                name = result.data.get("name")
                plan = result.data.get("plan", "free")

        return {
            "message": "Login successful",
            "email": user.email,
            "name": name,
            "plan": plan,
            "access_token": response.session.access_token if response.session else None,
            "user_id": user_id
        }
    except Exception as e:
        raise HTTPException(status_code=401, detail=safe_error(e))


# ---------- LOGOUT ---------- #

@app.post("/logout")
def logout():
    try:
        logout_user()
        return {"message": "Logged out"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))


# ---------- FORGOT PASSWORD ---------- #

@app.post("/forgot-password")
def forgot_password(request: ForgotPasswordRequest):
    try:
        supabase.auth.reset_password_email(
            request.email,
            options={"redirect_to": "http://localhost:8000/reset-password"}
        )
        return {"message": "Password reset email sent"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))


# ---------- CHANGE PASSWORD ---------- #

@app.post("/change-password")
def change_password(request: ChangePasswordRequest):
    try:
        # Step 1: Verify the current password by signing in
        auth_response = supabase.auth.sign_in_with_password({
            "email": request.email,
            "password": request.current_password
        })

        if not auth_response.session:
            raise HTTPException(status_code=401, detail="Current password is incorrect")

        # Step 2: Update to the new password using the valid session token
        access_token  = auth_response.session.access_token
        refresh_token = auth_response.session.refresh_token

        supabase.auth.set_session(access_token, refresh_token)
        supabase.auth.update_user({"password": request.new_password})

        return {"message": "Password changed successfully"}

    except HTTPException:
        raise
    except Exception as e:
        error_msg = safe_error(e)
        # Surface a friendly message for wrong password
        if "invalid" in error_msg.lower() or "credentials" in error_msg.lower():
            raise HTTPException(status_code=401, detail="Current password is incorrect")
        raise HTTPException(status_code=400, detail=error_msg)


# ---------- URL SCAN ---------- #

@app.post("/scan")
async def scan_url(request: URLRequest):
    try:
        url = request.url.strip().replace('\u2028', ' ').replace('\u2029', ' ')

        if not url:
            raise HTTPException(status_code=400, detail="URL cannot be empty")

        # Add http:// if no scheme provided
        if not url.startswith("http://") and not url.startswith("https://"):
            url = "http://" + url

        # Run combined risk detection
        risk_level, reasons = await determine_risk(url)

        # Log reasons to terminal for debugging
        print(f"\n🔍 Scanned: {url}")
        print(f"   Risk: {risk_level}")
        for r in reasons:
            print(f"   → {r}")

        # Save to Supabase scan_history
        supabase.table("scan_history").insert({
            "url": url,
            "risk_level": risk_level
        }).execute()

        return {
            "url": url,
            "risk_level": risk_level,
            "reasons": reasons
        }

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))