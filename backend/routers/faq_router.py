# routers/faq_router.py

from fastapi import APIRouter, HTTPException
from database import supabase

router = APIRouter(prefix="/faq", tags=["FAQ"])

# Grabs all the active help questions from Supabase and sorts them by our custom order.
@router.get("/")
def get_faqs():
    try:
        result = supabase.table("help_faqs") \
            .select("question, answer, category, sort_order") \
            .eq("is_active", True) \
            .order("sort_order") \
            .execute()
        return result.data or []
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
