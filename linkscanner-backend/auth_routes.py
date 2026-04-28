import os
import smtplib
from email.mime.text import MIMEText
from fastapi import APIRouter, HTTPException, Request, Query
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from database import supabase
from auth import logout_user
from supabase import create_client

# ── Email config ──────────────────────────────────────────────────────────────
# Add as many developer emails as you need — all will receive the approval request
DEVELOPER_EMAILS = [
    "tharlynnhtet24@gmail.com",       # Mickey
    "tanishqa@gmail.com",             # Teammate 1 — replace with real email
    # "another@gmail.com",            # add more here
]
DEVELOPER_EMAIL = DEVELOPER_EMAILS[0]  # kept for backward-compat log messages
SMTP_HOST       = os.environ.get("SMTP_HOST", "smtp.gmail.com")
SMTP_PORT       = int(os.environ.get("SMTP_PORT", "587"))
SMTP_USER       = os.environ.get("SMTP_USER", "tharlynnhtet24@gmail.com")
SMTP_PASS       = os.environ.get("SMTP_PASS", "dialdwvqotehzjiu")
SERVER_BASE_URL = os.environ.get("SERVER_BASE_URL", "http://192.168.68.56:8000")

def send_approval_email(user_id: str, name: str, email: str, role: str):
    # Points to the FastAPI /approve endpoint — served with correct Content-Type
    approve_url = f"{SERVER_BASE_URL}/approve?user_id={user_id}&action=approve"
    reject_url  = f"{SERVER_BASE_URL}/approve?user_id={user_id}&action=reject"
    role_label  = role.replace("_", " ").title()

    html_body = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>New Signup Request</title>
</head>
<body style="margin:0;padding:0;background:#f0f4ff;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#f0f4ff;padding:32px 0;">
    <tr><td align="center">
      <table width="520" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;">
        <!-- Header -->
        <tr>
          <td style="background:#1d4ed8;padding:28px 32px;text-align:center;">
            <p style="margin:0;font-size:22px;font-weight:700;color:#ffffff;">🔗 LinkScanner</p>
            <p style="margin:6px 0 0;font-size:14px;color:#bfdbfe;">Admin Approval Required</p>
          </td>
        </tr>
        <!-- Body -->
        <tr>
          <td style="padding:32px;">
            <p style="margin:0 0 20px;font-size:16px;color:#0f172a;">A new <strong>{role_label}</strong> account is awaiting your approval:</p>
            <table width="100%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border-radius:10px;padding:16px;margin-bottom:24px;">
              <tr><td style="padding:6px 0;font-size:14px;color:#64748b;width:80px;">Name</td>
                  <td style="padding:6px 0;font-size:14px;color:#0f172a;font-weight:600;">{name}</td></tr>
              <tr><td style="padding:6px 0;font-size:14px;color:#64748b;">Email</td>
                  <td style="padding:6px 0;font-size:14px;color:#0f172a;font-weight:600;">{email}</td></tr>
              <tr><td style="padding:6px 0;font-size:14px;color:#64748b;">Role</td>
                  <td style="padding:6px 0;font-size:14px;color:#1d4ed8;font-weight:600;">{role_label}</td></tr>
            </table>
            <!-- Buttons -->
            <table width="100%" cellpadding="0" cellspacing="0">
              <tr>
                <td width="48%" align="center">
                  <a href="{approve_url}" style="display:block;background:#16a34a;color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;padding:14px 0;border-radius:10px;text-align:center;">✅ Approve</a>
                </td>
                <td width="4%"></td>
                <td width="48%" align="center">
                  <a href="{reject_url}" style="display:block;background:#dc2626;color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;padding:14px 0;border-radius:10px;text-align:center;">❌ Reject</a>
                </td>
              </tr>
            </table>
            <p style="margin:24px 0 0;font-size:12px;color:#94a3b8;text-align:center;">
              ✅ These links work without the server running — hosted on Supabase.<br/>
              You can also approve/reject from the <strong>Admin Dashboard → User Management</strong> in the app.
            </p>
          </td>
        </tr>
        <tr>
          <td style="padding:16px 32px;border-top:1px solid #e2e8f0;text-align:center;font-size:12px;color:#94a3b8;">
            LinkScanner Admin Panel
          </td>
        </tr>
      </table>
    </td></tr>
  </table>
