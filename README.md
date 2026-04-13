
🕵️‍♂️ Weblink Scanner (FYP‑26‑S1‑07)
----------------------------------
Weblink Scanner is an Android security application designed to protect users from malicious links, phishing attempts, and insecure web pages. It combines mobile accessibility with high‑level script inspection and remote sandboxing, allowing users to safely verify any link—captured via browser, QR code, or camera OCR—before interacting with it.


🛡️ 3‑Layer Security Architecture

The application uses a fail‑fast scanning engine that evaluates threats across three layers:

1. Layer 1 — Global Threat Intelligence (Blacklist Check)
Cross‑references URLs against URLhaus (malware) and PhishTank (phishing).

Verdict: If a match is found, the link is instantly marked DANGEROUS, stopping further analysis.

2. Layer 2 — Heuristic Analysis Engine (Behavioral Check)
Inspects URL entropy, length, and structure.

Detects obfuscation techniques used to hide malicious destinations.

Verdict: Assigns a risk score → SAFE, SUSPICIOUS, or DANGEROUS.

3. Layer 3 — Remote Sandbox & Script Inspection (urlscan.io)
Renders the link in an isolated remote browser.

Visual Analysis: Generates a Base64 screenshot (via Coil 3).

Deep Inspection: Extracts SSL/TLS details, identifies trackers, and lists external links found in the source code.

----------------------------------
👥 System Actors & Roles (RBAC)
----------------------------------

Role‑Based Access Control is managed via Supabase:
Free User: Basic scanning + up to 5 history records.
Standard User: Higher daily limits + up to 500 records.
Premium User: Unlimited scans + advanced sandbox metrics + 2000+ history records.
Admin: User management and scan log moderation.
Platform Manager: Subscription Plan Management, API key management (URLhaus, urlscan), system analytics.

----------------------------------
🛠️ Technical Stack
----------------------------------

Frontend (Android Mobile)

Framework: Jetpack Compose (Kotlin)
Image Handling: Coil 3 (Base64 Data URI rendering)
OCR/Scanning: Google ML Kit (Text Recognition & Barcode)
Networking: Retrofit + OkHttp (60‑second timeouts)

Backend (API)
Framework: FastAPI (Python 3.12)
Database: Supabase (PostgreSQL)
Integrations: URLhaus, PhishTank, urlscan.io
Environment: Windows‑optimized using WindowsSelectorEventLoopPolicy

----------------------------------
📂 Project Structure
----------------------------------

WeblinkScanner/

├── app/                           # Android (Kotlin/Compose) source code

├── backend/                       # Python FastAPI source code

│            ├── routers/          # API endpoints (Scan, Sandbox, Plans, FAQ)

│            ├── services/         # 3-Layer scanning logic & API integrations

│            ├── config.py         # Global settings & .env loader

│            └── main.py           # Application entry point

├── .gitignore                     # Ignores venv, .env, large datasets

└── README.md                      # Project documentation


----------------------------------
⚙️ Setup & Installation
----------------------------------

# 1. Backend Setup
   
bash
1. Open terminal in project root
2. Create virtual environment
python -m venv venv

3. Activate environment
venv\Scripts\activate

4. Navigate to backend
cd backend

5. Install dependencies
pip install -r requirements.txt

Create a .env file inside backend/:

env
URLHAUS_API_KEY=your_key
PHISHTANK_API_KEY=your_key
URLSCAN_API_KEY=your_key

Run the server:

bash

uvicorn main:app --reload --host 0.0.0.0 --port 8000

# 2. Mobile Setup
Open the project in Android Studio.
Configure networking in NewRetrofitClient.kt:

Emulator:
<code/>
BASE_URL = "http://10.0.2.2:8000/"

Physical Device:
<code/>
BASE_URL = "http://<your_PC_IP>:8000/"

Build and run on your device/emulator.

----------------------------------
⚠️ Security Note
----------------------------------
This tool is for analysis purposes only. No scanner is 100% effective—always exercise caution when interacting with unknown URLs.

