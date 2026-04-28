================================================================================
                        WEBLINK SCANNER — USER GUIDE
                        Complete Guide for All User Roles
================================================================================

TABLE OF CONTENTS
-----------------
  1. What is Weblink Scanner?
  2. Getting Started
       2.1  Creating an Account
       2.2  Logging In
       2.3  Forgot Password
  3. Regular User Guide
       3.1  Home / Dashboard
       3.2  Scan a URL
       3.3  Camera Scan (OCR)
       3.4  QR Code Scan
       3.5  Scan Results & Security Analysis
       3.6  Sandbox Environment
       3.7  Saved Links
       3.8  Scan History
       3.9  My Plan
       3.10 Upgrade Plan
       3.11 Menu
       3.12 Settings
            - Edit Profile
            - Auto Log Out
            - Warning Strictness
            - Help / FAQ
            - Report / Support
            - Delete Account
  4. Subscription Plans
       4.1  Free Plan
       4.2  Standard Plan
       4.3  Premium Plan
  5. Admin Guide
       5.1  Admin Dashboard
       5.2  User Management
       5.3  User Profiles
       5.4  Security Monitor
       5.5  Scan Records
       5.6  Flagged Links
       5.7  Audit Log
       5.8  Subscriptions
       5.9  Settings (Admin)
  6. Platform Manager Guide
       6.1  PM Dashboard
       6.2  Subscription Plans
       6.3  Analytics
       6.4  Reports
       6.5  Support Requests
       6.6  FAQ Management
       6.7  System Health
       6.8  Settings (PM)
  7. Account Approval Process (Admin & PM Accounts)
  8. Technical Information


================================================================================
1. WHAT IS WEBLINK SCANNER?
================================================================================

Weblink Scanner is an Android mobile application that helps users identify
malicious, suspicious, or unsafe web links before clicking them. It uses
multi-layer security analysis to classify URLs by risk level and provides
detailed reports about phishing attempts, malware, SSL certificate issues,
redirects, and more.

The app supports three types of users:
  - Regular Users   : scan links, manage saved links, view history
  - Administrators  : manage users, monitor platform security
  - Platform Managers : manage subscription plans, analytics, and support


================================================================================
2. GETTING STARTED
================================================================================

2.1 CREATING AN ACCOUNT
------------------------
1. Open the app and tap "Sign Up".
2. Enter your full name, email address, and a strong password.
3. Select your role:
     - User           : standard account (approved instantly)
     - Admin          : requires developer approval before login
     - Platform Manager : requires developer approval before login
4. Tap "Create Account".

Note: Admin and Platform Manager accounts are sent to the developer team
for review. You will be notified by email once your account is approved.
You cannot log in until approval is granted.

2.2 LOGGING IN
--------------
1. Open the app and enter your email and password.
2. Tap "Log In".
3. If your account is pending approval, you will see a message saying
   "Your account is pending approval."
4. If your account was rejected, you will see a rejection message.
5. Approved accounts are directed to their role-specific dashboard.

2.3 FORGOT PASSWORD
-------------------
1. On the login screen, tap "Forgot Password?".
2. Enter your registered email address.
3. Check your email for a password reset link.
4. Follow the link to set a new password.
5. Return to the app and log in with your new password.


================================================================================
3. REGULAR USER GUIDE
================================================================================

3.1 HOME / DASHBOARD
---------------------
The home screen shows:
  - A welcome message with your name and current plan.
  - Quick action buttons: Scan URL, Camera Scan, QR Scan.
  - Recent scan results (last few scans at a glance).
  - A "View All" link to your full scan history.
  - Navigation to your plan details.

3.2 SCAN A URL
--------------
1. From the home screen, tap "Scan URL" or the URL scan option.
2. Type or paste a web link into the input field.
3. Tap "Scan".
4. Wait for the analysis to complete (a few seconds).
5. View the result — see Section 3.5 for result details.

Free plan users are limited to 5 scans per day.
Standard and Premium users have unlimited scans.

