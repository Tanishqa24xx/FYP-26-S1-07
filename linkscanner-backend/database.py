import os
from supabase import create_client
from dotenv import load_dotenv

load_dotenv()

SUPABASE_URL = os.environ.get(
    "SUPABASE_URL",
    "https://gcpqarrvkcizefszmyxi.supabase.co"          # fallback for local dev
)
SUPABASE_KEY = os.environ.get(
    "SUPABASE_SERVICE_KEY",
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdjcHFhcnJ2a2NpemVmc3pteXhpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTgwOTIyNiwiZXhwIjoyMDg3Mzg1MjI2fQ.7opGKn-G_CqP4hJXPZYROYp2rt0Bp9x-P5EWMnRGflc"  # fallback for local dev
)

supabase = create_client(SUPABASE_URL, SUPABASE_KEY.strip())