from supabase import create_client
from config import settings

# Initialize Supabase client for auth using settings
supabase = create_client(
    settings.SUPABASE_AUTH_URL or settings.SUPABASE_URL,
    settings.SUPABASE_AUTH_ANON_KEY or settings.SUPABASE_ANON_KEY
)

def signup_user(email, password):
    response = supabase.auth.sign_up({
        "email": email,
        "password": password
    })
    return response

def login_user(email, password):
    response = supabase.auth.sign_in_with_password({
        "email": email,
        "password": password
    })
    return response

def logout_user():
    supabase.auth.sign_out()