3.3 CAMERA SCAN (OCR)
---------------------
Use this to scan a URL that appears in a physical document, poster,
screenshot, or image.

1. Tap "Camera Scan" from the home screen.
2. Point your camera at the text containing the URL, or upload an image.
3. The app uses OCR (Optical Character Recognition) to extract the text.
4. Confirm the extracted URL and tap "Scan".
5. View the scan result.

3.4 QR CODE SCAN
----------------
Use this to scan a QR code that contains a web link.

1. Tap "QR Scan" from the home screen.
2. Point your camera at the QR code.
3. The app automatically reads and scans the URL inside the QR code.
4. View the scan result.

3.5 SCAN RESULTS & SECURITY ANALYSIS
--------------------------------------
After a scan completes, the result screen shows:

  RISK LEVEL:
    - Safe       : The link appears safe to visit.
    - Low Risk   : Minor concerns detected; proceed with caution.
    - Medium Risk: Potential threats found; not recommended.
    - High Risk  : Dangerous link; do not visit.
    - Malicious  : Confirmed threat; avoid completely.

  DETAILS SHOWN:
    - Overall risk score
    - Threat categories detected (phishing, malware, suspicious domain, etc.)
    - SSL/TLS certificate status (valid, expired, missing)
    - Redirect chain analysis
    - Domain age and reputation
    - IP address and hosting info
    - Blacklist status

  ACTIONS YOU CAN TAKE:
    - Save Link  : Save this URL to your Saved Links for future reference.
    - Sandbox    : Run a deeper sandboxed analysis (see Section 3.6).
    - Share      : Share the result.
    - Back       : Return to scanning.

WARNING STRICTNESS:
You can adjust how sensitive the app is when flagging warnings.
Go to Settings > Warning Strictness to set Low, Medium, or High sensitivity.

3.6 SANDBOX ENVIRONMENT
------------------------
The Sandbox runs a deeper, isolated analysis of a scanned URL. It is
available after any scan result.

What the Sandbox checks:
  - JavaScript and script behaviour
  - Tracker and ad network detection
  - Redirect chains
  - SSL certificate validity, age, and protocol
  - ASN (Autonomous System Number) and hosting details
  - Page content classification
  - Cookie and privacy policy presence

1. On a scan result screen, tap "Open Sandbox".
2. The sandbox analysis runs and displays a detailed security report.
3. Use this for links that show medium or borderline risk.

3.7 SAVED LINKS
---------------
Save important links so you can re-check them later or track their
safety over time.

  HOW TO SAVE A LINK:
    - After scanning, tap "Save Link" on the result screen.

  MANAGING SAVED LINKS:
    - View all saved links in the "Saved Links" section from the menu.
    - Each saved link shows: URL, risk level, and date saved.
    - Tap a link to see its last scan details.

  RE-CHECK LINKS:
    - Select one or more saved links and tap "Re-check Selected".
    - The app rescans your selected links and updates their risk level.
    - If you have reached your daily scan quota, a warning dialog appears
      giving you the option to skip already-safe links.

  DELETE LINKS:
    - Select links and tap the delete icon to remove them.

3.8 SCAN HISTORY
-----------------
View a complete log of all URLs you have scanned.

  - Access from the menu: Menu > Scan History.
  - Shows URL, risk level, scan date, and scan method.
  - Tap any entry to view the full scan result.
  - Use the search/filter to find specific scans.
  - Select multiple entries and delete them.
  - Export your history to CSV or PDF (Standard and Premium plans only).

History retention:
  - Free plan     : last 5 scans only
  - Standard plan : last 30 days
  - Premium plan  : full history

3.9 MY PLAN
-----------
View your current subscription details.

  - Access from the home screen or menu.
  - Shows your plan name (Free / Standard / Premium).
  - Shows daily scan usage:
      Free plan    : progress bar showing X of 5 scans used today.
      Paid plans   : "Unlimited" badge — no daily limit.
  - Lists all features included in your plan.
  - Free users see a "View Paid Plans" button to upgrade.

