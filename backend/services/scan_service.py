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

# live-sourced pattern caches (refreshed every 6 hours)
PATTERN_CACHE_TTL = 21600   # 6 hours - OWASP CRS releases infrequently

sqli_patterns_cache: list = []
sqli_patterns_cache_ts: float = 0.0

xss_patterns_cache: list = []
xss_patterns_cache_ts: float = 0.0

double_ext_cache: list = []
double_ext_cache_ts: float = 0.0

# Free subdomain services: sourced from URLhaus domain feed + seed fallback
# URLhaus feed updated every 5 minutes: https://urlhaus.abuse.ch/downloads/text_online/
free_subdomain_cache: list = []
free_subdomain_cache_ts: float = 0.0

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

# Fallback SQLi patterns. subset of OWASP CRS REQUEST-942 rule set.
# Used only when the live OWASP CRS fetch is unreachable.
# Source (live): https://raw.githubusercontent.com/coreruleset/coreruleset/main/rules/REQUEST-942-APPLICATION-ATTACK-SQLI.conf
SQLI_SEED = [
    r"(union\s+select|select\s+.+\s+from|insert\s+into|drop\s+table"
    r"|update\s+.+\s+set|delete\s+from|exec\s*\(|execute\s*\()",
    r"(--|;--|\/\*|\*\/|xp_|sp_)",
    r"('\s*(or|and)\s*'?\d|'\s*or\s*'1'\s*=\s*'1)",
    r"(benchmark\s*\(|sleep\s*\(|waitfor\s+delay)",
]

# Fallback XSS patterns. subset of OWASP CRS REQUEST-941 rule set.
# Source (live): https://raw.githubusercontent.com/coreruleset/coreruleset/main/rules/REQUEST-941-APPLICATION-ATTACK-XSS.conf
XSS_SEED = [
    r"<\s*script",
    r"javascript\s*:",
    r"on(error|load|click|mouseover|focus|keydown|keyup|submit|change)\s*=",
    r"(alert|confirm|prompt|eval|document\.cookie)\s*\(",
    r"(%3c|%3e|%22|%27).*(%3c|%3e|%22|%27)",
    r"&#\d+;",
    r"<\s*(iframe|object|embed|svg|math|video|audio|base)",
]

# Fallback double extension pairs (sourced from OWASP CRS and MITRE ATT&CK T1036).
# Source (live): OWASP CRS REQUEST-932-APPLICATION-ATTACK-RCE.conf
DOUBLE_EXT_SEED = [
    (r"pdf", r"exe"), (r"doc", r"exe"), (r"xls", r"exe"), (r"jpg", r"exe"),
    (r"png", r"exe"), (r"pdf", r"js"),  (r"doc", r"js"),  (r"pdf", r"vbs"),
    (r"pdf", r"bat"), (r"pdf", r"ps1"), (r"pdf", r"php"), (r"pdf", r"html"),
    (r"doc", r"zip"), (r"jpg", r"php"), (r"txt", r"exe"), (r"gif", r"exe"),
]

