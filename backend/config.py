# config.py

import os
from dotenv import load_dotenv

load_dotenv()

class Settings:

    # --- Supabase: main database ---
    SUPABASE_URL: str = os.getenv("SUPABASE_URL", "")
    SUPABASE_ANON_KEY: str = os.getenv("SUPABASE_ANON_KEY", "")

    # --- Supabase: auth project ---
    SUPABASE_AUTH_URL: str = os.getenv("SUPABASE_AUTH_URL", "")
    SUPABASE_AUTH_ANON_KEY: str = os.getenv("SUPABASE_AUTH_ANON_KEY", "")

    # --- Supabase: service role (admin, account deletion only)
    SUPABASE_SERVICE_KEY: str = os.getenv("SUPABASE_SERVICE_KEY", "")

    # --- URLhaus ---
    URLHAUS_API_KEY: str = os.getenv("URLHAUS_API_KEY", "")

    # --- PhishTank ---
    PHISHTANK_API_KEY: str = os.getenv("PHISHTANK_API_KEY", "")

    # --- urlscan.io (optional) ---
    URLSCAN_API_KEY: str = os.getenv("URLSCAN_API_KEY", "")


settings = Settings()