3.10 UPGRADE PLAN
-----------------
1. Go to My Plan > View Paid Plans, or Menu > Plans.
2. Browse available plans (Free, Standard, Premium).
3. Tap a plan to see its full feature list and price.
4. Tap "Upgrade to [Plan Name]" and confirm.
5. Your plan is updated immediately.

See Section 4 for full plan comparison.

3.11 MENU
---------
The side or bottom menu provides quick navigation to all sections:
  - Home
  - Scan URL
  - Camera Scan
  - QR Scan
  - Saved Links
  - Scan History
  - My Plan
  - Settings
  - Log Out

3.12 SETTINGS
-------------
Access from Menu > Settings or from the dashboard.

  EDIT PROFILE
    Update your display name and email address.
    1. Tap "Edit Profile".
    2. Change your name or email.
    3. Tap "Save Changes".

  AUTO LOG OUT
    Set how long the app waits before automatically logging you out
    due to inactivity. Options typically include: 5 min, 15 min, 30 min,
    1 hour, or Never.

  WARNING STRICTNESS
    Controls how sensitive the app is when flagging scan warnings.
      - Low    : Only flags confirmed threats.
      - Medium : Flags likely threats (default).
      - High   : Flags any suspicious signals, even minor ones.

  HELP / FAQ
    Browse frequently asked questions and guides about using the app.
    Questions are organised by category. Tap any question to expand
    the answer.

  REPORT / SUPPORT
    Send a support request directly to the platform team.
    1. Tap "Report / Support".
    2. Tap the + button to create a new report.
    3. Enter a subject and describe your issue.
    4. Tap "Submit".
    5. The platform manager team will review and reply.
    6. You can view replies and send follow-up messages from this screen.
    7. A "Reply" badge appears on tickets that have received a response.

  DELETE ACCOUNT
    Permanently deletes your account and all associated data.
    This action cannot be undone.
    1. Tap "Delete Account".
    2. Confirm in the dialog that appears.
    3. Your account is removed and you are logged out.


================================================================================
4. SUBSCRIPTION PLANS
================================================================================

4.1 FREE PLAN — $0/month
-------------------------
  - 5 scans per day
  - Manual URL scanning
  - Camera OCR scanning
  - Basic risk level classification
  - Save important links
  - Standard security analysis
  - Sandbox environment
  - Last 5 scans in history

4.2 STANDARD PLAN — $4.99/month
--------------------------------
  - Unlimited scans
  - Manual URL scanning
  - Camera OCR scanning
  - Detailed risk level classification
  - Save important links
  - Detailed security analysis
  - Alert threshold notifications
  - Sandbox environment
  - Last 30 days scan history
  - Export history (CSV + PDF)

4.3 PREMIUM PLAN — $9.99/month
--------------------------------
  - All Standard features
  - Advanced multi-layer security analysis
  - Full scan history (no time limit)
  - Export history (CSV + PDF)
  - Ad-heavy website warnings
  - Script and tracker detection


================================================================================
5. ADMIN GUIDE
================================================================================

Admin accounts must be approved by a developer before they can log in.
Once approved, admins are directed to the Admin Dashboard.

Admins manage the platform's users and monitor system security.
Admins do NOT have access to subscription plan management or analytics
(those are handled by Platform Managers).

5.1 ADMIN DASHBOARD
--------------------
The dashboard shows:
  - Total number of users on the platform.
  - Number of scans performed.
  - Number of flagged/malicious links detected.
  - Navigation to all admin sections.

5.2 USER MANAGEMENT
--------------------
View, create, and manage all user accounts on the platform.

  VIEW USERS:
    - See a list of all registered users.
    - Search for users by name or email.
    - Each user card shows: name, email, role, plan, and status.

  USER DETAIL:
    Tap a user to open their full profile:
      - View name, email, role, plan, and account status.
      - See their scan activity count.
      - Approve or reject pending accounts.
      - Suspend a user (they cannot log in while suspended).
      - Reactivate a suspended user.
      - Lock / Unlock an account.
      - Assign or remove a User Profile (role-based permission group).

  CREATE USER:
    Admins can manually create new user accounts directly.

