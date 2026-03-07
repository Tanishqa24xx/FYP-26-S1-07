# Python backend script

import os
import logging
from supabase import create_client, Client
from dotenv import load_dotenv

# Load .env variables
load_dotenv()

# configure logger
logger = logging.getLogger(__name__)

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")

# Global Supabase client instance
_supabase: Client | None = None

"""
Returns a singleton Supabase client.
The Client is created once and reused.
"""
def get_supabase() -> Client:
  global _supabase
  if _supbase is None:
    try:
        _supabase = create_client(SUPABASE_URL, SUPABASE_KEY)
        logger.info("Supabase client initialised.")
    except Exception as e:
        logger.error(f"Supabase connection failed: {e}")
        raise

  return _supabase

# ===============================
# DATABASE OPERATIONS
# ===============================

"""
Get a user's profile from the profiles table
"""
def get_profile(user_id: str):
  supabase = get_supabase()

  result = (supabase
             .table("profiles")
             .select("*")
             .eq("id", user_id)
             .single()
             .execute()
           )
  
  # return result as python dictionary
  return result.data 

"""
Insert new scan result into scan_results table
"""
def add_scan(user_id: str, url: str, scan_source="manual", risk_level="unknown", risk_score=0.0):
  supabase = get_supabase()

  data = {"user_id": user_id,
          "ulr": url,
          "scan_source": scan_source,
          "risk_level": risk_level,
          "risk_score": risk_score
  }

  result = supabase.table("scan_results").insert(data).execute()
  return result.data

"""
Retrieve all scans belonging to a user
"""
def get_scans(user_id: str):
  supabase = get_supabase()

  result = (supabase
            .table("scan_results")
            .select("*")
            .eq("user_id", user_id)
            .order("created_at", desc=True)
            .execute()
  )

  return result.data
  



