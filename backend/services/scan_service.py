# scan_service.py
#
# DECISION FRAMEWORK
# ------------------
# Layer 1 - Live threat feeds (URLhaus + PhishTank)
#            Run first, always. If either flags the URL -> DANGEROUS, stop.
#            These are authoritative external databases - they override everything.
#
# Layer 2 - Heuristic engine (structural URL analysis)
#            Runs when Layer 1 is clean.
#            Score >= 40 -> DANGEROUS, >= 20 -> SUSPICIOUS, < 20 -> SAFE
#            If verdict is DANGEROUS, stop - skip the trusted domain check.
#
# Layer 3 - Trusted domain check (Cisco Umbrella top-1M, refreshed daily)
#            Runs ONLY when verdict so far is SAFE or SUSPICIOUS.
#            If the registrable domain is in the Umbrella top-50k AND uses HTTPS
#            → override to SAFE (suppresses heuristic false positives).
#            A DANGEROUS verdict from Layer 2 is NEVER overridden here.
#
# KEY INVARIANT: URLhaus/PhishTank always run first and always win.
#                Trusted domain status cannot rescue a confirmed malware URL.

import re
import io
import zipfile
import httpx
import asyncio
import time
from urllib.parse import urlparse, parse_qs
from config import settings

# --- Cache TTLs ---
CACHE_TTL = 3600     # 1 hour  - keywords, TLDs
UMBRELLA_CACHE_TTL = 86400    # 24 hours - Umbrella top-1M (published daily)
UMBRELLA_TOP_N = 50_000   # keep only the top 50k from the full 1M list

# --- Runtime caches ---
keyword_cache: list  = []
keyword_cache_ts: float = 0.0

tld_cache: list  = []
tld_cache_ts: float = 0.0

umbrella_cache: set = set()
umbrella_cache_ts: float = 0.0

# --- Fallback seeds ---
# Used ONLY when live fetches are completely unreachable.

KEYWORD_SEED = [
    "login", "verify", "bank", "account", "secure", "password",
    "update", "confirm", "signin", "wallet", "invoice", "payment",
    "ebayisapi", "webscr", "paypal", "authenticate", "recover",
    "billing", "helpdesk", "unlock", "credential", "suspended",
    "verification", "unusual-activity",
]

TLD_SEED = [
    ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".click",
    ".download", ".loan", ".win", ".racing", ".party", ".date",
    ".icu", ".cyou", ".buzz", ".cfd", ".monster", ".stream",
]

# Critical domains kept as last-resort fallback if Umbrella download fails.
TRUSTED_SEED = {
    "google.com", "googleapis.com", "youtube.com",
    "microsoft.com", "microsoftonline.com", "live.com",
    "apple.com", "icloud.com",
    "amazon.com", "amazonaws.com",
    "github.com", "githubusercontent.com",
    "cloudflare.com", "cloudfront.net",
    "facebook.com", "instagram.com",
    "gov.sg",
    "skillbuilder.aws", "console.aws",
}

# Brand TLDs and ccSLDs for registrable-domain extraction
BRAND_TLDS = {
    "aws", "google", "apple", "microsoft", "github",
    "youtube", "facebook", "instagram", "twitter",
    "linkedin", "netflix", "adobe", "oracle", "ibm",
}
CCSLDS = {
    "co.uk", "com.au", "co.nz", "co.in", "com.sg",
    "co.za", "com.br", "co.jp", "org.uk", "net.au",
}


# --- Domain helpers ---

def get_registrable_domain(domain: str) -> str:
    """
    Extracts the registrable (apex) domain from a hostname.
    Handles brand TLDs (.aws, .google …) and common ccSLDs (.co.uk …).
    """
    parts = domain.lower().split(".")
    if len(parts) < 2:
        return domain
    tld = parts[-1]
    if tld in BRAND_TLDS:
        return ".".join(parts[-2:])
    if len(parts) >= 3 and ".".join(parts[-2:]) in CCSLDS:
        return ".".join(parts[-3:])
    return ".".join(parts[-2:])