5.3 USER PROFILES
-----------------
User Profiles are permission groups (similar to roles) that can be
assigned to users.

  - View all existing profiles.
  - Create a new profile with a name, description, and permission settings.
  - Edit or update an existing profile.
  - Suspend or reactivate a profile.
  - Assign a profile to a user from User Management > User Detail.

5.4 SECURITY MONITOR
---------------------
Monitor users with suspicious security activity.

  - View accounts with multiple failed login attempts.
  - See locked accounts.
  - Identify users with unusual scan patterns.
  - Manually lock or unlock accounts from this screen.

5.5 SCAN RECORDS
-----------------
View all scan activity across the entire platform.

  - See every scan performed by every user.
  - Filter by verdict (safe, suspicious, malicious, etc.).
  - View scan date, URL, user, risk level, and scan method.
  - Identify patterns of dangerous links being scanned.

5.6 FLAGGED LINKS
-----------------
View all links that have been flagged as malicious or suspicious.

  - Filter by verdict type.
  - See which users scanned flagged links.
  - Use this to identify recurring threats or phishing campaigns.

5.7 AUDIT LOG
--------------
A full log of all admin actions taken on the platform.

  - Records every action (user suspended, account approved, profile
    assigned, etc.).
  - Shows: action type, target user, performing admin, and timestamp.
  - Used for accountability and compliance tracking.

5.8 SUBSCRIPTIONS
-----------------
View and manage user subscription plans.

  - See all users and their current plan (Free / Standard / Premium).
  - Update a user's plan directly.
  - Cancel or reassign plans.
  - Tap a user to go to their full User Detail screen.

5.9 SETTINGS (ADMIN)
---------------------
Admin settings include:
  - Edit Profile   : Update admin name and email.
  - Auto Log Out   : Set inactivity timeout.
  - Help / FAQ     : Browse help articles.

Note: Warning Strictness and Report/Support are not available for admins
as these are regular-user-only features.


================================================================================
6. PLATFORM MANAGER GUIDE
================================================================================

Platform Manager accounts must be approved by a developer before login.
Once approved, PMs are directed to the PM Dashboard.

Platform Managers handle the business side of the platform: subscription
plans, analytics, reports, user support, FAQ content, and system health.

6.1 PM DASHBOARD
-----------------
The dashboard shows:
  - A welcome header with the PM's name and role.
  - Navigation cards to all PM management sections:
      Subscription Plans, Analytics, Reports,
      Support Requests, FAQ Management, System Health.

6.2 SUBSCRIPTION PLANS
-----------------------
Create and manage the subscription plans available to users.

  VIEW PLANS:
    - See all active and suspended plans.
    - Each plan shows: name, price, scan limit, status, and features.

  CREATE A PLAN:
    1. Tap the + button.
    2. Enter plan name, price, scan limit, and list of features.
    3. Tap "Create".

  EDIT A PLAN:
    - Tap a plan to open its detail screen.
    - Update name, price, or features.
    - Tap "Save".

  SUSPEND / ACTIVATE A PLAN:
    - Suspended plans are hidden from users and cannot be selected.
    - Activate a suspended plan to make it available again.

6.3 ANALYTICS
--------------
View platform-wide usage statistics and trends.

  - Total users, total scans, active users.
  - Scans broken down by risk level (safe, low, medium, high, malicious).
  - New user registrations by plan type.
  - Feature usage statistics (URL scan, camera scan, QR scan, sandbox).
  - Data is shown with visual charts and summary cards.

6.4 REPORTS
-----------
Generate detailed reports about platform activity.

  - Select a date range (start date and end date).
  - Tap "Generate Report".
  - The report shows:
      - Total scans and users in the period.
      - Risk level breakdown.
      - Most scanned domains.
      - Plan distribution.
  - Use reports for performance reviews and presentations.

