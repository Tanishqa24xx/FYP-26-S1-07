-- Audit log table
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    action text NOT NULL,
    target_type text,
    target_id text,
    target_email text,
    details text,
    admin_email text,
    created_at timestamptz DEFAULT now()
);

-- Platform Manager: subscription plans table
CREATE TABLE IF NOT EXISTS pm_subscription_plans (
    id          uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    name        text NOT NULL,
    description text,
    price       numeric(10,2) DEFAULT 0,
    scan_limit  int DEFAULT 10,
    features    text[],
    status      text DEFAULT 'active',
    created_at  timestamptz DEFAULT now(),
    updated_at  timestamptz DEFAULT now()
);

-- Seed with default plans
INSERT INTO pm_subscription_plans (name, description, price, scan_limit, features, status)
VALUES
  ('Free',     'Basic plan for individual users',  0.00,  5,  ARRAY['Manual URL scanning','Camera OCR scanning','Basic Risk Classification','Save Links','Last 5 scan history'], 'active'),
  ('Standard', 'For power users needing more',     4.99,  0,  ARRAY['All Free features','Unlimited scans','VirusTotal scan','Alert Notifications','Last 30 days history'],     'active'),
  ('Premium',  'Full-featured professional plan',  9.99,  0,  ARRAY['All Standard features','Advanced Multi-layer Analysis','Priority scanning','Full scan history','Export reports'], 'active')
ON CONFLICT DO NOTHING;

-- Support requests table
CREATE TABLE IF NOT EXISTS support_requests (
    id         uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    uuid,
    user_email text,
    subject    text NOT NULL,
    message    text NOT NULL,
    status     text DEFAULT 'open',
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

-- Support replies table
CREATE TABLE IF NOT EXISTS support_replies (
    id           uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    request_id   uuid REFERENCES support_requests(id) ON DELETE CASCADE,
    message      text NOT NULL,
    sender_type  text DEFAULT 'platform_manager',
    sender_email text,
    created_at   timestamptz DEFAULT now()
);

-- System alerts table
CREATE TABLE IF NOT EXISTS system_alerts (
    id          uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    type        text NOT NULL,
    message     text NOT NULL,
    severity    text DEFAULT 'warning',
    resolved    boolean DEFAULT false,
    created_at  timestamptz DEFAULT now(),
    resolved_at timestamptz
);

-- Ensure help_faqs has id column (it likely already does)
-- ALTER TABLE help_faqs ADD COLUMN IF NOT EXISTS id uuid DEFAULT gen_random_uuid();
-- ALTER TABLE help_faqs ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT now();