# --- Umbrella trusted domain list ---

async def fetch_trusted_domains() -> set:
    """
    Downloads the Cisco Umbrella top-1M CSV (free, no API key, updated daily).
    Keeps the top UMBRELLA_TOP_N entries as registrable domains.
    Cached for 24 hours. Falls back to TRUSTED_SEED if unreachable.

    Source: http://s3-us-west-1.amazonaws.com/umbrella-static/top-1m.csv.zip
    Format: rank,domain  (e.g.  1,google.com)
    """
    global umbrella_cache, umbrella_cache_ts
    now = time.time()
    if umbrella_cache and (now - umbrella_cache_ts) < UMBRELLA_CACHE_TTL:
        return umbrella_cache
    try:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(
                "http://s3-us-west-1.amazonaws.com/umbrella-static/top-1m.csv.zip"
            )
        if resp.status_code == 200:
            with zipfile.ZipFile(io.BytesIO(resp.content)) as zf:
                with zf.open(zf.namelist()[0]) as f:
                    domains: set = set()
                    for i, line in enumerate(f):
                        if i >= UMBRELLA_TOP_N:
                            break
                        parts = line.decode("utf-8", errors="ignore").strip().split(",")
                        if len(parts) >= 2:
                            domains.add(get_registrable_domain(parts[1].strip().lower()))
            if domains:
                umbrella_cache    = domains | TRUSTED_SEED
                umbrella_cache_ts = now
                print(f"[Umbrella] Loaded {len(umbrella_cache)} trusted domains", flush=True)
                return umbrella_cache
    except Exception as e:
        print(f"[Umbrella] Fetch failed: {e}", flush=True)
    return umbrella_cache if umbrella_cache else TRUSTED_SEED


async def is_trusted(url: str) -> bool:
    """
    Returns True when the URL uses HTTPS AND its registrable domain is in
    the Cisco Umbrella top-50k list.
    HTTP URLs are never trusted - even on well-known domains.

    NOTE: This function is async (it awaits fetch_trusted_domains).
    """
    if not url.lower().startswith("https://"):
        return False
    parsed      = urlparse(url)
    domain      = parsed.netloc.lower().split(":")[0]
    registrable = get_registrable_domain(domain)
    trusted_set = await fetch_trusted_domains()
    return registrable in trusted_set


# --- Live keyword and TLD fetchers ---

async def fetch_keywords() -> list:
    """
    Fetches live phishing/malware keyword tags from URLhaus (abuse.ch).
    Refreshed every hour. Falls back to KEYWORD_SEED if unreachable.
    Source: https://urlhaus-api.abuse.ch/v1/
    """
    global keyword_cache, keyword_cache_ts
    now = time.time()
    if keyword_cache and (now - keyword_cache_ts) < CACHE_TTL:
        return keyword_cache
    try:
        async with httpx.AsyncClient(timeout=8) as client:
            resp = await client.post(
                "https://urlhaus-api.abuse.ch/v1/tags/",
                data={"query": "get_tags"},
            )
        if resp.status_code == 200:
            tags = resp.json().get("tags", [])
            keywords = [
                t.lower() for t in tags
                if isinstance(t, str) and len(t) >= 3
                and not t.lower().endswith(
                    (".exe", ".dll", ".bat", ".ps1", ".jar", ".zip",
                     ".doc", ".xls", ".pdf", ".vbs", ".sh")
                )
            ]
            if keywords:
                keyword_cache = keywords
                keyword_cache_ts = now
                return keywords
    except Exception as e:
        print(f"[URLhaus keywords] Fetch failed: {e}", flush=True)
    return keyword_cache if keyword_cache else KEYWORD_SEED


