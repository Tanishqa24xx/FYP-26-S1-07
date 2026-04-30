# sandbox_service.py
#
# Uses urlscan.io's remote browser sandbox to analyse URLs.
# Flow:
#   1. POST to /api/v1/scan/ - submits the URL, get back a UUID
#   2. Poll GET /api/v1/result/{uuid}/ - wait until the scan finishes (HTTP 200)
#   3. Extract every useful field from the JSON response
#   4. Screenshot URL is: https://urlscan.io/screenshots/{uuid}.png
#
# API key is read from config.py (loaded from .env).

import asyncio
import httpx
from urllib.parse import urlparse
from config import settings

SUBMIT_URL  = "https://urlscan.io/api/v1/scan/"
RESULT_URL  = "https://urlscan.io/api/v1/result/{uuid}/"
SCREENSHOT  = "https://urlscan.io/screenshots/{uuid}.png"

# urlscan.io typically finishes in 15-45 seconds.
# Strategy: wait 5s before the first poll (scan needs time to start),
# then poll every 4s for up to 15 attempts (60s total ceiling).
INITIAL_WAIT  = 5   # seconds to wait before the very first poll
POLL_INTERVAL = 4   # seconds between subsequent polls
MAX_POLLS     = 15  # 15 × 4s = 60s total polling window

async def run_sandbox(url: str) -> dict:
    if not url.startswith(("http://", "https://")):
        url = "https://" + url

    api_key = settings.URLSCAN_API_KEY
    headers = {"API-Key": api_key, "Content-Type": "application/json"}

    # --- Step 1: Submit ---
    async with httpx.AsyncClient(timeout=15) as client:
        try:
            submit_resp = await client.post(
                SUBMIT_URL,
                json={"url": url, "visibility": "public"},
                headers=headers,
            )
        except Exception as e:
            print(f"[urlscan] Submit failed: {e}")
            return empty(url)

        if submit_resp.status_code not in (200, 201):
            print(f"[urlscan] Submit error {submit_resp.status_code}: {submit_resp.text[:200]}")
            return empty(url)

        submit_data = submit_resp.json()
        uuid = submit_data.get("uuid")
        if not uuid:
            print("[urlscan] No UUID in submit response")
            return empty(url)

    # --- Step 2: Poll until result is ready ---
    result_url = RESULT_URL.format(uuid=uuid)
    result = None

    async with httpx.AsyncClient(timeout=15) as client:
        for attempt in range(MAX_POLLS):
            wait = INITIAL_WAIT if attempt == 0 else POLL_INTERVAL
            await asyncio.sleep(wait)
            try:
                r = await client.get(result_url, headers={"API-Key": api_key})
            except Exception as e:
                print(f"[urlscan] Poll {attempt+1} network error: {e}")
                continue

            if r.status_code == 200:
                print(f"[urlscan] Result ready after {INITIAL_WAIT + attempt * POLL_INTERVAL}s")
                result = r.json()
                break
            elif r.status_code == 404:
                continue
            else:
                print(f"[urlscan] Poll {attempt+1} unexpected status {r.status_code}")
                break

    if result is None:
        print("[urlscan] Timed out waiting for result")
        return empty(url)

    # --- Step 3: Extract fields ---
    return extract(result, uuid, url)


