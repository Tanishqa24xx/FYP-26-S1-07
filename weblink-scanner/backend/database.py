from supabase import create_client
from config import settings

# Initialize Supabase client using environment variables from config.py
supabase = create_client(
    settings.SUPABASE_URL,
    settings.SUPABASE_ANON_KEY
)
