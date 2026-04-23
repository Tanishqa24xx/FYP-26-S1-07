from fastapi import APIRouter
from fastapi.responses import HTMLResponse
from database import supabase

router = APIRouter()


def _page(title: str, icon: str, heading: str, body: str, color: str) -> HTMLResponse:
    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>{title} — LinkScanner</title>
  <style>
    * {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      background: #f0f4ff;
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
    }}
    .card {{
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 24px rgba(0,0,0,0.10);
      padding: 48px 40px;
      max-width: 460px;
      width: 90%;
      text-align: center;
    }}
    .icon  {{ font-size: 56px; margin-bottom: 16px; }}
    h1     {{ font-size: 24px; color: #0f172a; margin-bottom: 12px; }}
    p      {{ font-size: 15px; color: #64748b; line-height: 1.6; }}
    .badge {{
      display: inline-block;
      background: {color}18;
      color: {color};
      border-radius: 8px;
      padding: 4px 14px;
      font-size: 13px;
      font-weight: 600;
      margin: 16px 0;
    }}
    .footer {{ margin-top: 24px; font-size: 12px; color: #94a3b8; }}
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">{icon}</div>
    <h1>{heading}</h1>
    <div class="badge">{title}</div>
    <p>{body}</p>
    <div class="footer">LinkScanner Admin Panel</div>
  </div>
</body>
</html>"""
    return HTMLResponse(content=html)


@router.get("/approve", response_class=HTMLResponse)
def handle_approval(user_id: str, action: str):
    # Fetch user
    try:
        result = supabase.table("users").select("name, email, role, status").eq("id", user_id).single().execute()
    except Exception:
        return _page("Error", "❓", "User Not Found",
                     "No account was found with this ID. It may have been deleted.",
                     "#dc2626")

    user = result.data
    if not user:
        return _page("Error", "❓", "User Not Found",
                     "No account was found with this ID. It may have been deleted.",
                     "#dc2626")

    name       = user.get("name", "Unknown")
    email      = user.get("email", "")
    role       = user.get("role", "user").replace("_", " ").title()
    cur_status = user.get("status", "approved")

    # Already actioned
    if cur_status != "pending":
        icon    = "✅" if cur_status == "approved" else "❌"
        heading = f"Already {cur_status.title()}"
        body    = f"<b>{name}</b> ({email}) was already <b>{cur_status}</b>. No further action needed."
        color   = "#16a34a" if cur_status == "approved" else "#dc2626"
        return _page(cur_status.title(), icon, heading, body, color)

    if action == "approve":
        supabase.table("users").update({"status": "approved"}).eq("id", user_id).execute()
        return _page(
            "Approved", "✅", "Account Approved",
            f"<b>{name}</b> ({email}) has been approved as <b>{role}</b>.<br><br>"
            f"They can now log in to LinkScanner.",
            "#16a34a"
        )

    if action == "reject":
        supabase.table("users").update({"status": "rejected"}).eq("id", user_id).execute()
        return _page(
            "Rejected", "❌", "Account Rejected",
            f"<b>{name}</b> ({email}) has been rejected.<br><br>"
            f"They will see a rejection message when they attempt to log in.",
            "#dc2626"
        )

    return _page("Invalid Action", "⚠️", "Invalid Action",
                 "The action in this link is not recognised. Please check the email link.",
                 "#f59e0b")
