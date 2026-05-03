#routers/approval_router.py
#-*- coding: utf-8 -*-
from fastapi import APIRouter
from fastapi.responses import HTMLResponse
from database import supabase

router = APIRouter()

# shared CSS for all approval page responses
_CSS = """
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    background: #f0f4ff;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .card {
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 4px 24px rgba(0,0,0,0.10);
    max-width: 480px;
    width: 90%;
    overflow: hidden;
    text-align: center;
  }
  .header {
    background: #1d4ed8;
    color: #fff;
    padding: 28px 32px;
  }
  .header .logo { font-size: 26px; font-weight: 800; margin-bottom: 4px; }
  .header .sub  { font-size: 14px; opacity: 0.85; }
  .body { padding: 28px 32px; }
  .info-box {
    background: #f8fafc;
    border-radius: 12px;
    padding: 16px 20px;
    margin: 18px 0;
    text-align: left;
  }
  .info-row {
    display: flex;
    padding: 8px 0;
    border-bottom: 1px solid #e2e8f0;
    font-size: 14px;
  }
  .info-row:last-child { border-bottom: none; }
  .info-label { width: 70px; color: #64748b; }
  .info-value { font-weight: 600; color: #0f172a; }
  .info-value.role { color: #1d4ed8; }
  .btn-row { display: flex; gap: 12px; margin-top: 20px; }
  .btn {
    flex: 1;
    padding: 14px;
    border-radius: 10px;
    border: none;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    text-decoration: none;
    display: inline-block;
    color: #fff;
  }
  .btn-approve { background: #16a34a; }
  .btn-reject  { background: #dc2626; }
  .result { padding: 40px 32px; }
  .icon   { font-size: 52px; margin-bottom: 14px; }
  h1      { font-size: 22px; color: #0f172a; margin-bottom: 10px; }
  p       { font-size: 15px; color: #64748b; line-height: 1.6; }
  .badge  {
    display: inline-block;
    border-radius: 8px;
    padding: 4px 14px;
    font-size: 13px;
    font-weight: 600;
    margin: 10px 0;
  }
  .footer { padding: 14px; font-size: 12px; color: #94a3b8; background: #f8fafc; }
"""


def _wrap(header_sub: str, inner_html: str) -> HTMLResponse:
    html = (
        "<!DOCTYPE html>"
        '<html lang="en">'
        "<head>"
        '<meta charset="UTF-8"/>'
        '<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>'
        '<meta name="viewport" content="width=device-width, initial-scale=1.0"/>'
        f"<title>Weblink Scanner - {header_sub}</title>"
        f"<style>{_CSS}</style>"
        "</head>"
        "<body><div class='card'>"
        "<div class='header'>"
        "<div class='logo'>&#128279; Weblink Scanner</div>"
        f"<div class='sub'>{header_sub}</div>"
        "</div>"
        f"{inner_html}"
        "<div class='footer'>Weblink Scanner Admin Panel</div>"
        "</div></body></html>"
    )
    return HTMLResponse(content=html, media_type="text/html; charset=utf-8")


def _result(icon: str, title: str, badge: str, badge_color: str, body: str) -> HTMLResponse:
    inner = (
        "<div class='result'>"
        f"<div class='icon'>{icon}</div>"
        f"<h1>{title}</h1>"
        f"<div class='badge' style='background:{badge_color}20;color:{badge_color}'>{badge}</div>"
        f"<p>{body}</p>"
        "</div>"
    )
    return _wrap(title, inner)


@router.get("/approve", response_class=HTMLResponse)
def handle_approval(user_id: str, action: str = ""):
    result = supabase.table("users") \
        .select("name, email, role, status") \
        .eq("id", user_id).execute()
    rows = result.data or []
    if not rows:
        return _result("&#x2753;", "User Not Found", "Error", "#dc2626",
                       "No account was found with this ID. It may have been deleted.")

    user = rows[0]

    name       = user.get("name", "Unknown")
    email      = user.get("email", "")
    role       = user.get("role", "user").replace("_", " ").title()
    cur_status = user.get("status", "approved")

    # if this user was already approved or rejected, just show the current state
    if cur_status != "pending":
        icon    = "&#x2705;" if cur_status == "approved" else "&#x274C;"
        color   = "#16a34a" if cur_status == "approved" else "#dc2626"
        return _result(icon, f"Already {cur_status.title()}", cur_status.title(), color,
                       f"<b>{name}</b> ({email}) was already <b>{cur_status}</b>. No further action needed.")

    # no action param in the URL, so show the confirm page with Approve/Reject buttons
    if action not in ("approve", "reject"):
        inner = (
            "<div class='body'>"
            f"<p style='font-size:15px;color:#0f172a'>A new <strong>{role}</strong> account is awaiting your approval:</p>"
            "<div class='info-box'>"
            f"<div class='info-row'><span class='info-label'>Name</span><span class='info-value'>{name}</span></div>"
            f"<div class='info-row'><span class='info-label'>Email</span><span class='info-value'>{email}</span></div>"
            f"<div class='info-row'><span class='info-label'>Role</span><span class='info-value role'>{role}</span></div>"
            "</div>"
            "<div class='btn-row'>"
            f"<a class='btn btn-approve' href='/approve?user_id={user_id}&action=approve'>&#10003; Approve</a>"
            f"<a class='btn btn-reject'  href='/approve?user_id={user_id}&action=reject'>&#10007; Reject</a>"
            "</div>"
            "</div>"
        )
        return _wrap("Admin Approval Required", inner)

    # apply approve or reject
    new_status = "approved" if action == "approve" else "rejected"
    try:
        supabase.table("users").update({"status": new_status}).eq("id", user_id).execute()
    except Exception as e:
        return _result("&#x26A0;", "Update Failed", "Error", "#dc2626",
                       f"Could not update account status: {e}")

    if action == "approve":
        return _result("&#x2705;", "Account Approved", "Approved", "#16a34a",
                       f"<b>{name}</b> ({email}) has been approved as <b>{role}</b>.<br><br>"
                       f"They can now log in to Weblink Scanner.")
    else:
        return _result("&#x274C;", "Account Rejected", "Rejected", "#dc2626",
                       f"<b>{name}</b> ({email}) has been rejected.<br><br>"
                       f"They will see a rejection message when they attempt to log in.")