def extract(result: dict, uuid: str, original_url: str) -> dict:
    page     = result.get("page",     {})
    lists    = result.get("lists",    {})
    data     = result.get("data",     {})
    stats    = result.get("stats",    {})
    meta     = result.get("meta",     {})
    task     = result.get("task",     {})
    verdicts = result.get("verdicts", {})

    # --- Page info ---
    page_title   = page.get("title")
    ip_address   = page.get("ip")
    status_code  = safe_int(page.get("status"))
    server       = page.get("server")
    mime_type    = page.get("mimeType")
    final_url    = page.get("url", original_url)
    ptr          = page.get("ptr")
    country      = page.get("country")
    city         = page.get("city")
    asn          = page.get("asn")
    asnname      = page.get("asnname")
    apex_domain  = page.get("apexDomain")

    # --- TLS / SSL ---
    tls_issuer     = page.get("tlsIssuer")
    tls_valid_from = page.get("tlsValidFrom")
    tls_valid_days = page.get("tlsValidDays")
    tls_age_days   = page.get("tlsAgeDays")
    has_tls        = bool(tls_issuer)
    ssl_info = {
        "valid":      has_tls,
        "issuer":     tls_issuer,
        "valid_from": tls_valid_from,
        "valid_days": str(tls_valid_days) if tls_valid_days is not None else None,
        "age_days":   str(tls_age_days)   if tls_age_days   is not None else None,
        "protocol":   None,
    }

    # --- Redirect chain ---
    redirect_chain = []
    task_url = task.get("url", original_url)
    if task_url:
        redirect_chain.append(task_url)
    if final_url and final_url != task_url:
        redirect_chain.append(final_url)

    # --- Domains, IPs, URLs contacted ---
    domains_contacted = lists.get("domains", [])
    ips_contacted     = lists.get("ips",     [])
    urls_contacted    = lists.get("urls",    [])

    # --- External links ---
    raw_links      = data.get("links", [])
    external_links = list({
        lnk.get("href", "")
        for lnk in raw_links
        if lnk.get("href", "").startswith("http")
    })

    # --- Console messages ---
    console_raw = data.get("console", [])
    console_messages = [
        c.get("message", {}).get("text", "") or c.get("text", "")
        for c in console_raw
        if c.get("message", {}).get("level") in ("error", "warning")
        or c.get("level") in ("error", "warning")
    ][:10]

    # --- Technologies (Wappalyzer) ---
    wappa         = meta.get("processors", {}).get("wappa", {}).get("data", [])
    tech_detected = [w.get("app") for w in wappa if w.get("app")]

    # --- Stats ---
    total_reqs    = stats.get("requests",   None)
    total_size    = stats.get("dataLength", None)
    total_size_kb = round(total_size / 1024) if total_size else None

    # --- urlscan verdict ---
    urlscan_verdict = verdicts.get("urlscan", {})
    verdict_score   = urlscan_verdict.get("score")
    verdict_cats    = urlscan_verdict.get("categories", [])
    malicious       = verdict_score is not None and verdict_score > 0

    # --- ASN info ---
    asn_info = None
    if asn or asnname:
        asn_info = {"asn": asn, "asnname": asnname, "country": country}

    # --- Screenshot URL ---
    screenshot_url = SCREENSHOT.format(uuid=uuid)

    # --- Load time ---
    timing       = data.get("timing", {})
    load_event   = timing.get("loadEventEnd")
    nav_start    = timing.get("navigationStart")
    load_time_ms = None
    if load_event and nav_start:
        try:
            load_time_ms = int(load_event - nav_start)
        except Exception:
            pass

    # -------------------------------------------------------------------------
    # Premium enrichment: Ads, Trackers, Suspicious Scripts
    #
    # All detection uses only what urlscan returns from data.requests.
    # Confirmed field paths from live API response:
    #   req["request"]["type"]           → resource type (Script/XHR/Image/Fetch/Ping)
    #   req["request"]["request"]["url"] → actual request URL
    #   req["response"]["dataLength"]    → response body size in bytes
    #
    # ADS: URL path contains "/ad", "/ads/", "adfuel", "advert", "sponsor"
    #      OR script filename contains "ad" as a whole word segment
    #      These are structural URL conventions used by all ad systems.
    #
    # TRACKERS: Behavioural signals from request type + URL path
    #   - Ping requests (browser-native tracking API, always third-party tracking)
    #   - XHR/Fetch to paths like /collect, /beacon, /pixel, /track, /analytics
    #   - Images ≤ 1KB from third-party hosts (tracking pixels)
    #
    # SCRIPTS: Structural signals
    #   - Third-party Script over HTTP on HTTPS page
    #   - Script filename is a hex hash (obfuscation pattern)
    # -------------------------------------------------------------------------

    import re as _re

    raw_requests = data.get("requests", [])

    # URL path segments used exclusively by ad delivery systems
    AD_PATH_SIGNALS = [
        "/ads/", "/ad/", "/advert", "/advertisement",
        "adfuel", "adserver", "admanager", "adtech",
        "prebid", "gpt.js", "/gpt/", "adsystem",
        "sponsor", "promoted", "pagead",
    ]

    # URL path segments used by tracking/analytics endpoints
    # These are HTTP-standard naming conventions, not vendor names
    TRACKER_PATH_SIGNALS = [
        "/collect", "/beacon", "/pixel", "/track", "/ping",
        "/analytics", "/hit", "/event", "/log", "/telemetry",
        "/impression", "/conversion", "/metric", "/stat",
    ]

    detected_ad_tech: list  = []
    detected_trackers: list = []
    suspicious_scripts: list = []

    seen_ad_urls:      set = set()
    seen_tracker_hosts: set = set()
    seen_script_urls:  set = set()

    for req in raw_requests:
        try:
            outer    = req.get("request", {}) or {}       # Chrome CDP wrapper
            inner    = outer.get("request", {}) or {}     # actual HTTP request
            resp_top = req.get("response", {}) or {}      # response wrapper

            req_url    = inner.get("url", "") or ""
            req_type   = outer.get("type", "") or ""      # Script/XHR/Image/Fetch/Ping
            data_len   = safe_int(resp_top.get("dataLength")) or 0

            if not req_url:
                continue

            parsed     = urlparse(req_url)
            req_host   = parsed.netloc.lower()
            req_path   = parsed.path.lower()
            req_scheme = parsed.scheme.lower()
            req_fname  = req_path.split("/")[-1]          # filename portion

            is_third_party = apex_domain and apex_domain not in req_host

            # --- ADS ---
            # Signal: URL path contains ad delivery segments
            if any(sig in req_path or sig in req_url.lower() for sig in AD_PATH_SIGNALS):
                label = req_host or req_url[:60]
                if label not in seen_ad_urls:
                    seen_ad_urls.add(label)
                    detected_ad_tech.append(label)
                continue   # don't double-count as tracker

            # --- TRACKERS ---
            if not is_third_party:
                pass  # only flag third-party tracker behaviour

            elif req_type == "Ping":
                # Browser Ping API — always tracking, never legitimate content
                if req_host not in seen_tracker_hosts:
                    seen_tracker_hosts.add(req_host)
                    detected_trackers.append(req_host)

            elif req_type in ("XHR", "Fetch"):
                # Third-party data requests to tracking-named endpoints
                if any(sig in req_path for sig in TRACKER_PATH_SIGNALS):
                    if req_host not in seen_tracker_hosts:
                        seen_tracker_hosts.add(req_host)
                        detected_trackers.append(req_host)

            elif req_type == "Image" and data_len <= 1024:
                # Tiny third-party image = tracking pixel
                if req_host not in seen_tracker_hosts:
                    seen_tracker_hosts.add(req_host)
                    detected_trackers.append(f"{req_host} (pixel, {data_len}B)")

            # --- SCRIPTS ---
            elif req_type == "Script" and is_third_party:
                flag = None

                # Signal 1: HTTP script on HTTPS page (mixed content / suspicious)
                if req_scheme == "http" and original_url.startswith("https"):
                    flag = f"[HTTP on HTTPS] {req_url}"

                # Signal 2: Hex hash filename (obfuscated/fingerprinted script)
                elif _re.fullmatch(r'[0-9a-f-]{8,}', req_fname.split(".")[0]):
                    flag = f"[hash filename] {req_url}"

                if flag and req_url not in seen_script_urls:
                    seen_script_urls.add(req_url)
                    suspicious_scripts.append(req_url)

        except Exception:
            pass

    suspicious_scripts = suspicious_scripts[:20]
    ad_heavy = len(detected_ad_tech) >= 3 or len(detected_trackers) >= 5

    print(f"[sandbox] detected_ad_tech={detected_ad_tech[:5]}", flush=True)
    print(f"[sandbox] detected_trackers={detected_trackers[:5]}", flush=True)
    print(f"[sandbox] suspicious_scripts={suspicious_scripts[:3]}", flush=True)
    print(f"[sandbox] ad_heavy={ad_heavy}", flush=True)

    return {
        # Core page info
        "status_code":       status_code,
        "page_title":        page_title or "Could not retrieve",
        "ip_address":        ip_address,
        "load_time_ms":      load_time_ms,
        "final_url":         final_url,
        "server":            server,
        "mime_type":         mime_type,
        "ptr":               ptr,
        "country":           country,
        "city":              city,
        "apex_domain":       apex_domain,

        # SSL
        "ssl_info":          ssl_info,

        # Network
        "redirect_chain":    redirect_chain,
        "external_links":    external_links,
        "domains_contacted": domains_contacted,
        "domain_count":      len(domains_contacted),
        "ips_contacted":     ips_contacted,
        "ip_count":          len(ips_contacted),
        "urls_contacted":    urls_contacted,

        # Content
        "tech_detected":     tech_detected,
        "console_messages":  console_messages,
        "total_size_kb":     total_size_kb,
        "total_requests":    total_reqs,

        # Verdict from urlscan
        "verdict_score":      verdict_score,
        "verdict_categories": verdict_cats,
        "malicious":          malicious,

        # Hosting
        "asn_info":          asn_info,

        # Screenshot and report links
        "screenshot_url":    screenshot_url,
        "screenshot_b64":    None,
        "report_url":        f"https://urlscan.io/result/{uuid}/",
        "sandbox_uuid":      uuid,

        # Premium enrichment
        "detected_ad_tech":   detected_ad_tech,
        "detected_trackers":  detected_trackers,
        "suspicious_scripts": suspicious_scripts,
        "ad_heavy":           ad_heavy,

        "analysis_source":   "urlscan.io",
    }


