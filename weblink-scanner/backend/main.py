# backend/main.py
# Weblink Scanner — FastAPI Application Entry Point

from fastapi import FastAPI                           # FastAPI framework to build APIs
from fastapi.middleware.cors import CORSMiddleware    # CORS (Cross Origin Resource Sharing) allows app to call this backend
import uvicorn                                        # server runner

from database import get_profile, add_scan, get_scans

# =====================================
# Create FastAPI application instance
# =====================================
app = FastAPI(
    title="Weblink Scanner API",
    description="Backend service for scanning and storing URL results.",
    version="1.0.0",
)

# =====================================
# Enable CORS for Android app requests
# adds middleware to FastAPI pipeline
# =====================================
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],    # allow all origins
    allow_credentials=True, # allows cookies or authentication tokens
    allow_methods=["*"],    # allow all HTTP methods: GET, POST, PUT, DELETE
    allow_headers=["*"],    # allows all request headers
)

# =====================================
# Root endpoint
# =====================================
@app.get("/")
async def root():
    return {"status": "running", "service": "Weblink Scanner API", "version": "1.0.0"}
  
# =====================================
# Health check endpoint
# =====================================
@app.get("/health")
async def health():
    return {"status": "healthy"}

# =====================================
# Get user profile
# =====================================
@app.get("/profile/{user_id}")
async def profile(user_id: str):
    profile_data = get_profile(user_id)
    return {"profile": profile_data}

# =====================================
# Add Scan result
# =====================================
@app.post("/scan")
async def scan(user_id: str, url: str):
    result = add_scan(user_id, url)
    return {"message": "Scan saved", "data": result}

# =====================================
# Get Scan History
# =====================================
@app.get("/scans/{user_id}")
async def scans(user_id: str):
    results = get_scans(user_id)
    return {"scans": results}

# ============================================================
# Run server
# uvicorn.run --> starts FastAPI server
# "main:app" --> file: main.py, object: app
# host="0.0.0.0" --> allows access from any network interface
# port=8000 --> server runs at http://localhost:8000
# reload=True --> auto reloads server when code changes
# ============================================================
if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)