# Fallback free subdomain service suffixes.
# Source (live): URLhaus malware domain feed - https://urlhaus.abuse.ch/downloads/text_online/
# The live feed gives us actual abused domains; these seeds cover well-known DDNS providers.
FREE_SUBDOMAIN_SEED = [
    ".ddns.net", ".hopto.org", ".zapto.org", ".sytes.net", ".duckdns.org",
    ".afraid.org", ".mooo.com", ".noip.me", ".servebeer.com", ".serveblog.net",
    ".servecounterstrike.com", ".serveftp.com", ".servegame.com", ".servehttp.com",
    ".serveirc.com", ".serveminecraft.net", ".servemp3.com", ".servepics.com",
    ".servequake.com", ".linkpc.net", ".publicvm.com", ".onthewifi.com",
    ".redirectme.net", ".webhop.me", ".gotdns.ch", ".myftp.biz", ".myftp.org",
    ".myvnc.com", ".dynamic-dns.net", ".changeip.com", ".in.net",
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
                umbrella_cache = domains | TRUSTED_SEED
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
    parsed = urlparse(url)
    domain = parsed.netloc.lower().split(":")[0]
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
            tlds = list({
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


# --- Live pattern fetchers (OWASP CRS + URLhaus domain feed) ---

async def fetch_sqli_patterns() -> list:
    """
    Fetches SQL injection regex patterns from the OWASP Core Rule Set (CRS).
    The CRS is the industry-standard WAF ruleset maintained by OWASP.
    Source: https://raw.githubusercontent.com/coreruleset/coreruleset/main/rules/REQUEST-942-APPLICATION-ATTACK-SQLI.conf
    Falls back to SQLI_SEED if unreachable.
    """
    global sqli_patterns_cache, sqli_patterns_cache_ts
    now = time.time()
    if sqli_patterns_cache and (now - sqli_patterns_cache_ts) < PATTERN_CACHE_TTL:
        return sqli_patterns_cache
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                "https://raw.githubusercontent.com/coreruleset/coreruleset/main"
                "/rules/REQUEST-942-APPLICATION-ATTACK-SQLI.conf",
                headers={"User-Agent": "WeblinkScanner/1.0"},
            )
        if resp.status_code == 200:
            # Extract @rx patterns from SecRule directives
            patterns = re.findall(r'@rx\s+([^\n\"]+)', resp.text)
            # Keep only patterns that mention SQL-specific terms to avoid false positives
            sql_terms = {"select", "union", "insert", "delete", "update", "drop",
                         "exec", "benchmark", "sleep", "waitfor", "xp_", "sp_"}
            filtered = [
                p.strip() for p in patterns
                if any(t in p.lower() for t in sql_terms) and 5 < len(p) < 300
            ]
            if filtered:
                sqli_patterns_cache = filtered
                sqli_patterns_cache_ts = now
                print(f"[OWASP CRS SQLi] Loaded {len(filtered)} patterns", flush=True)
                return filtered
    except Exception as e:
        print(f"[OWASP CRS SQLi] Fetch failed: {e}", flush=True)
    return sqli_patterns_cache if sqli_patterns_cache else SQLI_SEED


async def fetch_xss_patterns() -> list:
    """
    Fetches XSS detection regex patterns from the OWASP Core Rule Set (CRS).
    Source: https://raw.githubusercontent.com/coreruleset/coreruleset/main/rules/REQUEST-941-APPLICATION-ATTACK-XSS.conf
    Falls back to XSS_SEED if unreachable.
    """
    global xss_patterns_cache, xss_patterns_cache_ts
    now = time.time()
    if xss_patterns_cache and (now - xss_patterns_cache_ts) < PATTERN_CACHE_TTL:
        return xss_patterns_cache
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                "https://raw.githubusercontent.com/coreruleset/coreruleset/main"
                "/rules/REQUEST-941-APPLICATION-ATTACK-XSS.conf",
                headers={"User-Agent": "WeblinkScanner/1.0"},
            )
        if resp.status_code == 200:
            patterns = re.findall(r'@rx\s+([^\n\"]+)', resp.text)
            xss_terms = {"script", "javascript", "onerror", "onload", "onclick",
                         "alert", "iframe", "svg", "eval", "document"}
            filtered = [
                p.strip() for p in patterns
                if any(t in p.lower() for t in xss_terms) and 5 < len(p) < 300
            ]
            if filtered:
                xss_patterns_cache = filtered
                xss_patterns_cache_ts = now
                print(f"[OWASP CRS XSS] Loaded {len(filtered)} patterns", flush=True)
                return filtered
    except Exception as e:
        print(f"[OWASP CRS XSS] Fetch failed: {e}", flush=True)
    return xss_patterns_cache if xss_patterns_cache else XSS_SEED


async def fetch_double_ext_pairs() -> list:
    """
    Fetches double-extension masking pairs from the OWASP CRS RCE ruleset.
    Covers filenames like report.pdf.exe - a classic malware delivery technique
    documented in MITRE ATT&CK T1036.007 (Masquerading: Double File Extension).
    Source: https://raw.githubusercontent.com/coreruleset/coreruleset/main/rules/REQUEST-932-APPLICATION-ATTACK-RCE.conf
    Falls back to DOUBLE_EXT_SEED if unreachable.
    """
    global double_ext_cache, double_ext_cache_ts
    now = time.time()
    if double_ext_cache and (now - double_ext_cache_ts) < PATTERN_CACHE_TTL:
        return double_ext_cache
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                "https://raw.githubusercontent.com/coreruleset/coreruleset/main"
                "/rules/REQUEST-932-APPLICATION-ATTACK-RCE.conf",
                headers={"User-Agent": "WeblinkScanner/1.0"},
            )
        if resp.status_code == 200:
            # Extract file extension pairs from the CRS double-extension rules.
            # The CRS rule for T1036.007 matches patterns like \.pdf\.exe
            raw_exts = re.findall(
                r'\\\.(\w+)\\\.(exe|js|vbs|bat|ps1|php|html|zip|sh|jar|apk|msi)',
                resp.text
            )
            pairs = list({(a.lower(), b.lower()) for a, b in raw_exts}) or DOUBLE_EXT_SEED
            if pairs:
                double_ext_cache = pairs
                double_ext_cache_ts = now
                print(f"[OWASP CRS DoubleExt] Loaded {len(pairs)} pairs", flush=True)
                return pairs
    except Exception as e:
        print(f"[OWASP CRS DoubleExt] Fetch failed: {e}", flush=True)
    return double_ext_cache if double_ext_cache else DOUBLE_EXT_SEED


