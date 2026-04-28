// Supabase Edge Function — handles approve/reject links from signup emails.
// Always available, no local server needed.
// Deploy: supabase functions deploy approve --no-verify-jwt

const SUPABASE_URL = "https://gcpqarrvkcizefszmyxi.supabase.co"
const SERVICE_KEY  = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdjcHFhcnJ2a2NpemVmc3pteXhpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTgwOTIyNiwiZXhwIjoyMDg3Mzg1MjI2fQ.7opGKn-G_CqP4hJXPZYROYp2rt0Bp9x-P5EWMnRGflc"

// ── Icons as HTML entities — avoids any encoding corruption on deploy ─────────
const ICON_OK      = "&#x2705;" // ✅
const ICON_REJECT  = "&#x274C;" // ❌
const ICON_UNKNOWN = "&#x2753;" // ❓
const ICON_WARN    = "&#x26A0;" // ⚠

// ── HTML page builder ─────────────────────────────────────────────────────────
function page(icon: string, title: string, badge: string, body: string, color: string): Response {
  const html = [
    "<!DOCTYPE html>",
    '<html lang="en">',
    "<head>",
    '  <meta charset="UTF-8"/>',
    '  <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>',
    '  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>',
    `  <title>${title} - LinkScanner</title>`,
    "  <style>",
    "    *{box-sizing:border-box;margin:0;padding:0}",
    "    body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f0f4ff;min-height:100vh;display:flex;align-items:center;justify-content:center}",
    "    .card{background:#fff;border-radius:16px;box-shadow:0 4px 24px rgba(0,0,0,.10);padding:48px 40px;max-width:460px;width:90%;text-align:center}",
    "    .icon{font-size:56px;margin-bottom:16px}",
    "    h1{font-size:24px;color:#0f172a;margin-bottom:12px}",
    "    p{font-size:15px;color:#64748b;line-height:1.6}",
    `    .badge{display:inline-block;background:${color}18;color:${color};border-radius:8px;padding:4px 14px;font-size:13px;font-weight:600;margin:16px 0}`,
    "    .footer{margin-top:24px;font-size:12px;color:#94a3b8}",
    "  </style>",
    "</head>",
    "<body>",
    '  <div class="card">',
    `    <div class="icon">${icon}</div>`,
    `    <h1>${title}</h1>`,
    `    <div class="badge">${badge}</div>`,
    `    <p>${body}</p>`,
    '    <div class="footer">LinkScanner Admin Panel</div>',
    "  </div>",
    "</body>",
    "</html>",
  ].join("\n")

  return new Response(html, {
    headers: { "Content-Type": "text/html; charset=utf-8" }
  })
}

// ── Handler ───────────────────────────────────────────────────────────────────
Deno.serve(async (req: Request) => {
  const url    = new URL(req.url)
  const userId = url.searchParams.get("user_id")
  const action = url.searchParams.get("action")

  if (!userId || !["approve", "reject"].includes(action ?? "")) {
    return page(ICON_WARN, "Invalid Link", "Error",
      "This approval link is missing required parameters. Please check the email.", "#f59e0b")
  }

  const headers = {
    "Authorization": `Bearer ${SERVICE_KEY}`,
    "apikey": SERVICE_KEY,
    "Content-Type": "application/json",
  }

  // ── 1. Fetch user ─────────────────────────────────────────────────────────
  const getRes = await fetch(
    `${SUPABASE_URL}/rest/v1/users?id=eq.${userId}&select=name,email,role,status`,
    { headers }
  )
  const users = await getRes.json()

  if (!users || users.length === 0) {
    return page(ICON_UNKNOWN, "User Not Found", "Error",
      "No account was found with this ID. It may have been deleted.", "#dc2626")
  }

  const user = users[0]

  // ── 2. Already actioned? ──────────────────────────────────────────────────
  if (user.status !== "pending") {
    const icon  = user.status === "approved" ? ICON_OK : ICON_REJECT
    const color = user.status === "approved" ? "#16a34a" : "#dc2626"
    const label = user.status.charAt(0).toUpperCase() + user.status.slice(1)
    return page(icon, `Already ${label}`, label,
      `<b>${user.name}</b> (${user.email}) was already <b>${user.status}</b>. No further action needed.`,
      color)
  }

  // ── 3. Apply action ───────────────────────────────────────────────────────
  const newStatus = action === "approve" ? "approved" : "rejected"
  const patchRes  = await fetch(
    `${SUPABASE_URL}/rest/v1/users?id=eq.${userId}`,
    { method: "PATCH", headers: { ...headers, "Prefer": "return=minimal" },
      body: JSON.stringify({ status: newStatus }) }
  )

  if (!patchRes.ok && patchRes.status !== 204) {
    const err = await patchRes.text()
    return page(ICON_WARN, "Update Failed", "Error",
      `Could not update the account. (${patchRes.status}: ${err})`, "#dc2626")
  }

  const icon  = action === "approve" ? ICON_OK : ICON_REJECT
  const color = action === "approve" ? "#16a34a" : "#dc2626"
  const label = action === "approve" ? "Approved" : "Rejected"
  const roleLabel = (user.role ?? "user").replace(/_/g, " ").replace(/\b\w/g, (c: string) => c.toUpperCase())
  const msg = action === "approve"
    ? `<b>${user.name}</b> (${user.email}) has been approved as <b>${roleLabel}</b>.<br><br>They can now log in to LinkScanner.`
    : `<b>${user.name}</b> (${user.email}) has been rejected.<br><br>They will see a rejection message when they attempt to log in.`

  return page(icon, `Account ${label}`, label, msg, color)
})
