-- ============================================================
-- Weblink Scanner - Supabase PostgreSQL Schema
-- File: sql_schema.sql
-- Run in Supabase Dashboard > SQL Editor
-- ============================================================

-- ============================================================
-- PROFILES TABLE
-- Auto-created on Supabase Auth signup via trigger
-- id: matches user in auth.users
-- UUID: unique identifier used by Supabase Auth
-- ============================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id               BIGINT PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE, 
    email            TEXT UNIQUE NOT NULL,
    display_name     TEXT,
    plan             TEXT NOT NULL DEFAULT 'free' CHECK (plan IN ('free', 'standard', 'premium')),
    scans_today      INTEGER NOT NULL DEFAULT 0,
    daily_scan_limit INTEGER DEFAULT 5,
    last_scan_reset  DATE DEFAULT CURRENT_DATE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SCAN RESULTS TABLE
-- Core table: every URL scan result
-- url: stores scanned link
-- threat_categories: array of threats
-- virustotal_data: stores raw API response
-- google_safe_data: stores google threat results
-- ============================================================
CREATE TABLE IF NOT EXISTS public.scan_results (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    url               TEXT NOT NULL,
    scan_source       TEXT NOT NULL DEFAULT 'manual' CHECK (scan_source IN ('manual', 'camera', 'qr')),
    risk_level        TEXT NOT NULL CHECK (risk_level IN ('safe', 'suspicious', 'dangerous', 'unknown')),
    risk_score        FLOAT DEFAULT 0 CHECK (risk_score >= 0 AND risk_score <= 100),
    threat_categories TEXT[],
    virustotal_data   JSONB,
    google_safe_data  JSONB,
    scan_duration_ms  INTEGER,
    is_saved          BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- QR SCAN LOGS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS public.qr_scan_logs ( 
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scan_result_id    BIGINT REFERENCES scan_results(id) ON DELETE SET NULL,
    raw_qr_data       TEXT NOT NULL,
    extracted_url     TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- CAMERA SCAN LOGS TABLE
-- ocr_confidence: confidence level from OCR engine
-- ============================================================
CREATE TABLE IF NOT EXISTS public.camera_scan_logs (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scan_result_id    BIGINT REFERENCES scan_results(id) ON DELETE SET NULL,
    extracted_text    TEXT,
    extracted_url     TEXT,
    ocr_confidence    FLOAT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SANDBOX ANALYSES TABLE
-- Stores detailed sandbox environment analysis per scan
-- ============================================================
CREATE TABLE IF NOT EXISTS public.sandbox_analyses (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scan_id          BIGINT NOT NULL REFERENCES public.scan_results(id) ON DELETE CASCADE,
    user_id          BIGINT REFERENCES public.profiles(id),
    url              TEXT NOT NULL,
    status_code      INTEGER,
    page_title       TEXT,
    redirect_chain   TEXT[],
    external_links   TEXT[],
    scripts_found    TEXT[],
    forms_found      JSONB,
    ssl_valid        BOOLEAN,
    ssl_issuer       TEXT,
    ssl_expiry       TIMESTAMPTZ,
    ip_address       TEXT,
    hosting_country  TEXT,
    malware_signals  TEXT[],
    phishing_signals TEXT[],
    load_time_ms     INTEGER,
    screenshot_url   TEXT,
    raw_html_snippet TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES (allows fast queries)
-- ============================================================
CREATE INDEX idx_scan_results_user_id ON public.scan_results(user_id);
CREATE INDEX idx_sandbox_scan_id ON public.sandbox_analyses(scan_id);
CREATE INDEX idx_qr_scan_result ON public.qr_scan_logs(scan_result_id);
CREATE INDEX idx_camera_scan_result ON public.camera_scan_logs(scan_result_id);
  
    




