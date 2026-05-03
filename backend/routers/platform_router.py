#routers/platform_router.py

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import Optional
from database import supabase
from supabase import create_client
from config import settings
from datetime import datetime, timezone


# helper to safely get one row without using .single() which throws in supabase-py v2
def fetch_one(table: str, **eq_filters):
    query = supabase.table(table).select("*")
    for col, val in eq_filters.items():
        query = query.eq(col, val)
    rows = query.execute().data or []
    return rows[0] if rows else None


router = APIRouter(prefix="/platform")

# service role client needed for admin-level operations
admin_client = create_client(settings.SUPABASE_URL, settings.SUPABASE_SERVICE_KEY)


# models
class CreatePlanRequest(BaseModel):
    name: str
    description: Optional[str] = ""
    price: float = 0.0
    scan_limit: int = 10
    features: list[str] = []

class UpdatePlanRequest(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    scan_limit: Optional[int] = None
    features: Optional[list[str]] = None

class CreateSupportReplyRequest(BaseModel):
    message: str
    sender_email: Optional[str] = None

class UpdateSupportStatusRequest(BaseModel):
    status: str

class CreateUserSupportRequest(BaseModel):
    user_id: str
    email:   str
    subject: str
    message: str

class CreateFaqRequest(BaseModel):
    question:   str
    answer:     str
    category:   Optional[str] = "General"
    sort_order: Optional[int] = 0

class UpdateFaqRequest(BaseModel):
    question:   Optional[str]  = None
    answer:     Optional[str]  = None
    category:   Optional[str]  = None
    sort_order: Optional[int]  = None
    is_active:  Optional[bool] = None


# --- Subscription Plans ---

@router.get("/plans")
def list_pm_plans():
    try:
        result = supabase.table("pm_subscription_plans").select("*").order("price").execute()
        plans = result.data or []
        # attach how many users are on each plan
        users_result = supabase.table("users").select("plan").execute()
        plan_counts = {}
        for u in (users_result.data or []):
            p = (u.get("plan") or "free").lower()
            plan_counts[p] = plan_counts.get(p, 0) + 1
        for plan in plans:
            plan["user_count"] = plan_counts.get(plan.get("name", "").lower(), 0)
        return {"plans": plans}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/plans")
def create_pm_plan(request: CreatePlanRequest):
    try:
        result = supabase.table("pm_subscription_plans").insert({
            "name":        request.name,
            "description": request.description,
            "price":       request.price,
            "scan_limit":  request.scan_limit,
            "features":    request.features,
            "status":      "active"
        }).execute()
        return result.data[0] if result.data else {}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/plans/{plan_id}")
def get_pm_plan(plan_id: str):
    try:
        plan = fetch_one("pm_subscription_plans", id=plan_id)
        if not plan:
            raise HTTPException(status_code=404, detail="Plan not found")
        # use eq() for exact plan name match, not ilike() which does substring search
        users_result = supabase.table("users").select("id, name, email").eq("plan", plan.get("name", "").lower()).execute()
        users = users_result.data or []
        plan["users"] = users
        plan["user_count"] = len(users)
        return plan
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/plans/{plan_id}")
def update_pm_plan(plan_id: str, request: UpdatePlanRequest):
    try:
        update_data = {k: v for k, v in request.dict().items() if v is not None}
        if not update_data:
            raise HTTPException(status_code=400, detail="No fields to update")
        update_data["updated_at"] = datetime.now(timezone.utc).isoformat()
        supabase.table("pm_subscription_plans").update(update_data).eq("id", plan_id).execute()
        return fetch_one("pm_subscription_plans", id=plan_id)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/plans/{plan_id}/suspend")
def suspend_pm_plan(plan_id: str):
    try:
        supabase.table("pm_subscription_plans").update({"status": "suspended"}).eq("id", plan_id).execute()
        return {"message": "Plan suspended successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/plans/{plan_id}/activate")
def activate_pm_plan(plan_id: str):
    try:
        supabase.table("pm_subscription_plans").update({"status": "active"}).eq("id", plan_id).execute()
        return {"message": "Plan activated successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- Analytics ---

@router.get("/analytics/overview")
def analytics_overview():
    try:
        users_result = supabase.table("users").select("id, plan, created_at, account_status").execute()
        users = users_result.data or []
        today       = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        month_start = datetime.now(timezone.utc).strftime("%Y-%m-01")
        scans_result = supabase.table("scan_history").select("scan_id, created_at").execute()
        scans = scans_result.data or []
        plan_distribution = {}
        for u in users:
            p = (u.get("plan") or "free").lower()
            plan_distribution[p] = plan_distribution.get(p, 0) + 1
        return {
            "total_users":       len(users),
            "active_users":      sum(1 for u in users if u.get("account_status") == "active"),
            "total_scans":       len(scans),
            "scans_today":       sum(1 for s in scans if (s.get("created_at") or "").startswith(today)),
            "scans_this_month":  sum(1 for s in scans if (s.get("created_at") or "") >= month_start),
            "new_users_today":   sum(1 for u in users if (u.get("created_at") or "").startswith(today)),
            "plan_distribution": plan_distribution
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/analytics/features")
def analytics_features():
    try:
        scans_data = (supabase.table("scan_history").select("verdict").execute()).data or []
        verdict_counts = {"SAFE": 0, "SUSPICIOUS": 0, "DANGEROUS": 0}
        for s in scans_data:
            v = (s.get("verdict") or "SAFE").upper()
            verdict_counts[v] = verdict_counts.get(v, 0) + 1
        users_data = (supabase.table("users").select("plan").execute()).data or []
        plan_counts: dict = {}
        for u in users_data:
            p = (u.get("plan") or "free").lower()
            plan_counts[p] = plan_counts.get(p, 0) + 1
        return {
            "verdict_breakdown": verdict_counts,
            "plan_distribution": plan_counts,
            "feature_usage":     {"scan_url": len(scans_data)}
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/analytics/reports")
def generate_report(start_date: Optional[str] = Query(None), end_date: Optional[str] = Query(None)):
    try:
        scan_query = supabase.table("scan_history").select("scan_id, verdict, created_at, user_id")
        if start_date:
            scan_query = scan_query.gte("created_at", start_date)
        if end_date:
            scan_query = scan_query.lte("created_at", end_date + "T23:59:59")
        scans = (scan_query.execute()).data or []

        user_query = supabase.table("users").select("id, plan, created_at")
        if start_date:
            user_query = user_query.gte("created_at", start_date)
        if end_date:
            user_query = user_query.lte("created_at", end_date + "T23:59:59")
        users = (user_query.execute()).data or []

        scans_by_date: dict = {}
        for s in scans:
            d = (s.get("created_at") or "")[:10]
            if d:
                scans_by_date[d] = scans_by_date.get(d, 0) + 1

        verdict_counts: dict = {}
        for s in scans:
            v = (s.get("verdict") or "UNKNOWN").upper()
            verdict_counts[v] = verdict_counts.get(v, 0) + 1

        plan_counts: dict = {}
        for u in users:
            p = (u.get("plan") or "free").lower()
            plan_counts[p] = plan_counts.get(p, 0) + 1

        return {
            "period":            {"start": start_date, "end": end_date},
            "total_scans":       len(scans),
            "new_users":         len(users),
            "scans_by_date":     dict(sorted(scans_by_date.items())),
            "verdict_breakdown": verdict_counts,
            "new_users_by_plan": plan_counts
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- Support Requests ---

@router.get("/support")
def list_support(status: Optional[str] = Query(None)):
    try:
        query = supabase.table("support_requests").select("*")
        if status and status != "all":
            query = query.eq("status", status)
        result = query.order("created_at", desc=True).execute()
        return {"requests": result.data or []}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/support/submit")
def submit_support(request: CreateUserSupportRequest):
    try:
        result = supabase.table("support_requests").insert({
            "user_id":    request.user_id,
            "email":      request.email,
            "subject":    request.subject,
            "message":    request.message,
            "status":     "open",
            "created_at": datetime.now(timezone.utc).isoformat(),
            "updated_at": datetime.now(timezone.utc).isoformat()
        }).execute()
        return result.data[0] if result.data else {}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/support/my/{user_id}")
def get_my_support(user_id: str):
    try:
        result = supabase.table("support_requests").select("*") \
            .eq("user_id", user_id) \
            .order("created_at", desc=True) \
            .execute()
        requests = result.data or []
        for req in requests:
            replies = supabase.table("support_replies").select("*") \
                .eq("request_id", req["id"]) \
                .order("created_at") \
                .execute()
            req["replies"] = replies.data or []
        return {"requests": requests}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/support/{request_id}")
def get_support(request_id: str):
    try:
        result = supabase.table("support_requests").select("*").eq("id", request_id).execute()
        rows   = result.data or []
        if not rows:
            raise HTTPException(status_code=404, detail="Support request not found")
        request_data = rows[0]
        replies = supabase.table("support_replies").select("*") \
            .eq("request_id", request_id) \
            .order("created_at") \
            .execute().data or []
        return {**request_data, "replies": replies}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/support/{request_id}/reply")
def reply_support(request_id: str, request: CreateSupportReplyRequest):
    try:
        result = supabase.table("support_replies").insert({
            "request_id":   request_id,
            "message":      request.message,
            "sender_type":  "platform_manager",
            "sender_email": request.sender_email or "support@linkscanner.app"
        }).execute()
        # auto-move status from open -> in_progress when PM first replies
        supabase.table("support_requests").update({
            "status":     "in_progress",
            "updated_at": datetime.now(timezone.utc).isoformat()
        }).eq("id", request_id).eq("status", "open").execute()
        return result.data[0] if result.data else {}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/support/{request_id}/user-reply")
def user_reply_support(request_id: str, request: CreateSupportReplyRequest):
    try:
        result = supabase.table("support_replies").insert({
            "request_id":   request_id,
            "message":      request.message,
            "sender_type":  "user",
            "sender_email": request.sender_email or ""
        }).execute()
        supabase.table("support_requests").update({
            "updated_at": datetime.now(timezone.utc).isoformat()
        }).eq("id", request_id).execute()
        return result.data[0] if result.data else {}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.put("/support/{request_id}/status")
def update_support_status(request_id: str, request: UpdateSupportStatusRequest):
    try:
        supabase.table("support_requests").update({
            "status":     request.status,
            "updated_at": datetime.now(timezone.utc).isoformat()
        }).eq("id", request_id).execute()
        return {"message": f"Status updated to {request.status}"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- FAQ Management ---

@router.get("/faq")
def list_pm_faq():
    try:
        result = supabase.table("help_faqs").select("*").order("sort_order").execute()
        return {"faqs": result.data or []}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/faq")
def create_pm_faq(request: CreateFaqRequest):
    try:
        result = supabase.table("help_faqs").insert({
            "question":   request.question,
            "answer":     request.answer,
            "category":   request.category,
            "sort_order": request.sort_order,
            "is_active":  True
        }).execute()
        return result.data[0] if result.data else {}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.put("/faq/{faq_id}")
def update_pm_faq(faq_id: str, request: UpdateFaqRequest):
    try:
        update_data = {k: v for k, v in request.dict().items() if v is not None}
        if not update_data:
            raise HTTPException(status_code=400, detail="No fields to update")
        supabase.table("help_faqs").update(update_data).eq("id", faq_id).execute()
        return fetch_one("help_faqs", id=faq_id)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/faq/{faq_id}")
def delete_pm_faq(faq_id: str):
    try:
        supabase.table("help_faqs").delete().eq("id", faq_id).execute()
        return {"message": "FAQ deleted successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- System Health ---

@router.get("/health")
def system_health():
    try:
        db_ok = True
        db_start = datetime.now(timezone.utc)
        db_ms = -1
        try:
            supabase.table("users").select("id").limit(1).execute()
            db_ms = int((datetime.now(timezone.utc) - db_start).total_seconds() * 1000)
        except Exception:
            db_ok = False

        scan_ok = True
        try:
            supabase.table("scan_history").select("scan_id").limit(1).execute()
        except Exception:
            scan_ok = False

        alerts = []
        try:
            alerts_result = supabase.table("system_alerts").select("*") \
                .eq("resolved", False) \
                .order("created_at", desc=True) \
                .execute()
            alerts = alerts_result.data or []
        except Exception:
            pass

        critical_count = sum(1 for a in alerts if a.get("severity") == "critical")
        overall = "healthy" if (db_ok and scan_ok and len(alerts) == 0) else (
            "critical" if critical_count > 0 else "degraded"
        )

        return {
            "overall_status": overall,
            "services": {
                "database":     {"status": "healthy" if db_ok    else "down", "response_ms": db_ms},
                "scan_service": {"status": "healthy" if scan_ok  else "down", "response_ms": None},
                "auth_service": {"status": "healthy", "response_ms": None},
                "api":          {"status": "healthy", "response_ms": None}
            },
            "active_alerts": alerts,
            "alert_count":   len(alerts)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/health/alerts/{alert_id}/resolve")
def resolve_alert(alert_id: str):
    try:
        supabase.table("system_alerts").update({
            "resolved":    True,
            "resolved_at": datetime.now(timezone.utc).isoformat()
        }).eq("id", alert_id).execute()
        return {"message": "Alert resolved"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
