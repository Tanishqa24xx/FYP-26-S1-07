-- ============================================================
-- Weblink Scanner - Triggers, Functions & Row Level Security
-- File: sql_triggers.sql
-- ============================================================

-- ============================================================
-- TRIGGER: Auto-create profile on signup
-- ============================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles(id, email, display_name)
  VALUES (NEW.id, NEW.email, COALESCE(
                               NEW.raw_user_meta_data->>'display_name',
                               split_part(NEW.email, '@', 1)
                             )
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger runs after a new user is inserted in Supabase Auth
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW
EXECUTE FUNCTION public.handle_new_user();

-- ============================================================
-- TRIGGER: Update updated_at timestamp
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS update_profiles_updated_at ON public.profiles;

CREATE TRIGGER update_profiles_updated_at
BEFORE UPDATE ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- TRIGGER: Increment scan counter after each scan
-- ============================================================
CREATE OR REPLACE FUNCTION increment_scan_counter()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.profiles
    SET scans_today = scans_today + 1
    WHERE id = NEW.user_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_increment_scan_counter on public.scan_results;

CREATE TRIGGER trigger_increment_scan_counter
AFTER INSERT ON public.scan_results
FOR EACH ROW
EXECUTE FUNCTION increment_scan_counter();

-- ============================================================
-- ROW LEVEL SECURITY
-- Ensures users only access their own data
-- ============================================================
ALTER TABLE public.profiles          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scan_results      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sandbox_analyses  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.qr_scan_logs      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.camera_scan_logs  ENABLE ROW LEVEL SECURITY;

-- Profiles
CREATE POLICY "profiles_select_own" ON public.profiles
FOR SELECT USING (auth.uid() = id);

CREATE POLICY "profiles_update_own" ON public.profiles
FOR UPDATE USING (auth.uid() = id);

-- Scan Results
CREATE POLICY "scans_select_own" ON public.scan_results
FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "scans_insert_own" ON public.scan_results
FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Sandbox Analyses
CREATE POLICY "sandbox_select_own" ON public.sandbox_analyses
FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "sandbox_insert_own" ON public.sandbox_analyses
FOR INSERT WITH CHECK (auth.uid() = user_id);







