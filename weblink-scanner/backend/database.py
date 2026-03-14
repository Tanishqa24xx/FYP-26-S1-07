# backend/database.py
import logging
import asyncpg
from supabase import create_client, Client
from config import settings

# Configure logger
logger = logging.getLogger(__name__)

# Global Supabase client instance
_supabase: Client | None = None
_pool: asyncpg.pool.Pool | None = None

def get_supabase() -> Client:
    """
    Returns a singleton Supabase client.
    """
    global _supabase
    if _supabase is None:
        try:
            _supabase = create_client(settings.SUPABASE_URL, settings.SUPABASE_KEY)
            logger.info("Supabase client initialized.")
        except Exception as e:
            logger.error(f"Supabase connection failed: {e}")
            raise
    return _supabase

async def get_pool() -> asyncpg.pool.Pool:
    """
    Returns a singleton asyncpg connection pool.
    """
    global _pool
    if _pool is None:
        _pool = await asyncpg.create_pool(settings.DATABASE_URL)
        logger.info("PostgreSQL connection pool created.")
    return _pool

# -------------------------------
# DATABASE OPERATIONS
# -------------------------------

def get_profile(user_id: str):
    """
    Retrieve a user's profile from the 'profiles' table.
    """
    supabase = get_supabase()
    result = (
        supabase
        .table("profiles")
        .select("*")
        .eq("id", user_id)
        .single()
        .execute()
    )
    return result.data

def add_scan(user_id: str, url: str, scan_source="manual", risk_level="unknown", risk_score=0.0):
    """
    Insert a new scan result into the 'scan_results' table.
    """
    supabase = get_supabase()
    data = {
        "user_id": user_id,
        "url": url,  # fixed typo from "ulr"
        "scan_source": scan_source,
        "risk_level": risk_level,
        "risk_score": risk_score
    }
    result = supabase.table("scan_results").insert(data).execute()
    return result.data

def get_scans(user_id: str):
    """
    Retrieve all scans belonging to a user.
    """
    supabase = get_supabase()
    result = (
        supabase
        .table("scan_results")
        .select("*")
        .eq("user_id", user_id)
        .order("created_at", desc=True)
        .execute()
    )
    return result.data