async def fetch_suspicious_tlds() -> list:
    """
    Fetches the Spamhaus most-abused TLD list.
    Refreshed every hour. Falls back to TLD_SEED if unreachable.
    Source: https://www.spamhaus.org/statistics/tlds/
    """
    global tld_cache, tld_cache_ts
    now = time.time()
    if tld_cache and (now - tld_cache_ts) < CACHE_TTL:
        return tld_cache
    try:
        async with httpx.AsyncClient(timeout=8) as client:
            resp = await client.get(
                "https://www.spamhaus.org/statistics/tlds/",
                headers={"Accept": "text/html"},
            )
        if resp.status_code == 200:
            found = re.findall(r'\.([\w]{2,})', resp.text)
            tlds  = list({
                "." + t.lower() for t in found
                if t.isalpha() and 2 <= len(t) <= 8
            })
            if tlds:
                tld_cache = tlds
                tld_cache_ts = now
                return tlds
    except Exception as e:
        print(f"[Spamhaus TLDs] Fetch failed: {e}", flush=True)
    return tld_cache if tld_cache else TLD_SEED


# --- Layer 1: URLhaus ---

async def check_urlhaus(url: str):
    """
    Checks URLhaus for both the exact URL and the host.

    Two separate lookups run concurrently:
      /url/  - exact URL match (catches specific malware distribution paths)
      /host/ - host-level match (catches all malware hosted on a domain)

    Status values that indicate a threat:
      URL  lookup: "is_active" (currently distributing) OR "was_active" (confirmed past)
      HOST lookup: "is_active" OR "was_active" (host was confirmed malware server)

    Returns a threat description string, or None if clean or API unreachable.
    When the API is unreachable, None is returned - the caller must NOT treat
    this as confirmation of safety. Heuristics will still run as a fallback.

    Source: https://urlhaus-api.abuse.ch/v1/
    """
    parsed = urlparse(url if "://" in url else "https://" + url)
    host   = parsed.netloc or parsed.path

    async def lookup_url(client: httpx.AsyncClient, target: str):
        try:
            resp = await client.post(
                "https://urlhaus-api.abuse.ch/v1/url/",
                data={"url": target},
                timeout=10,
            )
            if resp.status_code == 200:
                data   = resp.json()
                status = data.get("query_status", "")
                print(f"[URLhaus /url/] status={status!r} for {target[:60]}", flush=True)
                # was_active = confirmed malware in the past; treat same as active
                if status in ("is_active", "was_active"):
                    threat = data.get("threat", "malware")
                    tags   = data.get("tags") or []
                    detail = threat + (f" [{', '.join(tags)}]" if tags else "")
                    return detail
        except Exception as e:
            print(f"[URLhaus /url/] Error: {e}", flush=True)
        return None

    async def lookup_host(client: httpx.AsyncClient, h: str):
        try:
            resp = await client.post(
                "https://urlhaus-api.abuse.ch/v1/host/",
                data={"host": h},
                timeout=10,
            )
            if resp.status_code == 200:
                data   = resp.json()
                status = data.get("query_status", "")
                print(f"[URLhaus /host/] status={status!r} for {h}", flush=True)
                # Accept both is_active and was_active for hosts too
                if status in ("is_active", "was_active"):
                    n = data.get("urls_count", 0)
                    return f"malware host ({n} malicious URL(s) on record)"
        except Exception as e:
            print(f"[URLhaus /host/] Error: {e}", flush=True)
        return None

    try:
        async with httpx.AsyncClient(timeout=12) as client:
            url_result, host_result = await asyncio.gather(
                lookup_url(client, url),
                lookup_host(client, host),
            )
        if url_result:
            return f"URLhaus (exact URL): {url_result}"
        if host_result:
            return f"URLhaus (malicious host): {host_result}"
    except Exception as e:
        print(f"[URLhaus] Outer error: {e}", flush=True)

    return None


# --- Layer 1: PhishTank ---