</body>
</html>"""

    plain_body = (
        f"New {role_label} signup request on LinkScanner:\n\n"
        f"Name:  {name}\n"
        f"Email: {email}\n"
        f"Role:  {role_label}\n\n"
        f"APPROVE: {approve_url}\n\n"
        f"REJECT:  {reject_url}\n\n"
        f"These links work without the server running (hosted on Supabase).\n"
        f"You can also approve/reject from Admin Dashboard → User Management in the app."
    )
    try:
        from email.mime.multipart import MIMEMultipart
        msg = MIMEMultipart("alternative")
        msg["Subject"] = f"[LinkScanner] New {role_label} signup - {name}"
        msg["From"]    = SMTP_USER
        msg["To"]      = ", ".join(DEVELOPER_EMAILS)
        msg.attach(MIMEText(plain_body, "plain", "utf-8"))
        msg.attach(MIMEText(html_body,  "html",  "utf-8"))
        with smtplib.SMTP(SMTP_HOST, SMTP_PORT) as server:
            server.starttls()
            server.login(SMTP_USER, SMTP_PASS)
            server.sendmail(SMTP_USER, DEVELOPER_EMAILS, msg.as_string())
        print(f"[EMAIL] Approval email sent to {DEVELOPER_EMAILS} for {name} ({email})")
    except Exception as e:
        print(f"[EMAIL ERROR] Failed to send approval email: {e}")
        raise  # re-raise so signup endpoint can report it

router = APIRouter()

# ── Test email endpoint (remove in production) ────────────────────────────────
@router.get("/test-email")
def test_email():
    try:
        send_approval_email(
            user_id="test-id-123",
            name="Test User",
            email="test@example.com",
            role="admin"
        )
        return {"status": "ok", "message": f"Test email sent to {DEVELOPER_EMAIL}"}
    except Exception as e:
        return {"status": "error", "message": str(e)}

# ── Admin client with service role key (for auth deletion) ────────────────────
SUPABASE_URL         = "https://gcpqarrvkcizefszmyxi.supabase.co"
SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdjcHFhcnJ2a2NpemVmc3pteXhpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTgwOTIyNiwiZXhwIjoyMDg3Mzg1MjI2fQ.7opGKn-G_CqP4hJXPZYROYp2rt0Bp9x-P5EWMnRGflc"
admin_client = create_client(SUPABASE_URL, SUPABASE_SERVICE_KEY)

def safe_error(e: Exception) -> str:
    return str(e).encode('ascii', 'ignore').decode('ascii')

# ── Models ────────────────────────────────────────────────────────────────────

class UserAuth(BaseModel):
    email: str
    password: str

class SignupRequest(BaseModel):
    name: str
    email: str
    password: str
    role: str = "user"

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

class UpdateProfileRequest(BaseModel):
    user_id: str
    name: str

# ── Reset Password Page ───────────────────────────────────────────────────────

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

# ── Signup ────────────────────────────────────────────────────────────────────

@router.post("/signup")
def signup(request: SignupRequest):
    try:
        response = supabase.auth.sign_up({
            "email": request.email,
            "password": request.password
        })
        if response.user is None:
            raise HTTPException(status_code=400, detail="Signup failed. Email may already be in use.")
        # When email confirmation is ON, Supabase returns a user with empty identities for duplicate emails
        identities = getattr(response.user, "identities", None)
        if identities is not None and len(identities) == 0:
            raise HTTPException(status_code=400, detail="This email is already registered. Please log in instead.")
        user_id = str(response.user.id)
        # Admin and platform_manager accounts need developer approval before login
        needs_approval = request.role in ("admin", "platform_manager")
        status = "pending" if needs_approval else "approved"
        supabase.table("users").insert({
            "id":     user_id,
            "name":   request.name,
            "email":  request.email,
            "role":   request.role,
            "status": status,
        }).execute()
        if needs_approval:
            try:
                send_approval_email(user_id, request.name, request.email, request.role)
            except Exception as email_err:
                print(f"[EMAIL ERROR] {email_err}")
                # Account is created and pending — just warn that email failed
                return {
                    "message": "pending_approval",
                    "name":    request.name,
                    "email":   request.email,
                    "role":    request.role,
                    "status":  status,
                    "email_warning": f"Account created but approval email failed: {str(email_err)}"
                }
            return {
                "message": "pending_approval",
                "name":    request.name,
                "email":   request.email,
                "role":    request.role,
                "status":  status,
            }
        return {
            "message": "User created successfully",
            "name":    request.name,
            "email":   request.email,
            "role":    request.role,
            "status":  status,
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))

# ── Login ─────────────────────────────────────────────────────────────────────

@router.post("/login")
def login(user: UserAuth):
    # Pre-check: look up user record by email to enforce account_status
    pre_check = supabase.table("users").select(
        "id, account_status, failed_login_count, status"
    ).eq("email", user.email).execute()
    pre_data = pre_check.data[0] if pre_check.data else None

    if pre_data:
        acc_status = pre_data.get("account_status", "active")
        if acc_status == "suspended":
            raise HTTPException(status_code=403, detail="Your account has been suspended. Please contact the administrator.")
        if acc_status == "locked":
            raise HTTPException(status_code=403, detail="Your account is locked after multiple failed login attempts. Please contact an administrator.")

    try:
        response = supabase.auth.sign_in_with_password({
            "email": user.email,
            "password": user.password
        })
        user_id = str(response.user.id) if response.user else None

        # Reset failed login count on success
        if pre_data:
            supabase.table("users").update({"failed_login_count": 0}).eq("email", user.email).execute()

        name   = None
        plan   = "free"
        role   = "user"
        status = "approved"
        if user_id:
            result = supabase.table("users").select("name, plan, role, status").eq("id", user_id).single().execute()
            if result.data:
                name   = result.data.get("name")
                plan   = result.data.get("plan", "free")
                role   = result.data.get("role", "user")
                status = result.data.get("status", "approved")
        if status == "pending":
            raise HTTPException(status_code=403, detail="Your account is pending approval. Please wait for a developer to approve your request.")
        if status == "rejected":
            raise HTTPException(status_code=403, detail="Your account registration was rejected. Please contact support.")
        return {
            "message":      "Login successful",
            "email":        user.email,
            "name":         name,
            "plan":         plan,
            "role":         role,
            "access_token": response.session.access_token if response.session else None,
            "user_id":      user_id
        }
    except HTTPException:
        raise
    except Exception as e:
        # Increment failed login count
        if pre_data:
            new_count = (pre_data.get("failed_login_count") or 0) + 1
            update_payload = {"failed_login_count": new_count}
            if new_count >= 5:
                update_payload["account_status"] = "locked"
            supabase.table("users").update(update_payload).eq("email", user.email).execute()
        raise HTTPException(status_code=401, detail="Invalid email or password")

# ── Logout ────────────────────────────────────────────────────────────────────

@router.post("/logout")
def logout():
    try:
        logout_user()
        return {"message": "Logged out"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))

# ── Forgot Password ───────────────────────────────────────────────────────────

@router.post("/forgot-password")
def forgot_password(request: ForgotPasswordRequest):
    try:
        supabase.auth.reset_password_email(
            request.email,
            options={"redirect_to": "http://localhost:8000/reset-password"}
        )
        return {"message": "Password reset email sent"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))

# ── Change Password ───────────────────────────────────────────────────────────

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

# ── Update Profile ────────────────────────────────────────────────────────────

@router.put("/update-profile")
def update_profile(request: UpdateProfileRequest):
    try:
        if not request.name.strip():
            raise HTTPException(status_code=400, detail="Name cannot be empty")
        supabase.table("users").update({"name": request.name.strip()}).eq("id", request.user_id).execute()
        return {"message": "Profile updated successfully"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))

# ── Delete Account ────────────────────────────────────────────────────────────

@router.delete("/delete-account")
def delete_account(user_id: str = Query(...)):
    try:
        supabase.rpc("delete_user_account", {"p_user_id": user_id}).execute()
        admin_client.auth.admin.delete_user(user_id)
        return {"message": "Account deleted successfully"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=safe_error(e))