# routers/saved_links_router.py

import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, HTTPException, Query
from schemas import SaveLinkRequest, SavedLinkItem, SavedLinksResponse
from database import supabase
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
