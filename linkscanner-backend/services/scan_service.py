# services/scan_service.py

import re
import httpx
from config import settings
from database import supabase

SUSPICIOUS_KEYWORDS = [
    "login", "verify", "bank", "account", "secure", "password",
    "update", "confirm", "signin", "wallet", "invoice", "payment",
    "ebayisapi", "webscr", "cmd=_", "paypal", "authenticate",
    "recover", "billing", "support", "helpdesk", "unlock",
]

SUSPICIOUS_TLDS = [
    ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".click",
    ".download", ".loan", ".win", ".racing", ".party", ".date",
]


def basic_heuristic_check(url: str):
    score      = 0
    categories = []
    url_lower  = url.lower()

    # No HTTPS — only penalise if explicitly http://
    # If no protocol, we don't know yet — sandbox will detect it
    if url_lower.startswith("http://"):
        score += 20
        categories.append("No HTTPS")

    # Suspicious keyword
    for word in SUSPICIOUS_KEYWORDS:
        if word in url_lower:
            score += 15
            categories.append(f"Suspicious keyword: {word}")
            break

    # Long URL
    if len(url) > 100:
        score += 10
        categories.append("Unusually long URL")

    # Excessive subdomains
    try:
        from urllib.parse import urlparse
        parsed = urlparse(url if "://" in url else "http://" + url)
        parts  = parsed.netloc.split(".")
        if len(parts) > 4:
            score += 15
            categories.append("Excessive subdomains")
    except Exception:
        pass

    # IP address URL
    if re.search(r"https?://\d{1,3}(\.\d{1,3}){3}", url):
        score += 35
        categories.append("IP address used instead of domain")

    # Suspicious TLD
    for tld in SUSPICIOUS_TLDS:
        if url_lower.endswith(tld) or f"{tld}/" in url_lower:
            score += 20
            categories.append(f"Suspicious domain extension ({tld})")
            break

    # @ symbol in URL
    if "@" in url:
        score += 25
        categories.append("@ symbol in URL (possible redirect trick)")

    # Multiple hyphens in domain
    try:
        from urllib.parse import urlparse
        domain = urlparse(url if "://" in url else "http://" + url).netloc
        if domain.count("-") >= 3:
            score += 15
            categories.append("Multiple hyphens in domain name")
    except Exception:
        pass

    # Hex encoding
    if re.search(r"%[0-9a-fA-F]{2}", url):
        score += 10
        categories.append("Encoded characters in URL")

    return score, categories


def check_blacklist(url: str):
    try:
        from urllib.parse import urlparse
        parsed = urlparse(url if "://" in url else "http://" + url)
        domain = parsed.netloc.lower().replace("www.", "")

        result = supabase.table("black_list") \
            .select("url, type") \
            .eq("url", url) \
            .execute()
        if result.data:
            return result.data[0].get("type", "Blacklisted URL")

        result2 = supabase.table("black_list") \
            .select("url, type") \
            .eq("url", domain) \
            .execute()
        if result2.data:
            return result2.data[0].get("type", "Blacklisted domain")

    except Exception:
        pass
    return None


async def check_google_safe_browsing(url: str):
    if not settings.GOOGLE_SAFE_BROWSING_API_KEY:
        return None

    payload = {
        "client": {"clientId": "linkscanner", "clientVersion": "1.0"},
        "threatInfo": {
            "threatTypes":      ["MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"],
            "platformTypes":    ["ANY_PLATFORM"],
            "threatEntryTypes": ["URL"],
            "threatEntries":    [{"url": url}],
        },
    }
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"https://safebrowsing.googleapis.com/v4/threatMatches:find"
                f"?key={settings.GOOGLE_SAFE_BROWSING_API_KEY}",
                json=payload, timeout=10
            )
        if response.status_code == 200:
            data = response.json()
            return "matches" in data and bool(data["matches"])
    except Exception:
        pass
    return None


async def perform_scan(url: str):
    # 1. Blacklist check
    blacklist_type = check_blacklist(url)
    if blacklist_type:
        return {
            "verdict":           "DANGEROUS",
            "risk_score":        100,
            "reason_1":          f"URL found in blacklist: {blacklist_type}",
            "reason_2":          None,
            "reason_3":          None,
            "blacklist_match":   True,
            "threat_categories": [f"Blacklisted: {blacklist_type}"],
        }

    # 2. Heuristic checks
    score, categories = basic_heuristic_check(url)

    # 3. Google Safe Browsing
    gsb = await check_google_safe_browsing(url)
    if gsb is True:
        score += 50
        categories.append("Flagged by Google Safe Browsing")

    # 4. Verdict
    if score >= 50:
        verdict = "DANGEROUS"
    elif score >= 20:
        verdict = "SUSPICIOUS"
    else:
        verdict = "SAFE"

    return {
        "verdict":           verdict,
        "risk_score":        score,
        "reason_1":          categories[0] if len(categories) > 0 else None,
        "reason_2":          categories[1] if len(categories) > 1 else None,
        "reason_3":          categories[2] if len(categories) > 2 else None,
        "blacklist_match":   False,
        "threat_categories": categories,
    }
