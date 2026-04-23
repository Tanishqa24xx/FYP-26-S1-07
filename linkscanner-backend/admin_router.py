from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import Optional
from database import supabase
from supabase import create_client
from datetime import datetime, timezone
import os

router = APIRouter(prefix="/admin")

SUPABASE_URL         = "https://gcpqarrvkcizefszmyxi.supabase.co"
SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdjcHFhcnJ2a2NpemVmc3pteXhpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTgwOTIyNiwiZXhwIjoyMDg3Mzg1MjI2fQ.7opGKn-G_CqP4hJXPZYROYp2rt0Bp9x-P5EWMnRGflc"
admin_client = create_client(SUPABASE_URL, SUPABASE_SERVICE_KEY)

# ── Pydantic Models ───────────────────────────────────────────────────────────

class CreateUserRequest(BaseModel):
    name: str
    email: str
    password: str
    role: str = "user"
    plan: str = "free"

class UpdateUserRequest(BaseModel):
    name: Optional[str] = None
    role: Optional[str] = None
    plan: Optional[str] = None

class CreateProfileRequest(BaseModel):
    name: str
    description: Optional[str] = ""
    permissions: list[str] = []

class UpdateProfileRequest(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    permissions: Optional[list[str]] = None

class AssignProfileRequest(BaseModel):
    profile_id: str

# ── Stats ─────────────────────────────────────────────────────────────────────

@router.get("/stats")
def get_admin_stats():
    try:
        users_result = supabase.table("users").select("id, plan, account_status").execute()
        users_data   = users_result.data or []
        total_users  = len(users_data)
        paid_users   = sum(1 for u in users_data if u.get("plan", "free").lower() != "free")

        today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        scans_result = supabase.table("scan_history").select("scan_id").gte("created_at", today).execute()
        scans_today  = len(scans_result.data or [])

        flagged_result = supabase.table("scan_history").select("scan_id").eq("verdict", "DANGEROUS").execute()
        flagged_count  = len(flagged_result.data or [])

        return {
            "total_users":  total_users,
            "scans_today":  scans_today,
            "flagged_links": flagged_count,
            "paid_users":   paid_users
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── List / Search Users ───────────────────────────────────────────────────────

@router.get("/users")
def list_users(q: Optional[str] = Query(None)):
    try:
        query = supabase.table("users").select(
            "id, name, email, role, plan, status, account_status, failed_login_count, profile_id"
        )
        if q:
            query = query.or_(f"name.ilike.%{q}%,email.ilike.%{q}%")
        result = query.order("email").execute()
        users = result.data or []
        # Bulk-fetch profile names
        profile_ids = list({u["profile_id"] for u in users if u.get("profile_id")})
        if profile_ids:
            pr = supabase.table("user_profiles").select("id, name").in_("id", profile_ids).execute()
            profile_map = {p["id"]: p["name"] for p in (pr.data or [])}
            for u in users:
                u["profile_name"] = profile_map.get(u.get("profile_id"))
        return {"users": users}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Create User ───────────────────────────────────────────────────────────────

@router.post("/users")
def create_user(request: CreateUserRequest):
    try:
        response = admin_client.auth.admin.create_user({
            "email":          request.email,
            "password":       request.password,
            "email_confirm":  True
        })
        user_id = str(response.user.id)
        supabase.table("users").insert({
            "id":             user_id,
            "name":           request.name,
            "email":          request.email,
            "role":           request.role,
            "plan":           request.plan,
            "status":         "approved",
            "account_status": "active",
            "failed_login_count": 0
        }).execute()
        result = supabase.table("users").select("*").eq("id", user_id).single().execute()
        log_admin_action("User Created", target_type="user", target_id=user_id, target_email=request.email)
        return result.data
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# ── Get Single User ───────────────────────────────────────────────────────────

@router.get("/users/{user_id}")
def get_user(user_id: str):
    try:
        result = supabase.table("users").select("*").eq("id", user_id).single().execute()
        if not result.data:
            raise HTTPException(status_code=404, detail="User not found")
        user = result.data
        # Attach profile_name if a profile is assigned
        if user.get("profile_id"):
            pr = supabase.table("user_profiles").select("name").eq("id", user["profile_id"]).single().execute()
            user["profile_name"] = pr.data.get("name") if pr.data else None
        return user
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Update User ───────────────────────────────────────────────────────────────

@router.put("/users/{user_id}")
def update_user(user_id: str, request: UpdateUserRequest):
    try:
        update_data = {k: v for k, v in request.dict().items() if v is not None}
        if not update_data:
            raise HTTPException(status_code=400, detail="No fields to update")
        supabase.table("users").update(update_data).eq("id", user_id).execute()
        result = supabase.table("users").select("*").eq("id", user_id).single().execute()
        return result.data
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Suspend ───────────────────────────────────────────────────────────────────

@router.post("/users/{user_id}/suspend")
def suspend_user(user_id: str):
    try:
        supabase.table("users").update({"account_status": "suspended"}).eq("id", user_id).execute()
        log_admin_action("User Suspended", target_type="user", target_id=user_id)
        return {"message": "User suspended successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Reactivate ────────────────────────────────────────────────────────────────

@router.post("/users/{user_id}/reactivate")
def reactivate_user(user_id: str):
    try:
        supabase.table("users").update({"account_status": "active"}).eq("id", user_id).execute()
        log_admin_action("User Reactivated", target_type="user", target_id=user_id)
        return {"message": "User reactivated successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Lock ──────────────────────────────────────────────────────────────────────

@router.post("/users/{user_id}/lock")
def lock_user(user_id: str):
    try:
        supabase.table("users").update({"account_status": "locked"}).eq("id", user_id).execute()
        log_admin_action("User Locked", target_type="user", target_id=user_id)
        return {"message": "User locked successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Unlock ────────────────────────────────────────────────────────────────────

@router.post("/users/{user_id}/unlock")
def unlock_user(user_id: str):
    try:
        supabase.table("users").update({
            "account_status":     "active",
            "failed_login_count": 0
        }).eq("id", user_id).execute()
        log_admin_action("User Unlocked", target_type="user", target_id=user_id)
        return {"message": "User unlocked successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Approve (pending → approved) ─────────────────────────────────────────────

@router.post("/users/{user_id}/approve")
def approve_user(user_id: str):
    try:
        supabase.table("users").update({"status": "approved"}).eq("id", user_id).execute()
        log_admin_action("User Approved", target_type="user", target_id=user_id)
        return {"message": "User approved successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Reject (pending → rejected) ───────────────────────────────────────────────

@router.post("/users/{user_id}/reject")
def reject_user(user_id: str):
    try:
        supabase.table("users").update({"status": "rejected"}).eq("id", user_id).execute()
        log_admin_action("User Rejected", target_type="user", target_id=user_id)
        return {"message": "User rejected"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ═══════════════════════════════════════════════════════════════════════════════
# USER PROFILES
# ═══════════════════════════════════════════════════════════════════════════════

# ── List / Search Profiles ────────────────────────────────────────────────────

@router.get("/profiles")
def list_profiles(q: Optional[str] = Query(None)):
    try:
        query = supabase.table("user_profiles").select("*")
        if q:
            query = query.ilike("name", f"%{q}%")
        result = query.order("name").execute()
        return {"profiles": result.data or []}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Create Profile ────────────────────────────────────────────────────────────

@router.post("/profiles")
def create_profile(request: CreateProfileRequest):
    try:
        result = supabase.table("user_profiles").insert({
            "name":        request.name,
            "description": request.description,
            "permissions": request.permissions,
            "status":      "active"
        }).execute()
        return result.data[0] if result.data else {}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# ── Get Profile ───────────────────────────────────────────────────────────────

@router.get("/profiles/{profile_id}")
def get_profile(profile_id: str):
    try:
        result = supabase.table("user_profiles").select("*").eq("id", profile_id).single().execute()
        if not result.data:
            raise HTTPException(status_code=404, detail="Profile not found")
        # Also fetch users assigned to this profile
        users = supabase.table("users").select("id, name, email, role").eq("profile_id", profile_id).execute()
        return {**result.data, "assigned_users": users.data or []}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Update Profile ────────────────────────────────────────────────────────────

@router.put("/profiles/{profile_id}")
def update_profile(profile_id: str, request: UpdateProfileRequest):
    try:
        update_data = {k: v for k, v in request.dict().items() if v is not None}
        if not update_data:
            raise HTTPException(status_code=400, detail="No fields to update")
        supabase.table("user_profiles").update(update_data).eq("id", profile_id).execute()
        result = supabase.table("user_profiles").select("*").eq("id", profile_id).single().execute()
        return result.data
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Suspend Profile ───────────────────────────────────────────────────────────

@router.post("/profiles/{profile_id}/suspend")
def suspend_profile(profile_id: str):
    try:
        supabase.table("user_profiles").update({"status": "suspended"}).eq("id", profile_id).execute()
        log_admin_action("Profile Suspended", target_type="profile", target_id=profile_id)
        return {"message": "Profile suspended. New assignments are blocked."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Reactivate Profile ────────────────────────────────────────────────────────

@router.post("/profiles/{profile_id}/reactivate")
def reactivate_profile(profile_id: str):
    try:
        supabase.table("user_profiles").update({"status": "active"}).eq("id", profile_id).execute()
        log_admin_action("Profile Reactivated", target_type="profile", target_id=profile_id)
        return {"message": "Profile reactivated successfully."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Assign Profile to User ────────────────────────────────────────────────────

@router.post("/users/{user_id}/assign-profile")
def assign_profile(user_id: str, request: AssignProfileRequest):
    try:
        # Check profile exists and is active
        profile = supabase.table("user_profiles").select("id, name, status").eq("id", request.profile_id).single().execute()
        if not profile.data:
            raise HTTPException(status_code=404, detail="Profile not found.")
        if profile.data.get("status") == "suspended":
            raise HTTPException(status_code=400, detail="This profile is suspended and cannot be assigned. Reactivate it first.")
        supabase.table("users").update({"profile_id": request.profile_id}).eq("id", user_id).execute()
        log_admin_action("Profile Assigned", target_type="user", target_id=user_id, details=f"Profile: {profile.data['name']}")
        return {"message": f"Profile '{profile.data['name']}' assigned successfully."}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Remove Profile from User ──────────────────────────────────────────────────

@router.delete("/users/{user_id}/assign-profile")
def remove_profile(user_id: str):
    try:
        supabase.table("users").update({"profile_id": None}).eq("id", user_id).execute()
        return {"message": "Profile removed from user."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Security Monitor ──────────────────────────────────────────────────────────

@router.get("/security")
def security_monitor():
    try:
        result = supabase.table("users").select(
            "id, name, email, role, plan, status, account_status, failed_login_count"
        ).or_("account_status.eq.locked,failed_login_count.gt.0").order("failed_login_count", desc=True).execute()
        return {"users": result.data or []}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Scan Records ──────────────────────────────────────────────────────────────

@router.get("/scans")
def list_scan_records(verdict: Optional[str] = Query(None)):
    try:
        query = supabase.table("scan_history").select(
            "scan_id, url, verdict, risk_level, threat_categories, created_at, user_id"
        )
        if verdict:
            query = query.eq("verdict", verdict.upper())
        result = query.order("created_at", desc=True).limit(200).execute()
        records = result.data or []
        # Batch-fetch user emails
        user_ids = list({r["user_id"] for r in records if r.get("user_id")})
        if user_ids:
            ur = supabase.table("users").select("id, email").in_("id", user_ids).execute()
            user_map = {u["id"]: u["email"] for u in (ur.data or [])}
            for r in records:
                r["user_email"] = user_map.get(r.get("user_id"))
        return {"records": records, "total": len(records)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Flagged Links ─────────────────────────────────────────────────────────────

@router.get("/flagged")
def list_flagged_links(verdict: Optional[str] = Query(None)):
    try:
        query = supabase.table("scan_history").select(
            "scan_id, url, verdict, risk_level, threat_categories, created_at, user_id"
        )
        if verdict:
            query = query.eq("verdict", verdict.upper())
        else:
            query = query.in_("verdict", ["DANGEROUS", "SUSPICIOUS"])
        result = query.order("created_at", desc=True).limit(200).execute()
        records = result.data or []
        user_ids = list({r["user_id"] for r in records if r.get("user_id")})
        if user_ids:
            ur = supabase.table("users").select("id, email").in_("id", user_ids).execute()
            user_map = {u["id"]: u["email"] for u in (ur.data or [])}
            for r in records:
                r["user_email"] = user_map.get(r.get("user_id"))
        return {"records": records, "total": len(records)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Audit Log ─────────────────────────────────────────────────────────────────

def log_admin_action(action: str, target_type: str = None, target_id: str = None,
                     target_email: str = None, details: str = None):
    """Silently log an admin action to the audit log."""
    try:
        supabase.table("admin_audit_log").insert({
            "action":       action,
            "target_type":  target_type,
            "target_id":    target_id,
            "target_email": target_email,
            "details":      details
        }).execute()
    except Exception:
        pass

@router.get("/audit")
def get_audit_log():
    try:
        result = supabase.table("admin_audit_log").select("*").order("created_at", desc=True).limit(500).execute()
        return {"entries": result.data or []}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ── Subscription Management ───────────────────────────────────────────────────

@router.get("/subscriptions")
def get_subscriptions():
    try:
        result = supabase.table("users").select("id, name, email, plan").order("plan").execute()
        users = result.data or []
        stats = {
            "total":    len(users),
            "free":     sum(1 for u in users if u.get("plan", "free").lower() == "free"),
            "standard": sum(1 for u in users if u.get("plan", "free").lower() == "standard"),
            "premium":  sum(1 for u in users if u.get("plan", "free").lower() == "premium"),
        }
        return {"stats": stats, "users": users}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
