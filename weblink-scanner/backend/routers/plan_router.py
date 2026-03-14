# WeblinkScanner\backend\routers\plan_router.py

from fastapi import APIRouter
from schemas import PlanInfo

router = APIRouter()

# plan storage
current_plan = "free"

PLAN_CATALOGUE = [
    PlanInfo(
        name="Free",
        price="$0/month",
        scan_limit="5 scans/day",
        features=[
            "Manual URL scanning",
            "Camera OCR scanning",
            "Basic Risk Level Classification",
            "Save Important Links",
            "Standard Security Analysis",
            "Sandbox Environment",
            "Last 5 scans in History"
        ]
    ),
    PlanInfo(
        name="Standard",
        price="$4.99/month",
        scan_limit="unlimited scans",
        features=[
            "Manual URL scanning",
            "Camera OCR scanning",
            "VirusTotal scan",
            "Detailed Risk Level Classification",
            "Save Important Links",
            "Detailed Security Analysis",
            "Alert Threshold Notification",
            "Risk Confidence Score",
            "Sandbox Environment",
            "Last 30 days scan history",
            "Export Weekly reports"
        ]
    ),
    PlanInfo(
        name="Premium",
        price="$9.99/month",
        scan_limit="Unlimited scans",
        features=[
            "Manual URL scanning",
            "Camera OCR scanning",
            "VirusTotal scan",
            "Advanced Risk Level Classification",
            "Save Important Links",
            "Advanced Multi-layer Security Analysis",
            "Alert Threshold Notification",
            "Risk Confidence Score",
            "Sandbox Environment with Advanced sandbox report",
            "Priority scanning",
            "Full scan history",
            "Export all/any reports",
            "Warning about ad-intensive websites",
            "Highlight Suspicious weblinks"
        ]
    )
]

@router.get("/")
async def get_my_plan():
    return {
        "currentPlan": current_plan,
        "scansToday": 3,
        "dailyLimit": 5,
        "planDetails": PLAN_CATALOGUE[0]
    }

@router.get("/all")
async def get_all_plans():
    return {
        "plans": PLAN_CATALOGUE
    }

@router.post("/upgrade")
async def upgrade_plan(plan: str):
    global current_plan
    current_plan = plan
    return {
        "message": f"Plan upgraded to {plan}"
    }
