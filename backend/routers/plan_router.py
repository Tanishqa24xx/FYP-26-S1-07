# routers/plan_router.py

from fastapi import APIRouter, Query
from fastapi.responses import JSONResponse
from schemas import PlanInfo, UserPlanResponse, UpgradePlanRequest
from database import supabase
from datetime import date

router = APIRouter()

PLAN_CATALOGUE = [
    PlanInfo(name="Free", price="$0/month", scan_limit="5 scans/day", features=["Manual URL scanning","Camera OCR scanning","Basic Risk Level Classification","Save Important Links","Standard Security Analysis","Sandbox Environment","Last 5 scans in History"]),
    PlanInfo(name="Standard", price="$4.99/month", scan_limit="Unlimited scans", features=["Manual URL scanning","Camera OCR scanning","Unlimited scans","Detailed Risk Level Classification","Save Important Links","Detailed Security Analysis","Alert Threshold Notification","Sandbox Environment","Last 30 days scan history","Export history (CSV + PDF)"]),
    PlanInfo(name="Premium", price="$9.99/month", scan_limit="Unlimited scans", features=["All Standard features","Advanced Multi-layer Security Analysis","Full scan history","Export history (CSV + PDF)","Ad-heavy website warnings","Script & tracker detection"]),
]

def get_plan_details(plan_name: str) -> PlanInfo:
    for p in PLAN_CATALOGUE:
        if p.name.lower() == plan_name.lower():
            return p
    return PLAN_CATALOGUE[0]


@router.get("/")
async def get_my_plan(user_id: str = Query(default="00000000-0000-0000-0000-000000000000")):
    GUEST_ID = "00000000-0000-0000-0000-000000000000"
    scans_today = 0
    daily_limit = 5
    current_plan = "free"

    if user_id != GUEST_ID:
        try:
            # get user plan info
            rows = supabase.table("users") \
                .select("plan, daily_scan_limit") \
                .eq("id", user_id) \
                .execute().data or []

            if rows:
                current_plan = rows[0].get("plan", "free")
                plan_lower = current_plan.lower()
                PLAN_LIMITS = {"free": 5, "standard": 30, "premium": 999999}
                daily_limit = PLAN_LIMITS.get(plan_lower, 5)

            # Count today's scans from scan_records using UTC midnight
            from datetime import datetime, timezone
            today_start = datetime.now(timezone.utc).replace(
                hour=0, minute=0, second=0, microsecond=0
            ).isoformat()
            count_result = supabase.table("scan_records") \
                .select("id", count="exact") \
                .eq("user_id", user_id) \
                .gte("created_at", today_start) \
                .execute()

            scans_today = count_result.count if count_result.count is not None else 0

        except Exception:
            pass

    response = UserPlanResponse(
        current_plan = current_plan,
        scans_today = scans_today,
        daily_limit = daily_limit,
        plan_details = get_plan_details(current_plan)
    )
    return JSONResponse(content=response.model_dump(by_alias=True))


@router.get("/all")
async def get_all_plans():
    return {"plans": [p.model_dump() for p in PLAN_CATALOGUE]}


@router.post("/upgrade")
async def upgrade_plan(
        user_id: str = Query(default="00000000-0000-0000-0000-000000000000"),
        body: UpgradePlanRequest = None
):
    new_plan = body.new_plan if body else "standard"
    valid_plans = {"free", "standard", "premium"}
    if new_plan.lower() not in valid_plans:
        return {"message": f"Invalid plan '{new_plan}'", "new_plan": new_plan}
    try:
        if user_id != "00000000-0000-0000-0000-000000000000":
            supabase.table("users").update({"plan": new_plan.lower()}).eq("id", user_id).execute()
        return {"message": f"Plan upgraded to {new_plan}", "new_plan": new_plan}
    except Exception as e:
        return {"message": str(e), "new_plan": new_plan}