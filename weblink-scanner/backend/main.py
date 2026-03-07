# backend/main.py
# Weblink Scanner — FastAPI Application Entry Point

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from database import get_profile, add_scan, get_scans
