# backend/dependencies.py
# FastAPI dependency functions shared across routers

from fastapi import Depends, HTTPException, status
from datetime import date

from database import get_pool


# -----------------------------------------------------------
# check_scan_quota
# Ensures the user has not exceeded their daily scan limit.
# Resets the counter if the day has rolled over.
# -----------------------------------------------------------

"""
current_user: dict with keys 'user_id', 'plan', etc.
Returns updated current_user with scans_today
Raises HTTPException if limit exceeded
"""
async def check_scan_quota(current_user: dict) -> dict:
    pool = await get_pool()

    async with pool.acquire() as conn:
        profile = await conn.fetchrow(
            """
            SELECT plan, scans_today, daily_scan_limit, last_scan_reset
            FROM public.users
            WHERE id = $1
            """,
            current_user["user_id"],
        )

        if not profile:
            raise HTTPException(status_code=404, detail="User profile not found")

        scans_today = profile["scans_today"] or 0

        # Reset counter if a new day has started
        last_reset = profile["last_scan_reset"]
        if last_reset is None or last_reset < date.today():
            await conn.execute(
                """
                UPDATE public.users
                SET scans_today = 0,
                    last_scan_reset = $1
                WHERE id = $2
                """,
                date.today(),
                current_user["user_id"],
            )
            scans_today = 0

        # Enforce limit only for free plan users
        daily_limit = profile["daily_scan_limit"]
        if profile["plan"] == "free" and daily_limit and scans_today >= daily_limit:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"Daily scan limit of {daily_limit} reached. Upgrade your plan.",
            )

    current_user["scans_today"] = scans_today
    return current_user