async def check_phishtank(url: str) -> bool:
    """
    Checks PhishTank for confirmed phishing pages.
    Source: https://www.phishtank.com/developer_info.php
    """
    try:
        async with httpx.AsyncClient(timeout=8) as client:
            resp = await client.post(
                "https://checkurl.phishtank.com/checkurl/",
                data={
                    "url": url, "format": "json",
                    "app_key": settings.PHISHTANK_API_KEY,
                },
                headers={"User-Agent": "phishtank/WeblinkScanner"},
            )
        if resp.status_code == 200:
            results = resp.json().get("results", {})
            if results.get("in_database") and results.get("valid"):
                return True
    except Exception as e:
        print(f"[PhishTank] Error: {e}", flush=True)
    return False


# --- Layer 2: Heuristic engine ---

async def heuristic_check(url: str) -> tuple:
    """
    Structural URL analysis. All reference data comes from live external sources.
    Returns (score: int, categories: list[str])
    Thresholds: >= 40 DANGEROUS  |  >= 20 SUSPICIOUS  |  < 20 SAFE
    """
    score      = 0
    categories = []
    url_lower  = url.lower()

    parsed       = urlparse(url if "://" in url else "http://" + url)
    domain       = parsed.netloc.lower()
    clean_domain = domain.replace("www.", "")
    query_str    = parsed.query

    keywords, tlds = await asyncio.gather(fetch_keywords(), fetch_suspicious_tlds())

    # Signal 1: No HTTPS
    if url_lower.startswith("http://"):
        score += 20
        categories.append("No HTTPS - connection is unencrypted")

    # Signal 2: Phishing keyword in URL
    matched = [kw for kw in keywords if kw in url_lower]
    if matched:
        score += 15
        categories.append(
            f"Phishing-related keyword in URL: '{matched[0]}'"
            " (source: URLhaus live tags)"
        )

    # Signal 3: Unusually long URL (> 100 chars)
    if len(url) > 100:
        score += 10
        categories.append("Unusually long URL (common obfuscation technique)")

    # Signal 4: Excessive subdomains (>= 4 domain parts)
    parts = domain.split(".")
    if len(parts) >= 4:
        score += 15
        categories.append(
            f"Excessive subdomains ({len(parts)} levels) - "
            "attackers use this to bury the real domain"
        )

    # Signal 5: IP address used as host
    if re.search(r"https?://\d{1,3}(\.\d{1,3}){3}", url):
        score += 35
        categories.append(
            "IP address used instead of a domain name - "
            "strong indicator of malicious hosting"
        )

    # Signal 6: High-risk TLD (Spamhaus list)
    for tld in tlds:
        if clean_domain.endswith(tld) or f"{tld}/" in url_lower or f"{tld}?" in url_lower:
            score += 20
            categories.append(
                f"High-risk domain extension ({tld}) - "
                "consistently abused per Spamhaus TLD statistics"
            )
            break

    # Signal 7: @ symbol in URL
    if "@" in url:
        score += 25
        categories.append(
            "@ symbol in URL - browser redirects to the address after @, "
            "ignoring everything before it"
        )

    # Signal 8: Multiple hyphens in domain (>= 3)
    if clean_domain.count("-") >= 3:
        score += 20
        categories.append(
            f"Multiple hyphens in domain ({clean_domain.count('-')}) - "
            "common in phishing domain names"
        )

    # Signal 9: Percent-encoded characters in URL
    if re.search(r"%[0-9a-fA-F]{2}", url):
        score += 10
        categories.append(
            "Percent-encoded characters in URL - "
            "used to evade keyword-based filters"
        )

    # Signal 10: Punycode / IDN homograph attack
    if "xn--" in url_lower:
        score += 25
        categories.append(
            "Internationalised domain (xn-- prefix) - "
            "may be a homograph attack using lookalike characters"
        )

    # Signal 11: Double file extension in path
    if re.search(r"\.(pdf|doc|xls|jpg|png)\.(exe|js|php|html|zip)", url_lower):
        score += 30
        categories.append(
            "Double file extension in URL path - "
            "commonly used to disguise malicious file types"
        )

    # Signal 12: Free subdomain hosting services
    FREE_SUBDOMAIN_SERVICES = [
        ".in.net", ".ddns.net", ".hopto.org", ".zapto.org", ".sytes.net",
        ".duckdns.org", ".afraid.org", ".mooo.com", ".noip.me",
        ".servebeer.com", ".serveblog.net", ".servecounterstrike.com",
        ".serveftp.com", ".servegame.com", ".servehttp.com",
        ".serveirc.com", ".serveminecraft.net", ".servemp3.com",
        ".servepics.com", ".servequake.com", ".linkpc.net",
        ".publicvm.com", ".onthewifi.com", ".redirectme.net",
        ".webhop.me", ".gotdns.ch", ".myftp.biz", ".myftp.org",
        ".myvnc.com", ".dynamic-dns.net", ".changeip.com",
    ]
    for svc in FREE_SUBDOMAIN_SERVICES:
        if clean_domain.endswith(svc):
            score += 25
            categories.append(
                f"Free dynamic subdomain service ({svc}) - "
                "commonly abused by attackers to avoid registering real domains"
            )
            break

    # Signal 13: UUID or long random hex segment in path
    if re.search(
        r"/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
        url_lower
    ) or re.search(r"/[0-9a-f]{20,}", url_lower):
        score += 20
        categories.append(
            "UUID or random hex token in URL path - "
            "phishing campaigns use unique per-victim URLs to evade blacklists"
        )

    # Signal 14: SQL injection patterns in query parameters
    SQL_PATTERNS = [
        r"(union\s+select|select\s+.*\s+from|insert\s+into|drop\s+table"
        r"|update\s+.*\s+set|delete\s+from|exec\s*\(|execute\s*\()",
        r"(--|;--|\/\*|\*\/|xp_|sp_)",
        r"('\s*(or|and)\s*'?\d|'\s*or\s*'1'\s*=\s*'1)",
        r"(benchmark\s*\(|sleep\s*\(|waitfor\s+delay)",
    ]
    query_decoded = query_str.replace("%27", "'").replace("%20", " ").replace("+", " ")
    for pattern in SQL_PATTERNS:
        if re.search(pattern, query_decoded, re.IGNORECASE):
            score += 40
            categories.append(
                "SQL injection pattern detected in URL parameters - "
                "typical of automated attack probes or crafted exploit URLs"
            )
            break

    # Signal 15: XSS patterns in query parameters
    XSS_PATTERNS = [
        r"<\s*script",
        r"javascript\s*:",
        r"on(error|load|click|mouseover|focus)\s*=",
        r"(alert|confirm|prompt)\s*\(",
        r"(%3c|%3e|%22|%27).*(%3c|%3e|%22|%27)",
        r"&#\d+;",
    ]
    for pattern in XSS_PATTERNS:
        if re.search(pattern, query_decoded, re.IGNORECASE):
            score += 40
            categories.append(
                "Cross-site scripting (XSS) pattern detected in URL parameters - "
                "indicates a crafted attack payload targeting browser script execution"
            )
            break

    # Signal 16: Direct binary or archive file download in URL path
    # Three tiers of suspicion:
    #   Tier A (+35): Executable files (.exe, .msi, .bat …) anywhere in path
    #   Tier B (+40): Archive files in a known download/release subpath
    #   Tier C (+30): Archive files anywhere in the path (catches CDN-hosted zips
    #                 like /leuxtrogxre_x64.zip on oss/cloud storage hosts)
    # Tier A is scored higher than Tier B because executables are more directly
    # dangerous. Tier B is highest because a release-path archive is almost
    # certainly a deliberate software delivery point, not an incidental file.
    BINARY_EXTENSIONS   = r"\.(exe|msi|bat|ps1|vbs|jar|apk|dmg|pkg|deb|rpm|iso|img|bin)$"
    ARCHIVE_IN_DOWNLOAD = r"/(download|releases|dist|raw|drop|payload|setup|install)/.+\.(zip|rar|7z|tar|gz|tgz)$"
    ARCHIVE_ANYWHERE    = r"\.(zip|rar|7z|tar\.gz|tgz|tar)$"
    path_lower = parsed.path.lower()
    if re.search(BINARY_EXTENSIONS, path_lower):
        score += 35
        categories.append(
            f"Direct executable file in URL path ({path_lower.rsplit('.',1)[-1].upper()}) - "
            "executable files served directly via URL are a primary malware delivery method"
        )
    elif re.search(ARCHIVE_IN_DOWNLOAD, path_lower):
        score += 40
        categories.append(
            "Archive file (.zip/.rar etc.) served from a download/release path - "
            "a common malware delivery method; treat with caution"
        )
    elif re.search(ARCHIVE_ANYWHERE, path_lower):
        score += 30
        categories.append(
            "Archive file directly served via URL - "
            "cloud storage and CDN-hosted archives are frequently used for malware distribution"
        )

    return min(score, 100), categories


