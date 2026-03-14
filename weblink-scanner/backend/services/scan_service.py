# WeblinkScanner\backend\services\scan_service.py
# URL threat detection logic used by scan endpoints

import re
import httpx
from config import settings

# Keywords commonly found in phishing URLs
SUSPICIOUS_KEYWORDS = ["login", "verify",
                       "bank", "account",
                       "secure", "password",
                       "update", "confirm",
                       ]

# --------------------------------------------
# Basic heuristic checks
# --------------------------------------------
def basic_heuristic_check(url: str):

    score = 0
    categories = []
    url_lower = url.lower()

    # Rule check 1: No HTTPS
    if url_lower.startswith("http://"):
        score += 20
        categories.append("No HTTPS")

    # Rule check 2: Suspicious keyword
    for word in SUSPICIOUS_KEYWORDS:
        if word in url_lower:
            score += 10
            categories.append("Suspicious keyword")
            break

    # Rule check 3: Long URL
    if len(url) > 120:
        score += 10
        categories.append("Long URL")

    # Rule check 4: IP address URL
    if re.search(r"https?://\d{1,3}(\.\d{1,3}){3}", url):
        score += 30
        categories.append("IP address URL")

    return score, categories


# --------------------------------------------
# Google Safe Browsing check
# --------------------------------------------
async def check_google_safe_browsing(url: str):

    # Skip if API key not configured
    if not settings.GOOGLE_SAFE_BROWSING_API_KEY:
        return None

    payload = {
        "client": {
            "clientId": "weblinkscanner",
            "clientVersion": "1.0"
        },
        "threatInfo": {
            "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING"],
            "platformTypes": ["ANY_PLATFORM"],
            "threatEntryTypes": ["URL"],
            "threatEntries": [{"url": url}],
        },
    }

    try:
        async with httpx.AsyncClient() as client:

            response = await client.post(
                f"https://safebrowsing.googleapis.com/v4/threatMatches:find"
                f"?key={settings.GOOGLE_SAFE_BROWSING_API_KEY}",
                json=payload,
                timeout=10
            )

        if response.status_code == 200:
            data = response.json()
            return "matches" in data

    except Exception:
        return None

    return None


# --------------------------------------------
# Main scan function used by API
# --------------------------------------------
async def perform_scan(url: str):

    score, categories = basic_heuristic_check(url)

    # Google Safe Browsing check
    gsb = await check_google_safe_browsing(url)

    if gsb is True:
        score += 50
        categories.append("Google Safe Browsing threat")

    result = {"verdict": "SAFE",
              "risk_score": 0,
              "reason_1": None,
              "reason_2": None,
              "reason_3": None,
              "blacklist_match": False,
              "threat_categories": [],
              }

    # Determine risk level
    if score >= 70:
        result["verdict"] = "DANGEROUS"
    elif score >= 30:
        result["verdict"] = "SUSPICIOUS"
    else:
        result["verdict"] = "SAFE"

    # Store risk score
    result["risk_score"] = score

    # Save threat categories
    result["threat_categories"] = categories

    # fill reasons
    if len(categories) > 0:
        result["reason_1"] = categories[0]

    if len(categories) > 1:
        result["reason_2"] = categories[1]

    if len(categories) > 2:
        result["reason_3"] = categories[2]

    return result


