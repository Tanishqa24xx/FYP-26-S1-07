"""
Run once to push approve_page.html to Supabase Storage.
  python3 upload_approval.py
"""
import os, json
from urllib.request import urlopen, Request
from urllib.error   import HTTPError

SUPABASE_URL     = "https://gcpqarrvkcizefszmyxi.supabase.co"
SERVICE_KEY      = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdjcHFhcnJ2a2NpemVmc3pteXhpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTgwOTIyNiwiZXhwIjoyMDg3Mzg1MjI2fQ.7opGKn-G_CqP4hJXPZYROYp2rt0Bp9x-P5EWMnRGflc"
BASE             = f"{SUPABASE_URL}/storage/v1"
PUBLIC_URL       = f"{SUPABASE_URL}/storage/v1/object/public/approval/approve.html"
AUTH             = {"Authorization": f"Bearer {SERVICE_KEY}", "apikey": SERVICE_KEY}

html_path = os.path.join(os.path.dirname(__file__), "approve_page.html")
with open(html_path, "rb") as f:
    content = f.read()

# ── 1. Create bucket (ignore error if it already exists) ─────────────────────
print("Creating bucket...")
try:
    body = json.dumps({"id": "approval", "name": "approval", "public": True}).encode()
    urlopen(Request(f"{BASE}/bucket", data=body, method="POST",
                    headers={**AUTH, "Content-Type": "application/json"}))
    print("  Bucket created.")
except HTTPError as e:
    msg = e.read().decode()
    if "already exists" in msg.lower() or e.code == 409:
        print("  Bucket already exists, continuing.")
    else:
        print(f"  Bucket note ({e.code}): {msg}")

# ── 2. Delete existing file (so Content-Type metadata is reset on re-upload) ──
print("Deleting old file (if any)...")
try:
    urlopen(Request(f"{BASE}/object/approval/approve.html",
                    method="DELETE", headers=AUTH))
    print("  Old file deleted.")
except HTTPError as e:
    print(f"  Delete skipped ({e.code}) — continuing.")

# ── 3. Upload file fresh with correct Content-Type ────────────────────────────
print("Uploading approve_page.html...")
upload_h = {**AUTH, "Content-Type": "text/html; charset=utf-8", "x-upsert": "true"}
try:
    urlopen(Request(f"{BASE}/object/approval/approve.html",
                    data=content, method="POST", headers=upload_h))
    print(f"\nDone! Approval page is live at:\n   {PUBLIC_URL}\n")
except HTTPError as e:
    err = e.read().decode()
    print(f"\nUpload failed ({e.code}): {err}\n")