# --- Main scan entry point ---

async def perform_scan(url: str) -> dict:
    """
    Layer 1: URLhaus + PhishTank - run first, always, results are final.
    Layer 2: Heuristics - run when Layer 1 is clean.
             If heuristics → DANGEROUS, stop. Trusted domain cannot save it.
    Layer 3: Trusted domain check - run only when verdict is SAFE or SUSPICIOUS.
             If trusted, override to SAFE (suppresses heuristic false positives).
             Never runs when verdict is already DANGEROUS.
    """

    # --- Layer 1 ---
    urlhaus_result, phishtank_flagged = await asyncio.gather(
        check_urlhaus(url),
        check_phishtank(url),
    )

    if urlhaus_result:
        return build_result(
            verdict="DANGEROUS",
            score=100,
            categories=[
                f"Confirmed malicious URL - {urlhaus_result}",
                "URLhaus (abuse.ch) maintains a real-time database of active "
                "malware distribution URLs, updated by security researchers.",
            ],
            blacklist_match=True,
        )

    if phishtank_flagged:
        return build_result(
            verdict="DANGEROUS",
            score=100,
            categories=[
                "Confirmed phishing page - PhishTank database",
                "PhishTank (Anti-Phishing Working Group) contains "
                "community-verified phishing URLs confirmed by analysts.",
            ],
            blacklist_match=True,
        )

    # --- Layer 2: Heuristics ---
    score, categories = await heuristic_check(url)

    if score >= 40:
        heuristic_verdict = "DANGEROUS"

        # DANGEROUS from heuristics - do NOT check trusted domain.
        # A high-scoring malicious URL is not made safe by appearing on a
        # popular hosting platform.

        return build_result(
            verdict="DANGEROUS",
            score=score,
            categories=categories,
            blacklist_match=False,
        )

    if score >= 20:
        heuristic_verdict = "SUSPICIOUS"
    else:
        heuristic_verdict = "SAFE"

    if not categories and heuristic_verdict == "SAFE":
        categories = ["No suspicious signals detected across all checks"]


    # --- Layer 3: Trusted domain check ---
    # Only runs when verdict so far is SAFE or SUSPICIOUS.
    # If trusted -> return SAFE (suppresses false positives for legitimate
    # auth/redirect flows on well-known platforms).

    if heuristic_verdict != "DANGEROUS":
        if await is_trusted(url):
            return build_result(
                verdict="SAFE",
                score=0,
                categories=["Verified trusted domain (Umbrella Top-50k)"],
                blacklist_match=False,
            )

    return build_result(
        verdict=heuristic_verdict,
        score=score,
        categories=categories,
        blacklist_match=False,
    )


def build_result(verdict: str, score: int, categories: list, blacklist_match: bool) -> dict:
    # Suppress heuristic signal details for SAFE - minor signals are noise.
    shown = categories if verdict != "SAFE" else []
    return {
        "verdict":           verdict,
        "risk_score":        score,
        "reason_1":          shown[0] if len(shown) > 0 else None,
        "reason_2":          shown[1] if len(shown) > 1 else None,
        "reason_3":          shown[2] if len(shown) > 2 else None,
        "blacklist_match":   blacklist_match,
        "threat_categories": shown,
    }
