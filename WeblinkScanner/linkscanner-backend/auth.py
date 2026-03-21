from supabase import create_client

SUPABASE_URL = "https://qfyfofxvgqvvnaijrbti.supabase.co"
SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFmeWZvZnh2Z3F2dm5haWpyYnRpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI3MTQ0NzMsImV4cCI6MjA4ODI5MDQ3M30.1MAvhL4DCM4Xn8W3rxcfwrjEuCLevKEadDOOxD5VTNo"

supabase = create_client(SUPABASE_URL, SUPABASE_KEY)


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