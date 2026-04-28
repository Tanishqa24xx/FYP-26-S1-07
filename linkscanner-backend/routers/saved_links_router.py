# routers/saved_links_router.py

import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, HTTPException, Query
from schemas import SaveLinkRequest, SavedLinkItem, SavedLinksResponse, RescanResponse
from database import supabase
from services.scan_service import perform_scan
from typing import List

router = APIRouter(prefix="/saved-links", tags=["Saved Links"])


@router.post("/")
def save_link(body: SaveLinkRequest):
    try:
        existing = supabase.table("saved_links") \
            .select("id") \
            .eq("user_id", body.user_id) \
            .eq("url", body.url) \
            .execute()

        if existing.data:
            supabase.table("saved_links") \
                .update({
                    "last_checked_at": datetime.now(timezone.utc).isoformat(),
                    "type": body.risk_level or "UNKNOWN",
                }) \
                .eq("user_id", body.user_id) \
                .eq("url", body.url) \
                .execute()
            return {"message": "Link updated", "already_saved": True}

        link_id = str(uuid.uuid4())
        supabase.table("saved_links").insert({
            "id": link_id,
            "user_id": body.user_id,
            "scan_id": body.scan_id,
            "url": body.url,
            "last_checked_at": datetime.now(timezone.utc).isoformat(),
            "type": body.risk_level or "UNKNOWN",
        }).execute()

        return {"message": "Link saved", "id": link_id}

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{user_id}", response_model=SavedLinksResponse)
def get_saved_links(user_id: str):
    try:
        result = supabase.table("saved_links") \
            .select("id, url, type, last_checked_at") \
            .eq("user_id", user_id) \
            .order("last_checked_at", desc=True) \
            .execute()

        links = [
            SavedLinkItem(
                id              = row["id"],
                url             = row["url"],
                risk_level      = row.get("type"),
                last_checked_at = row.get("last_checked_at"),
            )
            for row in (result.data or [])
        ]
        return SavedLinksResponse(links=links)

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/delete")
def delete_links(ids: List[str]):
    try:
        supabase.table("saved_links") \
            .delete() \
            .in_("id", ids) \
            .execute()
        return {"message": f"Deleted {len(ids)} link(s)"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/rescan", response_model=RescanResponse)
async def rescan_saved_links(
    user_id: str        = Query(...),
    force: bool         = Query(default=False),
    selected_ids: List[str] = Query(default=[])
):
    """
    Rescans saved links with quota awareness.
    selected_ids: if provided, only those links are rescanned.
    force: if True, proceed even if quota is insufficient (scans up to remaining).
    Oldest-first ordering so quota is spent on most stale links.
    """
    try:
        GUEST_ID       = "00000000-0000-0000-0000-000000000000"
        FREE_DAILY_LIMIT = 5
        plan      = "free"
        remaining = FREE_DAILY_LIMIT

        if user_id != GUEST_ID:
            try:
                user_result = supabase.table("users") \
                    .select("plan, daily_scan_limit") \
                    .eq("id", user_id) \
                    .single() \
                    .execute()
                if user_result.data:
                    plan        = user_result.data.get("plan", "free")
                    daily_limit = user_result.data.get("daily_scan_limit") or FREE_DAILY_LIMIT
                    today_start = datetime.now(timezone.utc).replace(
                        hour=0, minute=0, second=0, microsecond=0
                    ).isoformat()
                    count_result = supabase.table("scan_records") \
                        .select("id", count="exact") \
                        .eq("user_id", user_id) \
                        .gte("created_at", today_start) \
                        .execute()
                    scans_today = count_result.count if count_result.count is not None else 0
                    remaining = max(daily_limit - scans_today, 0) if plan == "free" else 999999
            except Exception as e:
                print(f"[rescan] Quota check error: {e}", flush=True)

        # Fetch links — oldest first so quota is spent on most stale
        query = supabase.table("saved_links") \
            .select("id, url, type") \
            .eq("user_id", user_id) \
            .order("last_checked_at", desc=False)
        if selected_ids:
            query = query.in_("id", selected_ids)
        result = query.execute()

        links = result.data or []
        if not links:
            return RescanResponse(message="No saved links to rescan.", total=0)

        # Block if free user has 0 remaining
        if plan == "free" and remaining == 0:
            return RescanResponse(
                message="No scans remaining today. Your quota resets at midnight UTC.",
                quota_warning=True,
                remaining=0,
                total=len(links),
            )

        # Warn free users if quota insufficient and not forced
        if plan == "free" and remaining < len(links) and not force:
            return RescanResponse(
                message=f"Only {remaining} scan(s) remaining today but you have {len(links)} link(s) to check. Proceed to scan the oldest {remaining}?",
                quota_warning=True,
                remaining=remaining,
                total=len(links),
            )

        updated      = 0
        scanned      = 0
        quota_skipped = 0
        error_skipped = 0

        for link in links:
            if plan == "free" and scanned >= remaining:
                quota_skipped = len(links) - scanned
                break
            try:
                scan_result = await perform_scan(link["url"])
                new_verdict = scan_result.get("verdict", "UNKNOWN")

                supabase.table("saved_links").update({
                    "type":            new_verdict,
                    "last_checked_at": datetime.now(timezone.utc).isoformat(),
                }).eq("id", link["id"]).execute()

                supabase.table("scan_records").insert({
                    "id":               str(uuid.uuid4()),
                    "user_id":          user_id,
                    "scanned_url":      link["url"],
                    "verdict":          new_verdict,
                    "scan_source":      "manual",
                    "threat_categories": scan_result.get("threat_categories", []),
                    "blacklist_match":  False,
                }).execute()

                if new_verdict != link.get("type"):
                    updated += 1
                scanned += 1

            except Exception as e:
                print(f"[rescan] Error scanning {link['url']}: {e}", flush=True)
                error_skipped += 1
                continue

        msg = f"Rescan complete. {scanned} scanned, {updated} changed verdict."
        if quota_skipped > 0:
            msg += f" {quota_skipped} skipped (daily quota reached)."
        if error_skipped > 0:
            msg += f" {error_skipped} failed."

        return RescanResponse(
            message=msg,
            quota_warning=False,
            total=len(links),
            scanned=scanned,
            updated=updated,
            skipped=quota_skipped + error_skipped,
        )

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
