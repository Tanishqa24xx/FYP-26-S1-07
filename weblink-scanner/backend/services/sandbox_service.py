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
            # Wait before polling: longer initial wait, then regular interval
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
                # Still processing - keep polling
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
    page    = result.get("page",    {})
    lists   = result.get("lists",   {})
    data    = result.get("data",    {})
    stats   = result.get("stats",   {})
    meta    = result.get("meta",    {})
    task    = result.get("task",    {})
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
        "protocol":   None,   # urlscan does not expose TLS version
    }

    # --- Redirect chain: task URL -> final page URL ---
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

    # --- External links on page ---
    # data.links contains {href, text} objects; we extract href
    raw_links    = data.get("links", [])
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
    wappa = meta.get("processors", {}).get("wappa", {}).get("data", [])
    tech_detected = [w.get("app") for w in wappa if w.get("app")]

    # --- Stats ---
    total_reqs   = stats.get("requests",  None)
    total_size   = stats.get("dataLength", None)
    total_size_kb = round(total_size / 1024) if total_size else None

    # --- urlscan verdict ---
    urlscan_verdict = verdicts.get("urlscan", {})
    verdict_score   = urlscan_verdict.get("score")      # -100 to 100
    verdict_cats    = urlscan_verdict.get("categories", [])
    malicious       = verdict_score is not None and verdict_score > 0

    # --- ASN info (for hosting section) ---
    asn_info = None
    if asn or asnname:
        asn_info = {"asn": asn, "asnname": asnname, "country": country}

    # --- Screenshot URL ---
    screenshot_url = SCREENSHOT.format(uuid=uuid)

    # --- Load time (urlscan does not expose this directly. use timing if present) ---
    timing     = data.get("timing", {})
    load_event = timing.get("loadEventEnd")
    nav_start  = timing.get("navigationStart")
    load_time_ms = None
    if load_event and nav_start:
        try:
            load_time_ms = int(load_event - nav_start)
        except Exception:
            pass

    # --- Ad networks and trackers (Premium enrichment) ---
    AD_TECH_KEYWORDS = {
        "google ads", "google adsense", "google admanager", "google doubleclick",
        "doubleclick", "adsense", "admanager", "adroll", "adnxs", "appnexus",
        "amazon advertising", "amazon associates", "media.net", "outbrain",
        "taboola", "revcontent", "propellerads", "mgid", "sharethrough",
        "criteo", "pubmatic", "rubicon", "openx", "triplelift", "sovrn",
        "indexexchange", "33across", "smartadserver", "teads", "undertone",
        "conversant", "spotx", "yieldmo", "bidswitch",
    }
    TRACKER_DOMAINS = {
        "google-analytics.com", "googletagmanager.com", "googletagservices.com",
        "doubleclick.net", "googlesyndication.com",
        "connect.facebook.net", "facebook.net",
        "analytics.twitter.com", "static.ads-twitter.com",
        "snap.licdn.com", "ads.linkedin.com",
        "hotjar.com", "fullstory.com", "mouseflow.com", "clarity.ms",
        "mixpanel.com", "amplitude.com", "segment.com", "segment.io",
        "intercom.io", "intercom.com",
        "criteo.com", "criteo.net", "adnxs.com", "taboola.com", "outbrain.com",
        "scorecardresearch.com", "quantserve.com", "comscore.com",
        "omtrdc.net", "demdex.net", "2mdn.net", "rubiconproject.com",
        "pubmatic.com", "openx.net", "adsrvr.org",
        "amazon-adsystem.com", "media.net", "adroll.com",
        "nr-data.net", "newrelic.com",
        "pardot.com", "marketo.net", "hubspot.com", "hs-scripts.com",
    }
    SAFE_CDN_DOMAINS = {
        "jquery.com", "bootstrapcdn.com", "cloudflare.com", "jsdelivr.net",
        "unpkg.com", "cdnjs.cloudflare.com", "googleapis.com", "gstatic.com",
    }

    detected_ad_tech = [
        t for t in tech_detected
        if any(kw in t.lower() for kw in AD_TECH_KEYWORDS)
    ]
    detected_trackers = [
        d for d in domains_contacted
        if any(d == td or d.endswith("." + td) for td in TRACKER_DOMAINS)
    ]
    suspicious_scripts = [
        u for u in urls_contacted
        if u.endswith(".js")
        and apex_domain and apex_domain not in u
        and not any(cdn in u for cdn in SAFE_CDN_DOMAINS)
    ][:20]

    all_ad_signals = list({*detected_ad_tech, *detected_trackers})
    ad_heavy = len(all_ad_signals) >= 3

    # Debug log so you can verify in backend terminal
    print(f"[sandbox] tech_detected={tech_detected}", flush=True)
    print(f"[sandbox] domains_contacted={domains_contacted[:10]}", flush=True)
    print(f"[sandbox] detected_ad_tech={detected_ad_tech}", flush=True)
    print(f"[sandbox] detected_trackers={detected_trackers}", flush=True)
    print(f"[sandbox] ad_heavy={ad_heavy} (signals={len(all_ad_signals)})", flush=True)

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
        "verdict_score":     verdict_score,
        "verdict_categories": verdict_cats,
        "malicious":         malicious,

        # Hosting
        "asn_info":          asn_info,

        # Screenshot and report links
        "screenshot_url":    screenshot_url,
        "screenshot_b64":    None,
        "report_url":        f"https://urlscan.io/result/{uuid}/",
        "sandbox_uuid":      uuid,

        # Premium enrichment — ad/tracker/script analysis
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