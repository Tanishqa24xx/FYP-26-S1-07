# WeblinkScanner\backend\services\sandbox_service.py

import httpx
import socket
import ssl
import time
import re
import uuid
import os
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

        with context.wrap_socket(
                socket.socket(),
                server_hostname=host
        ) as s:
            s.connect((host, 443))
            cert = s.getpeercert()

            return {
                "valid": True,
                "issuer": str(cert.get("issuer")),
                "expiry": cert.get("notAfter")
            }
    except:
        return {
            "valid": False,
            "issuer": None,
            "expiry": None
        }

async def run_sandbox(url: str):
    parsed = urlparse(url)
    host = parsed.netloc
    ip_address = None
    ssl_info = None

    try:
        ip_address = socket.gethostbyname(host)
    except:
        pass

    if parsed.scheme == "https":
        ssl_info = check_ssl(host)

    redirect_chain = []
    external_links = []
    page_title = None
    status_code = None

    start = time.time()

    async with httpx.AsyncClient(follow_redirects=True) as client:
        response = await client.get(url, headers={"User-Agent": "Mozilla/5.0"})
        status_code = response.status_code

        for r in response.history:
            redirect_chain.append(str(r.url))

        redirect_chain.append(str(response.url))
        html = response.text
        # screenshot_path = await capture_screenshot(url)
        page_title = extract_title(html)
        external_links = extract_links(html)

    load_time_ms = int((time.time() - start) * 1000)

    return {
        "status_code": status_code,
        "page_title": page_title,
        "ip_address": ip_address,
        "load_time_ms": load_time_ms,
        "redirect_chain": redirect_chain,
        "external_links": external_links,
        # "screenshot_path": screenshot_path,
        "ssl_info": ssl_info
    }

"""
async def capture_screenshot(url: str):

    os.makedirs("screenshots", exist_ok=True)

    async with async_playwright() as p:
        browser = await p.chromium.launch()
        page = await browser.new_page()

        await page.goto(url, timeout=15000)

        filename = f"screenshots/{uuid.uuid4()}.png"

        await page.screenshot(path=filename, full_page=True)
        await browser.close()
        return filename
"""