def safe_int(val) -> int | None:
    try:
        return int(val)
    except (TypeError, ValueError):
        return None


def empty(url: str) -> dict:
    """Returned when urlscan.io cannot complete the scan."""
    return {
        "status_code":       None,
        "page_title":        None,
        "ip_address":        None,
        "load_time_ms":      None,
        "final_url":         url,
        "server":            None,
        "mime_type":         None,
        "ptr":               None,
        "country":           None,
        "city":              None,
        "apex_domain":       None,
        "ssl_info":          {"valid": None, "issuer": None, "valid_from": None,
                              "valid_days": None, "age_days": None, "protocol": None},
        "redirect_chain":    [],
        "external_links":    [],
        "domains_contacted": [],
        "domain_count":      None,
        "ips_contacted":     [],
        "ip_count":          None,
        "urls_contacted":    [],
        "tech_detected":     [],
        "console_messages":  [],
        "total_size_kb":     None,
        "total_requests":    None,
        "verdict_score":     None,
        "verdict_categories": [],
        "malicious":         None,
        "asn_info":          None,
        "screenshot_url":    None,
        "screenshot_b64":    None,
        "report_url":        None,
        "sandbox_uuid":      None,
        "detected_ad_tech":   [],
        "detected_trackers":  [],
        "suspicious_scripts": [],
        "ad_heavy":           False,
        "analysis_source":   "urlscan.io",
    }
