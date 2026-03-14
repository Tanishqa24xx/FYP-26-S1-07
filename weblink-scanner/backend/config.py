# backend/config.py
from pydantic_settings import BaseSettings
from functools import lru_cache
from typing import Optional

class Settings(BaseSettings):
    APP_NAME: str = "Weblink Scanner"
    DEBUG: bool = False

    # Supabase
    SUPABASE_URL: str = ""
    SUPABASE_KEY: str = ""

    # PostgreSQL connection
    DATABASE_URL: str = (
        "postgresql://postgres.gcpqarrvkcizefszmyxi:BtsEnhypenTxt@aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres"
    )

    # External APIs
    VIRUSTOTAL_API_KEY: str = ""
    GOOGLE_SAFE_BROWSING_API_KEY: str = ""
    URLSCAN_API_KEY: str = ""

    # Sandbox settings
    SANDBOX_TIMEOUT_SECONDS: int = 30
    SANDBOX_MAX_REDIRECTS: int = 5
    FREE_DAILY_SCAN_LIMIT: Optional[int] = None

    class Config:
        env_file = ".env"
        case_sensitive = True

@lru_cache()
def get_settings():
    return Settings()

settings = get_settings()
