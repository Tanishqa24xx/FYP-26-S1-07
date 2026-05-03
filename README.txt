🔗 Weblink Scanner

A multi-role mobile security platform for detecting malicious, phishing, and suspicious web links - before you click them.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features by Role](#features-by-role)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Threat Detection Engine](#threat-detection-engine)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Backend Setup](#backend-setup)
  - [Android Setup](#android-setup)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Subscription Plans](#subscription-plans)
- [Deployment](#deployment)
- [Screenshots](#screenshots)

---

## Overview

Weblink Scanner is a full-stack, production-grade Android application designed to protect users from malicious URLs encountered in daily digital life. The system provides real-time threat intelligence through a multi-layer scanning engine that cross-references live threat feeds, heuristic URL analysis, and a remote sandbox environment.

The platform supports three distinct user roles — **Regular Users**, **Administrators**, and **Platform Managers** — each with a dedicated interface and a well-defined set of permissions enforced at both the API and database level.

---

## Features by Role

### 👤 Regular Users (Free / Standard / Premium)

| Feature | Free | Standard | Premium |
|---|:---:|:---:|:---:|
| Manual URL scanning | ✅ (5/day) | ✅ (30/day) | ✅ (Unlimited) |
| Camera OCR scanning | ✅ | ✅ | ✅ |
| QR code scanning | ✅ | ✅ | ✅ |
| Risk verdict (Safe / Suspicious / Dangerous) | ✅ | ✅ | ✅ |
| Save & re-check links | ✅ | ✅ | ✅ |
| Scan history | Last 5 | Last 30 days | All time |
| Standard security analysis | ✅ | ✅ | ✅ |
| Sandbox environment | ✅ | ✅ | ✅ |
| Share scan results | ❌ | ✅ | ✅ |
| Browse & auto-scan mode | ❌ | ✅ | ✅ |
| History search & filter | ❌ | ✅ | ✅ |
| Export history (CSV / PDF) | ❌ | ✅ | ✅ |
| Advanced security analysis | ❌ | ❌ | ✅ |
| Ad-heavy & tracker detection | ❌ | ❌ | ✅ |
| Script-level inspection | ❌ | ❌ | ✅ |
| Warning strictness control | ❌ | ✅ | ✅ |

### 🛡️ Administrators

- Full user lifecycle management (create, view, update, suspend, reactivate, lock/unlock)
- User profile (permission group) management with granular per-feature access control
- Security monitoring dashboard - failed login tracking and account lockout enforcement
- Platform-wide scan record and flagged link review
- Immutable audit log of all admin actions
- Subscription plan assignment and management
- Pending account approval workflow with email notifications

### ⚙️ Platform Managers

- Subscription plan lifecycle management (create, update, suspend, activate)
- Analytics dashboard (user counts, scan volumes, verdict breakdowns, plan distribution)
- Date-range report generation (CSV export support)
- Support ticket management with threaded PM ↔ User replies
- FAQ / Help content management (create, edit, delete, reorder)
- System health monitoring and active alert resolution

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Android Client                      │
│         (Jetpack Compose + Retrofit + CameraX)       │
└───────────────────────┬─────────────────────────────┘
                        │ HTTPS / REST
┌───────────────────────▼─────────────────────────────┐
│               FastAPI Backend (Python)               │
│                                                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │ Auth     │ │ Scan     │ │ Admin    │ │ Platform│  │
│  │ Router   │ │ Router   │ │ Router   │ │ Router │  │
│  └──────────┘ └─────┬────┘ └──────────┘ └────────┘  │
│                     │                                │
│          ┌──────────▼──────────┐                    │
│          │  Threat Detection   │                    │
│          │     Engine          │                    │
│          │  Layer 1: URLhaus   │                    │
│          │           PhishTank │                    │
│          │  Layer 2: Heuristics│                    │
│          │  Layer 3: Umbrella  │                    │
│          └──────────┬──────────┘                    │
│                     │                               │
│          ┌──────────▼──────────┐                    │
│          │  Sandbox Service    │                    │
│          │  (urlscan.io)       │                    │
│          └─────────────────────┘                    │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│                  Supabase                            │
│       (PostgreSQL · Auth · Row-Level Security)       │
└─────────────────────────────────────────────────────┘
```

---

## Tech Stack

### Backend
| Component | Technology |
|---|---|
| Framework | FastAPI 0.115 |
| Runtime | Python 3.11+ |
| ASGI Server | Uvicorn |
| HTTP Client | HTTPX (async) |
| Database | Supabase (PostgreSQL) |
| Auth | Supabase Auth (JWT) |
| Email | SMTP via Gmail (TLS) |
| PDF Generation | ReportLab |
| Validation | Pydantic v2 |

### Android Client
| Component | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Networking | Retrofit 2 + OkHttp |
| Camera | CameraX |
| OCR | ML Kit Text Recognition |
| QR Scanning | ML Kit Barcode Scanning |
| Image Loading | Coil 3 |
| Session Storage | Jetpack DataStore |
| Architecture | MVVM (ViewModel + StateFlow) |

### External APIs
| Service | Purpose |
|---|---|
| [URLhaus (abuse.ch)](https://urlhaus.abuse.ch/) | Real-time malware URL and host feed |
| [PhishTank](https://phishtank.org/) | Community-verified phishing URL database |
| [urlscan.io](https://urlscan.io/) | Remote sandbox browser analysis |
| [Cisco Umbrella Top-1M](https://s3-us-west-1.amazonaws.com/umbrella-static/top-1m.csv.zip) | Trusted domain allowlist |
| [Spamhaus TLD Statistics](https://www.spamhaus.org/statistics/tlds/) | High-risk TLD feed |
| [OWASP Core Rule Set](https://github.com/coreruleset/coreruleset) | SQLi / XSS / RCE pattern detection |

---

## Threat Detection Engine

The scan engine runs URLs through three sequential layers. Each layer can escalate or resolve the verdict before passing to the next.

### Layer 1 — Live Threat Feeds (always runs first)
Checks the URL and its host against **URLhaus** (active malware distribution) and **PhishTank** (confirmed phishing). A positive match from either source returns `DANGEROUS` immediately. These results are authoritative and override all heuristics.

### Layer 2 — Heuristic Analysis (runs when Layer 1 is clean)
Scores the URL against 16 structural signals, all sourced from live external feeds where possible:

| Signal | Score | Live Source |
|---|:---:|---|
| No HTTPS | +20 | — |
| Phishing keyword in URL | +15 | URLhaus live tags |
| URL length > 100 chars | +10 | — |
| ≥ 4 subdomain levels | +15 | — |
| IP address as host | +35 | — |
| High-risk TLD | +20 | Spamhaus TLD stats |
| @ symbol in URL | +25 | — |
| ≥ 3 hyphens in domain | +20 | — |
| Percent-encoded characters | +10 | — |
| Punycode / IDN homograph | +25 | — |
| Double file extension | +30 | OWASP CRS REQUEST-932 |
| Free DDNS subdomain service | +25 | URLhaus malware feed |
| UUID / random hex in path | +20 | — |
| SQL injection patterns | +40 | OWASP CRS REQUEST-942 |
| XSS patterns | +40 | OWASP CRS REQUEST-941 |
| Executable / archive file | +30–40 | — |

**Verdict thresholds:** Score ≥ 40 → `DANGEROUS` · Score ≥ 20 → `SUSPICIOUS` · Score < 20 → `SAFE`

### Layer 3 — Trusted Domain Check (runs for SAFE and SUSPICIOUS only)
If the verdict is not yet `DANGEROUS`, the registrable domain is checked against the **Cisco Umbrella Top-50,000**. A match with a valid HTTPS URL overrides the verdict to `SAFE`, suppressing false positives on major legitimate platforms.

### Sandbox Analysis
Powered by **urlscan.io**, the sandbox submits the URL to a remote browser, polls for the result, and extracts: redirect chain, SSL certificate details, external links, domains and IPs contacted, technology stack (Wappalyzer), console errors, page load time, ASN/hosting info, a screenshot, and — for Premium users — ad network detection, tracker detection, and suspicious third-party script inspection.

---

## Project Structure

```
weblink-scanner/
├── backend/                    # FastAPI application
│   ├── main.py                 # App entry point, router registration
│   ├── config.py               # Environment variable loader
│   ├── database.py             # Supabase client initialisation
│   ├── schemas.py              # Pydantic request/response models
│   ├── requirements.txt
│   ├── reset_password.html     # Self-contained password reset page
│   ├── routers/
│   │   ├── auth_routes.py      # Login, signup, password reset, profile
│   │   ├── scan_router.py      # URL, camera, QR scanning + history/export
│   │   ├── sandbox_router.py   # Sandbox analysis endpoint
│   │   ├── plan_router.py      # Subscription plan management
│   │   ├── saved_links_router.py
│   │   ├── admin_router.py     # Admin user/profile/audit management
│   │   ├── platform_router.py  # PM plans/analytics/support/FAQ/health
│   │   ├── approval_router.py  # Email-based account approval page
│   │   └── faq_router.py       # Public FAQ endpoint
│   └── services/
│       ├── scan_service.py     # 3-layer threat detection engine
│       └── sandbox_service.py  # urlscan.io integration
│
└── android/                    # Kotlin / Jetpack Compose app
    ├── data/
    │   ├── api/
    │   │   ├── NewApiService.kt        # Retrofit interface (all endpoints)
    │   │   └── NewRetrofitClient.kt    # OkHttp + Retrofit configuration
    │   ├── models/                     # Request/response data classes
    │   └── repository/
    │       ├── WeblinkScannerRepository.kt
    │       └── SessionStore.kt
    ├── viewmodel/
    │   ├── ScanViewModel.kt
    │   ├── SandboxViewModel.kt
    │   ├── PlanViewModel.kt
    │   ├── AdminViewModel.kt
    │   └── PlatformViewModel.kt
    ├── ui/
    │   ├── screens/            # All user-facing screens
    │   ├── screens/admin/      # Admin-only screens
    │   └── screens/platform/   # Platform Manager screens
    └── utils/
        ├── TokenManager.kt
        ├── AutoLogoutManager.kt
        ├── WarningStrictnessManager.kt
        └── ScanLimitNotificationManager.kt
```

---

## Getting Started

### Prerequisites

- Python 3.11+
- Android Studio Hedgehog or later
- A [Supabase](https://supabase.com/) project
- API keys for URLhaus, PhishTank, and urlscan.io (see [Environment Variables](#environment-variables))

---

### Backend Setup

**1. Clone the repository**
```bash
git clone https://github.com/your-org/weblink-scanner.git
cd weblink-scanner/backend
```

**2. Create and activate a virtual environment**
```bash
python -m venv venv
# macOS / Linux
source venv/bin/activate
# Windows
venv\Scripts\activate
```

**3. Install dependencies**
```bash
pip install -r requirements.txt
# For PDF export support
pip install reportlab
```

**4. Configure environment variables**

Copy the example file and fill in your credentials:
```bash
cp .env.example .env
```

See [Environment Variables](#environment-variables) for the full reference.

**5. Run the development server**
```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at `http://localhost:8000`.  
Interactive docs: `http://localhost:8000/docs`

---

### Android Setup

**1. Open the project**

Open the `android/` directory (or root if monorepo) in Android Studio.

**2. Set the backend URL**

In `app/src/main/java/.../data/api/NewRetrofitClient.kt`, update `BASE_URL`:

```kotlin
// Local development (Android Emulator)
private const val BASE_URL = "http://10.0.2.2:8000/"

// Production
private const val BASE_URL = "https://your-app.onrender.com/"
```

Also update the export URL in `ScanHistoryScreen.kt`:
```kotlin
val baseUrl = "https://your-app.onrender.com/scan/export"
```

**3. Build and run**

Select an emulator or physical device and press **Run** (`Shift+F10`).

---

## Environment Variables

Create a `.env` file in the backend directory with the following variables:

```env
# --- Supabase ---
# Main database project
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key

# Auth project (can be same project)
SUPABASE_AUTH_URL=https://your-project.supabase.co
SUPABASE_AUTH_ANON_KEY=your-anon-key

# Service role key (used server-side only for admin ops)
SUPABASE_SERVICE_KEY=your-service-role-key

# --- Threat Intelligence APIs ---
URLHAUS_API_KEY=your-urlhaus-key
PHISHTANK_API_KEY=your-phishtank-key
URLSCAN_API_KEY=your-urlscan-key

# --- Email (SMTP - Gmail recommended) ---
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-gmail@gmail.com
SMTP_PASS=your-app-password
DEVELOPER_EMAIL=admin@yourdomain.com

# --- Server ---
# Used in approval email links - must be publicly reachable
SERVER_BASE_URL=https://your-app.onrender.com
```

> **Security note:** Never commit `.env` to version control. The `.gitignore` is pre-configured to exclude it.

---

## API Reference

The full interactive API reference is available at `/docs` (Swagger UI) or `/redoc` when the server is running.

### Key Endpoint Groups

| Prefix | Description |
|---|---|
| `POST /login` | Authenticate and receive a JWT access token |
| `POST /signup` | Register a new account |
| `POST /forgot-password` | Send password reset email |
| `PUT /update-profile` | Update name and email |
| `DELETE /delete-account` | Permanently delete account |
| `POST /scan/url` | Scan a URL for threats |
| `POST /scan/camera` | Scan text extracted via OCR |
| `POST /scan/qr` | Scan raw QR code data |
| `GET /scan/history/{userId}` | Retrieve scan history |
| `GET /scan/export` | Export history as CSV or PDF |
| `POST /sandbox/analyse` | Run sandbox analysis via urlscan.io |
| `GET /plan/` | Get user's current plan and usage |
| `GET /plan/all` | List all available subscription plans |
| `POST /plan/upgrade` | Change the user's subscription plan |
| `GET /saved-links/{userId}` | Retrieve saved links |
| `POST /saved-links/rescan` | Re-scan saved links (quota-aware) |
| `GET /admin/users` | List all users (admin only) |
| `GET /admin/audit` | Retrieve admin audit log |
| `GET /platform/analytics/overview` | Platform analytics summary |
| `GET /platform/support` | List support tickets |
| `GET /platform/health` | System health status |

All protected endpoints require an `Authorization: Bearer <token>` header.

---

## Subscription Plans

| | Free | Standard | Premium |
|---|:---:|:---:|:---:|
| **Price** | $0/mo | $4.99/mo | $9.99/mo |
| **Daily Scans** | 5 | 30 | Unlimited |
| **Scan History** | Last 5 | 30 days | All time |
| **Security Analysis** | Standard | Standard | Advanced |
| **Sandbox** | ✅ | ✅ | ✅ |
| **Browse & Auto-scan** | ❌ | ✅ | ✅ |
| **Export (CSV/PDF)** | ❌ | ✅ | ✅ |
| **Ad/Tracker Detection** | ❌ | ❌ | ✅ |
| **Script Inspection** | ❌ | ❌ | ✅ |

---

## Deployment

### Backend — Render (recommended)

1. Push your code to GitHub.
2. Create a new **Web Service** on [Render](https://render.com).
3. Set **Build Command:** `pip install -r requirements.txt`
4. Set **Start Command:** `uvicorn main:app --host 0.0.0.0 --port $PORT`
5. Add all environment variables from `.env` in the Render dashboard under **Environment**.
6. Set `SERVER_BASE_URL` to your Render public URL (e.g. `https://weblink-scanner-api.onrender.com`).
7. Deploy. Render will auto-deploy on every push to `main`.

### Android — Production Build

1. Update `BASE_URL` in `NewRetrofitClient.kt` to your Render URL.
2. Update the export URL in `ScanHistoryScreen.kt`.
3. In Android Studio: **Build → Generate Signed Bundle / APK**.
4. Distribute via Google Play or direct APK.

---

## Security Considerations

- All authentication is handled by **Supabase Auth** (JWT-based, bcrypt-hashed passwords).
- The **service role key** is used exclusively server-side and is never exposed to the Android client.
- Admin and Platform Manager accounts require **manual approval** via an email workflow before login is permitted.
- Accounts are automatically **locked** after 5 consecutive failed login attempts.
- All user sessions support configurable **auto-logout** on inactivity (5, 10, or 30 minutes).
- **User Profiles** provide fine-grained, per-feature permission control enforced at the API level.
- The sandbox never loads the scanned URL on the user's device — analysis is performed entirely by urlscan.io's remote infrastructure.

---

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request against `main`.

Please ensure all new backend endpoints are covered by Pydantic schemas and that any new screen follows the existing MVVM pattern.

---

<p align="center">Built with ❤️ using FastAPI, Supabase, and Jetpack Compose</p>