6.5 SUPPORT REQUESTS
---------------------
Manage support tickets submitted by regular users.

  VIEW REQUESTS:
    - See all submitted support requests.
    - Filter by status: All, Open, In Progress, Resolved.
    - Each ticket shows: subject, submitter email, status, reply count.

  REPLY TO A REQUEST:
    1. Tap a support request to open it.
    2. Read the user's message and any previous replies.
    3. Type your reply in the text field.
    4. Tap "Send Reply".
    5. The user will see your reply in their "Report / Support" screen.
    6. Status automatically changes to "In Progress" after first PM reply.

  UPDATE STATUS:
    - Change ticket status to: Open, In Progress, or Resolved.
    - Resolved tickets are closed and no further replies can be sent.

6.6 FAQ MANAGEMENT
-------------------
Create and manage the FAQ articles that users see in Help / FAQ.

  VIEW FAQs:
    - See all existing FAQ entries with question, answer, and category.

  CREATE A FAQ:
    1. Tap the + button.
    2. Enter the question, answer, and category.
    3. Tap "Save".

  EDIT A FAQ:
    - Tap any FAQ entry to edit its content.
    - Update question, answer, or category.
    - Tap "Save".

  DELETE A FAQ:
    - Tap a FAQ entry and select Delete.

6.7 SYSTEM HEALTH
-----------------
Monitor the health of the platform's backend services.

  - Database status (connected / error).
  - API service status (online / offline).
  - Scan service status (running / degraded).
  - Active system alerts (e.g. high error rate, slow response).
  - Resolve alerts once they have been addressed.

This screen helps the PM quickly identify if something is wrong with
the platform before users are affected.

6.8 SETTINGS (PM)
------------------
PM settings include:
  - Edit Profile   : Update PM name and email.
  - Auto Log Out   : Set inactivity timeout.
  - Help / FAQ     : Browse help articles.

Note: Warning Strictness and Report/Support are not shown for PM accounts.


================================================================================
7. ACCOUNT APPROVAL PROCESS (ADMIN & PM ACCOUNTS)
================================================================================

When a new Admin or Platform Manager account is created:

  1. The account is created with status "Pending".
  2. The new user sees "Your account is pending approval" when they try
     to log in.
  3. An approval email is automatically sent to the developer team.
  4. The email shows the new user's name, email, and role.
  5. The developer clicks "Approve" or "Reject" directly from the email.

  IF APPROVED:
    - The account status is set to "Approved".
    - The user can now log in normally.

  IF REJECTED:
    - The account status is set to "Rejected".
    - The user sees a rejection message when they attempt to log in.

  ALTERNATIVE — APPROVE FROM APP:
    Admins can also approve or reject pending accounts directly from
    the app: Admin Dashboard > User Management > tap the user >
    tap "Approve" or "Reject".


================================================================================
8. TECHNICAL INFORMATION
================================================================================

  Platform      : Android (Jetpack Compose)
  Backend       : FastAPI (Python)
  Database      : Supabase (PostgreSQL)
  Min Android   : Android 8.0 (API 26)

  BACKEND API
    The app connects to a FastAPI backend server. The server handles:
      - User authentication (login, signup, password reset)
      - URL scanning and risk analysis
      - Sandbox environment analysis
      - Plan management and quota enforcement
      - Support request messaging
      - Admin and platform manager operations

  SCANNING TECHNOLOGY
    - URL scanning uses multi-layer threat intelligence.
    - Camera scan uses OCR (Optical Character Recognition) to extract
      URLs from images.
    - QR scan reads QR codes directly using the device camera.
    - Sandbox environment provides isolated deep-link analysis.

  DATA & PRIVACY
    - User passwords are hashed and never stored in plain text.
    - Scan results are stored securely in the cloud database.
    - Deleting your account removes all associated data permanently.
    - The service role key is used server-side only and never exposed
      to the mobile app.

  SUBSCRIPTION ENFORCEMENT
    - Free users: limited to 5 scans per day (enforced server-side).
    - Standard / Premium users: unlimited scans (limit set to 999,999).
    - Quota resets daily at midnight (UTC).
    - Scan history retention is enforced based on plan at query time.

================================================================================
                        END OF WEBLINK SCANNER USER GUIDE
================================================================================
