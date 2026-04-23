# services/sandbox_service.py

import httpx
import socket
import ssl
import time
import re
from urllib.parse import urlparse


def extract_title(html: str):
    match = re.search(r"<title>(.*?)</title>", html, re.IGNORECASE)
    return match.group(1) if match else None


def extract_links(html: str):
    links = re.findall(r'href="(http[^"]+)"', html)
    return list(set(links))[:20]


def check_ssl(host):
    try:
        context = ssl.create_default_context()
        with context.wrap_socket(socket.socket(), server_hostname=host) as s:
            s.connect((host, 443))
            cert = s.getpeercert()
            return {"valid": True, "issuer": str(cert.get("issuer")), "expiry": cert.get("notAfter")}
    except Exception:
        return {"valid": False, "issuer": None, "expiry": None}


async def run_sandbox(url: str):
    # Add protocol if missing — use https by default
    if not url.startswith("http://") and not url.startswith("https://"):
        url = "https://" + url

    parsed     = urlparse(url)
    host       = parsed.netloc
    ip_address = None
    ssl_info   = None

    try:
        ip_address = socket.gethostbyname(host)
    except Exception:
        pass

    redirect_chain = []
    external_links = []
    page_title     = None
    status_code    = None
    load_time_ms   = 0
    final_url      = url

    try:
        start = time.time()
        async with httpx.AsyncClient(
            follow_redirects = True,
            timeout          = httpx.Timeout(15.0, connect=5.0)
        ) as client:
            response    = await client.get(url, headers={"User-Agent": "Mozilla/5.0"})
            status_code = response.status_code
            final_url   = str(response.url)

            for r in response.history:
                redirect_chain.append(str(r.url))
            redirect_chain.append(final_url)

            html           = response.text
            page_title     = extract_title(html)
            external_links = extract_links(html)

        load_time_ms = int((time.time() - start) * 1000)

        # If httpx fetched successfully over https, SSL is valid
        # (httpx verifies SSL by default — if it didn't raise, cert is good)
        final_parsed = urlparse(final_url)
        if final_parsed.scheme == "https":
            ssl_info = {"valid": True, "issuer": None, "expiry": None}
        else:
            ssl_info = {"valid": False, "issuer": None, "expiry": None}

    except httpx.TimeoutException:
        page_title = "Request timed out — site took too long to respond"
        if parsed.scheme == "https":
            ssl_info = check_ssl(host)
    except httpx.ConnectError:
        # https failed — try http fallback
        try:
            http_url = "http://" + host + parsed.path
            start    = time.time()
            async with httpx.AsyncClient(follow_redirects=True, timeout=httpx.Timeout(10.0, connect=5.0)) as client:
                response    = await client.get(http_url, headers={"User-Agent": "Mozilla/5.0"})
                status_code = response.status_code
                final_url   = str(response.url)
                for r in response.history:
                    redirect_chain.append(str(r.url))
                redirect_chain.append(final_url)
                html           = response.text
                page_title     = extract_title(html)
                external_links = extract_links(html)
            load_time_ms = int((time.time() - start) * 1000)
            # No SSL since we fell back to http
            ssl_info = {"valid": False, "issuer": None, "expiry": None}
        except Exception as e:
            page_title = f"Could not connect to site"
    except Exception as e:
        page_title = f"Could not fetch page: {str(e)[:80]}"
        if parsed.scheme == "https":
            ssl_info = check_ssl(host)

    return {
        "status_code":    status_code,
        "page_title":     page_title,
        "ip_address":     ip_address,
        "load_time_ms":   load_time_ms,
        "redirect_chain": redirect_chain,
        "external_links": external_links,
        "ssl_info":       ssl_info,
    }
