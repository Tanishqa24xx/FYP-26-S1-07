# routers/saved_links_router.py

import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, HTTPException, Query
from schemas import SaveLinkRequest, SavedLinkItem, SavedLinksResponse
from database import supabase
from typing import List
from pydantic import BaseModel

router = APIRouter(prefix="/saved-links", tags=["Saved Links"])


# --- Request / Response models for recheck ---

class RecheckUrlItem(BaseModel):
    id: str
    url: str

class RecheckRequest(BaseModel):
    user_id: str
    links: List[RecheckUrlItem]

class RecheckResultItem(BaseModel):
    id: str
    url: str
    new_risk_level: str
    last_checked_at: str

class RecheckResponse(BaseModel):
    results: List[RecheckResultItem]
    errors: List[str] = []


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
                id = row["id"],
                url = row["url"],
                risk_level = row.get("type"),
                last_checked_at = row.get("last_checked_at"),
            )
            for row in (result.data or [])
        ]
        return SavedLinksResponse(links=links)

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# Use POST for delete to avoid Retrofit @Body + @DELETE issue
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


# --- Re-check endpoint (re-scans each URL) ---

@router.post("/recheck", response_model=RecheckResponse)
async def recheck_links(body: RecheckRequest):
    """
    Re-runs perform_scan() on each saved link URL.
    Updates risk_level (type) and last_checked_at in the saved_links table.
    Returns each link's new verdict so the Android app can update its UI
    immediately without a second GET request.
    """
    from services.scan_service import perform_scan

    results: List[RecheckResultItem] = []
    errors: List[str] = []
    now = datetime.now(timezone.utc).isoformat()

    for item in body.links:
        try:
            scan_result = await perform_scan(item.url)
            new_verdict = scan_result.get("verdict", "UNKNOWN")

            # Update Supabase saved_links row
            supabase.table("saved_links").update({
                "type": new_verdict,
                "last_checked_at": now,
            }).eq("id", item.id).execute()

            results.append(RecheckResultItem(
                id = item.id,
                url = item.url,
                new_risk_level = new_verdict,
                last_checked_at = now,
            ))

        except Exception as e:
            errors.append(f"{item.url}: {str(e)[:80]}")

    return RecheckResponse(results=results, errors=errors)