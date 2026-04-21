import os
from fastapi import APIRouter, HTTPException, Request, Query
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from database import supabase
from auth import logout_user
from supabase import create_client
from config import settings

router = APIRouter()

# --- Admin client with service role key (for auth deletion) ---
# Uses the service role key from settings
admin_client = create_client(
    settings.SUPABASE_URL, 
    settings.SUPABASE_SERVICE_KEY
)

def safe_error(e: Exception) -> str:
    return str(e).encode('ascii', 'ignore').decode('ascii')

# --- Models ---

class UserAuth(BaseModel):
    email: str
    password: str

class SignupRequest(BaseModel):
    name: str
    email: str
    password: str

class ForgotPasswordRequest(BaseModel):
    email: str

class ChangePasswordRequest(BaseModel):
    email: str
    current_password: str
    new_password: str

class ResetPasswordRequest(BaseModel):
    access_token: str
    new_password: str

class DeleteAccountRequest(BaseModel):
    user_id: str

# --- Reset Password Page ---

@router.get("/reset-password", response_class=HTMLResponse)
async def reset_password_page(request: Request):
    html_path = os.path.join(os.path.dirname(__file__), "reset_password.html")
    with open(html_path, "r", encoding="utf-8") as f:
        return HTMLResponse(content=f.read())

@router.post("/reset-password")
def reset_password(request: ResetPasswordRequest):
    try:
        supabase.auth.set_session(request.access_token, request.access_token)
        supabase.auth.update_user({"password": request.new_password})
        return {"message": "Password updated successfully"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))

# --- Signup ---

@router.post("/signup")
def signup(request: SignupRequest):
    try:
        response = supabase.auth.sign_up({
            "email": request.email,
            "password": request.password
        })
        if response.user is None:
            raise HTTPException(status_code=400, detail="Signup failed. Email may already be in use.")
        user_id = str(response.user.id)
        supabase.table("users").insert({
            "id": user_id,
            "name": request.name,
            "email": request.email,
        }).execute()
        return {"message": "User created successfully", "name": request.name, "email": request.email}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))

# --- Login ---

@router.post("/login")
def login(user: UserAuth):
    try:
        response = supabase.auth.sign_in_with_password({
            "email": user.email,
            "password": user.password
        })
        user_id = str(response.user.id) if response.user else None
        name = None
        plan = "free"
        if user_id:
            result = supabase.table("users").select("name, plan").eq("id", user_id).single().execute()
            if result.data:
                name = result.data.get("name")
                plan = result.data.get("plan", "free")
        return {
            "message": "Login successful",
            "email": user.email,
            "name": name,
            "plan": plan,
            "access_token": response.session.access_token if response.session else None,
            "user_id": user_id
        }
    except Exception as e:
        raise HTTPException(status_code=401, detail=safe_error(e))

# --- Logout ---

@router.post("/logout")
def logout():
    try:
        logout_user()
        return {"message": "Logged out"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))

# --- Forgot Password ---

@router.post("/forgot-password")
def forgot_password(request: ForgotPasswordRequest):
    try:
        supabase.auth.reset_password_email(
            request.email,
            options={"redirect_to": "https://weblink-scanner.onrender.com"}
        )
        return {"message": "Password reset email sent"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))

# --- Change Password ---

@router.post("/change-password")
def change_password(request: ChangePasswordRequest):
    try:
        auth_response = supabase.auth.sign_in_with_password({
            "email": request.email,
            "password": request.current_password
        })
        if not auth_response.session:
            raise HTTPException(status_code=401, detail="Current password is incorrect")
        supabase.auth.set_session(auth_response.session.access_token, auth_response.session.refresh_token)
        supabase.auth.update_user({"password": request.new_password})
        return {"message": "Password changed successfully"}
    except HTTPException:
        raise
    except Exception as e:
        error_msg = safe_error(e)
        if "invalid" in error_msg.lower() or "credentials" in error_msg.lower():
            raise HTTPException(status_code=401, detail="Current password is incorrect")
        raise HTTPException(status_code=400, detail=error_msg)

# --- Delete Account ---

@router.delete("/delete-account")
def delete_account(user_id: str = Query(...)):
    try:
        supabase.rpc("delete_user_account", {"p_user_id": user_id}).execute()
        admin_client.auth.admin.delete_user(user_id)
        return {"message": "Account deleted successfully"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))