async def fetch_free_subdomain_services() -> list:
    """
    Fetches the URLhaus malware domain feed to identify free subdomain services actively being abused by attackers at this moment.
    Source: https://urlhaus.abuse.ch/downloads/text_online/
    This feed lists URLs currently distributing malware - updated every 5 minutes.
    No API key required. Abuse.ch explicitly allows automated consumption.
    Cached for 1 hour. Falls back to FREE_SUBDOMAIN_SEED if unreachable.

    Strategy: extract the suffix of each domain in the feed (e.g., ".ddns.net",
    ".hopto.org"). Suffixes that appear 3+ times are likely free DDNS services
    being abused - add them to the live blocklist.
    """
    global free_subdomain_cache, free_subdomain_cache_ts
    now = time.time()
    if free_subdomain_cache and (now - free_subdomain_cache_ts) < CACHE_TTL:
        return free_subdomain_cache
    try:
        async with httpx.AsyncClient(timeout=12) as client:
            resp = await client.get(
                "https://urlhaus.abuse.ch/downloads/text_online/",
                headers={"User-Agent": "WeblinkScanner/1.0"},
            )
        if resp.status_code == 200:
            from collections import Counter
            lines = [
                l.strip() for l in resp.text.splitlines()
                if l.strip() and not l.startswith("#")
            ]
            # Extract domain from each URL, then get the last two or three parts
            suffix_counts: Counter = Counter()
            for line in lines:
                try:
                    from urllib.parse import urlparse as _up
                    host = _up(line).netloc or _up("https://" + line).netloc
                    host = host.lower().split(":")[0]
                    parts = host.split(".")
                    # Two-part suffix: e.g. .ddns.net
                    if len(parts) >= 3:
                        suffix_counts["." + ".".join(parts[-2:])] += 1
                    # Three-part suffix: e.g. .hopto.org (already covered above)
                except Exception:
                    pass
            # Suffixes seen 3+ times across active malware URLs
            live_suffixes = [
                suf for suf, count in suffix_counts.items()
                if count >= 3 and len(suf) > 5
            ]
            # Merge with seed. always keep the known good ones
            merged = list(set(live_suffixes) | set(FREE_SUBDOMAIN_SEED))
            if merged:
                free_subdomain_cache = merged
                free_subdomain_cache_ts = now
                print(f"[URLhaus DDNS] {len(live_suffixes)} live + {len(FREE_SUBDOMAIN_SEED)} seed = {len(merged)} total subdomain suffixes", flush=True)
                return merged
    except Exception as e:
        print(f"[URLhaus DDNS] Fetch failed: {e}", flush=True)
    return free_subdomain_cache if free_subdomain_cache else FREE_SUBDOMAIN_SEED


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
    host = parsed.netloc or parsed.path

    async def lookup_url(client: httpx.AsyncClient, target: str):
        try:
            resp = await client.post(
                "https://urlhaus-api.abuse.ch/v1/url/",
                data={"url": target},
                timeout=10,
            )
            if resp.status_code == 200:
                data = resp.json()
                status = data.get("query_status", "")
                print(f"[URLhaus /url/] status={status!r} for {target[:60]}", flush=True)
                # was_active = confirmed malware in the past; treat same as active
                if status in ("is_active", "was_active"):
                    threat = data.get("threat", "malware")
                    tags = data.get("tags") or []
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
                data = resp.json()
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
    score = 0
    categories = []
    url_lower = url.lower()

    parsed = urlparse(url if "://" in url else "http://" + url)
    domain = parsed.netloc.lower()
    clean_domain = domain.replace("www.", "")
    query_str = parsed.query

    # Fetch all live intelligence sources concurrently
    keywords, tlds, sqli_patterns, xss_patterns, double_ext_pairs, free_subdomain_suffixes = (
        await asyncio.gather(
            fetch_keywords(),
            fetch_suspicious_tlds(),
            fetch_sqli_patterns(),
            fetch_xss_patterns(),
            fetch_double_ext_pairs(),
            fetch_free_subdomain_services(),
        )
    )

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
    # Pairs sourced live from OWASP CRS REQUEST-932 (MITRE ATT&CK T1036.007).
    # Falls back to DOUBLE_EXT_SEED when OWASP CRS is unreachable.
    for (benign_ext, exec_ext) in double_ext_pairs:
        if re.search(
                r"\." + re.escape(benign_ext) + r"\." + re.escape(exec_ext),
                url_lower
        ):
            score += 30
            categories.append(
                f"Double file extension in URL path (.{benign_ext}.{exec_ext}) - "
                "MITRE ATT\u0026CK T1036.007: masquerading malicious files as benign types"
            )
            break

    # Signal 12: Free subdomain hosting services
    # Suffixes sourced live from the URLhaus malware domain feed
    # Live feed shows which DDNS suffixes are CURRENTLY being abused.
    # Merged with FREE_SUBDOMAIN_SEED as a permanent fallback.
    for svc in free_subdomain_suffixes:
        if clean_domain.endswith(svc):
            score += 25
            categories.append(
                f"Free dynamic subdomain service ({svc}) - "
                "currently appearing in URLhaus active malware feed; "
                "attackers use these to host malware without registering real domains"
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
    # Patterns sourced live from OWASP Core Rule Set REQUEST-942-APPLICATION-ATTACK-SQLI.conf
    # The CRS is the industry standard WAF ruleset, maintained by OWASP.
    # Falls back to SQLI_SEED when GitHub is unreachable.
    query_decoded = query_str.replace("%27", "'").replace("%20", " ").replace("+", " ")
    for pattern in sqli_patterns:
        try:
            if re.search(pattern, query_decoded, re.IGNORECASE):
                score += 40
                categories.append(
                    "SQL injection pattern detected in URL parameters - "
                    "matched against OWASP CRS REQUEST-942 (industry-standard WAF ruleset)"
                )
                break
        except re.error:
            continue

    # Signal 15: XSS patterns in query parameters
    # Patterns sourced live from OWASP Core Rule Set REQUEST-941-APPLICATION-ATTACK-XSS.conf
    # Falls back to XSS_SEED when GitHub is unreachable.
    for pattern in xss_patterns:
        try:
            if re.search(pattern, query_decoded, re.IGNORECASE):
                score += 40
                categories.append(
                    "Cross-site scripting (XSS) pattern detected in URL parameters - "
                    "matched against OWASP CRS REQUEST-941 (industry-standard WAF ruleset)"
                )
                break
        except re.error:
            continue

    # Signal 16: Direct binary or archive file download in URL path
    # Three tiers of suspicion:
    #   Tier A (+35): Executable files (.exe, .msi, .bat …) anywhere in path
    #   Tier B (+40): Archive files in a known download/release subpath
    #   Tier C (+30): Archive files anywhere in the path (catches CDN-hosted zips
    #                 like /leuxtrogxre_x64.zip on oss/cloud storage hosts)
    # Tier A is scored higher than Tier B because executables are more directly
    # dangerous. Tier B is highest because a release-path archive is almost
    # certainly a deliberate software delivery point, not an incidental file.
    BINARY_EXTENSIONS = r"\.(exe|msi|bat|ps1|vbs|jar|apk|dmg|pkg|deb|rpm|iso|img|bin)$"
    ARCHIVE_IN_DOWNLOAD = r"/(download|releases|dist|raw|drop|payload|setup|install)/.+\.(zip|rar|7z|tar|gz|tgz)$"
    ARCHIVE_ANYWHERE = r"\.(zip|rar|7z|tar\.gz|tgz|tar)$"
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
        "verdict": verdict,
        "risk_score": score,
        "reason_1": shown[0] if len(shown) > 0 else None,
        "reason_2": shown[1] if len(shown) > 1 else None,
        "reason_3": shown[2] if len(shown) > 2 else None,
        "blacklist_match": blacklist_match,
        "threat_categories": shown,
    }
