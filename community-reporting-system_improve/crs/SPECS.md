# Community Reporting System: Project Specs

**Developer:** Errel (College Student)
**Stack:** HTML, CSS, JavaScript (front end) · Java (back end) · Java arrays (database)

---

## 1. Project Overview

A web system where **residents** report community problems (broken streetlights, garbage, flooding, etc.) and **admins** review, update, and resolve those reports. Admins also post announcements and manage resident accounts.

### Goals
- Residents can register, log in, submit reports, and track their status.
- Admins can view, manage, update, and delete reports, manage announcements, and manage residents.
- Keep it simple: no frameworks, no external database, no picture uploads.

### Out of Scope (do NOT build)
- Picture or file uploads
- Real database (MySQL, etc.)
- Email or SMS notifications
- Password reset
- Payment, maps, or chat

---

## 2. Tech Stack and Constraints

| Layer | Technology | Notes |
|---|---|---|
| Front end | HTML, CSS, vanilla JavaScript | No frameworks. Use `fetch()` to call the back end. |
| Back end | Java (JDK 17+) | Use the built-in `com.sun.net.httpserver.HttpServer`. No Spring needed. |
| Database | Java arrays / `ArrayList` | In-memory only. **Data resets when the server restarts.** |
| Data format | JSON | Front end and back end exchange JSON. |
| Auth | Simple session token | Server stores a token for each logged-in user. |

> **Tip for overwhelm:** Build ONE feature at a time, from front end to back end, then test it before moving on. See the build phases in Section 10.

---

## 3. User Roles

### 3.1 Resident
| # | Feature | Description |
|---|---|---|
| R1 | Resident Registration | Create an account with name, address, contact number, username, password. |
| R2 | Resident Login | Log in with username and password. |
| R3 | View Homepage | Resident dashboard with a welcome message, quick links, latest announcements. |
| R4 | Report Categories | Choose a category when submitting a report. |
| R5 | Submit Report | Fill in category, title, description, location. |
| R6 | View My Reports | See a list of the resident's own reports. |
| R7 | Track Report Status | See status of each report (Pending, In Progress, Resolved, Rejected). |
| R8 | View Announcements | Read announcements posted by admin. |
| R9 | Edit Profile | Update name and details only (no picture). |
| R10 | Logout | End session and return to the login page. |

### 3.2 Admin
| # | Feature | Description |
|---|---|---|
| A1 | Admin Login | Log in with admin credentials. |
| A2 | Admin Dashboard | Summary counts (total, pending, in progress, resolved) and quick links. |
| A3 | View All Reports | Table of every report from all residents, with filters. |
| A4 | Manage Reports | View full details of a report and take actions. |
| A5 | Update Report Status | Change a report's status and optionally add an admin remark. |
| A6 | Delete Reports | Remove a report (with confirmation). |
| A7 | Manage Announcements | Add, edit, and delete announcements. |
| A8 | Manage Residents | View, edit, and delete resident accounts. |
| A9 | Logout | End session and return to the login page. |

---

## 4. Pages

| # | Page | File | Who can see it | Purpose |
|---|---|---|---|---|
| 1 | Home Page | `index.html` | Everyone | Landing page with system intro and Login/Register buttons. |
| 2 | About Us | `about.html` | Everyone | What the system is, mission, contact info. |
| 3 | Login Page | `login.html` | Everyone | One login form for both Resident and Admin. Redirects by role. |
| 4 | Registration Page | `register.html` | Everyone | Resident sign-up form. |
| 5 | Resident Dashboard | `resident-dashboard.html` | Resident | Welcome, stats of own reports, latest announcements, profile edit. |
| 6 | Submit Report | `submit-report.html` | Resident | Form to create a report. |
| 7 | My Reports | `my-reports.html` | Resident | List of own reports with status tracking. |
| 8 | Announcements | `announcements.html` | Resident | List of all announcements. |
| 9 | Admin Dashboard | `admin-dashboard.html` | Admin | Stats, links to management pages. Also holds Manage Announcements and Manage Residents sections (or separate tabs). |
| 10 | Manage Reports | `manage-reports.html` | Admin | View all reports, update status, delete. |

> Edit Profile can be a section or modal inside `resident-dashboard.html`. Manage Announcements and Manage Residents can be tabs inside `admin-dashboard.html`. This keeps you at exactly the 10 required pages.

### Page Flow

```
index.html ──► login.html ──► (Resident) ──► resident-dashboard.html ──► submit-report.html
   │              │                                  │                 ──► my-reports.html
   │              │                                  │                 ──► announcements.html
   │              └──► (Admin) ──► admin-dashboard.html ──► manage-reports.html
   ├──► register.html
   └──► about.html
```

---

## 5. Data Model (Java Arrays)

Each "table" is a Java `ArrayList` (or fixed array) stored in a `Database.java` class.

### 5.1 Resident
| Field | Type | Notes |
|---|---|---|
| id | int | Auto-increment |
| fullName | String | |
| address | String | |
| contactNumber | String | |
| username | String | Must be unique |
| password | String | Plain text is OK for a school project |

### 5.2 Admin
| Field | Type | Notes |
|---|---|---|
| id | int | |
| username | String | |
| password | String | |

Preload one admin: `admin / admin123` (change if you like).

### 5.3 Report
| Field | Type | Notes |
|---|---|---|
| id | int | Auto-increment |
| residentId | int | Links to Resident |
| category | String | See categories below |
| title | String | |
| description | String | |
| location | String | |
| status | String | `Pending`, `In Progress`, `Resolved`, `Rejected` |
| adminRemark | String | Optional, set by admin |
| dateSubmitted | String | e.g. `2026-11-09` |
| dateUpdated | String | |

### 5.4 Announcement
| Field | Type | Notes |
|---|---|---|
| id | int | |
| title | String | |
| content | String | |
| datePosted | String | |

### 5.5 Report Categories (fixed list)
1. Road and Infrastructure
2. Garbage and Sanitation
3. Streetlights and Electricity
4. Water and Drainage
5. Noise and Disturbance
6. Safety and Security
7. Others

### 5.6 Sessions
A `HashMap<String, Session>` mapping a token to `{userId, role}`. Created at login, removed at logout.

---

## 6. Report Status Flow

```
Pending ──► In Progress ──► Resolved
   │
   └──────► Rejected
```

- New reports always start as **Pending**.
- Only admin can change the status.
- Resident can only view the status, not edit it.

---

## 7. Back-End API (Java)

All responses are JSON. Base URL example: `http://localhost:8080`

### 7.1 Authentication
| Method | Endpoint | Body | Who | Description |
|---|---|---|---|---|
| POST | `/api/register` | fullName, address, contactNumber, username, password | Public | Create resident |
| POST | `/api/login` | username, password | Public | Returns `{token, role, name}` |
| POST | `/api/logout` | (token in header) | Any | Ends session |

### 7.2 Resident Endpoints
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/categories` | List of report categories |
| POST | `/api/reports` | Submit a new report |
| GET | `/api/reports/mine` | Get the logged-in resident's reports |
| GET | `/api/announcements` | List all announcements |
| GET | `/api/profile` | Get own profile |
| PUT | `/api/profile` | Update name, address, contact number |

### 7.3 Admin Endpoints
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/stats` | Totals for dashboard |
| GET | `/api/admin/reports` | All reports (supports `?status=` and `?category=`) |
| PUT | `/api/admin/reports/{id}/status` | Update status and adminRemark |
| DELETE | `/api/admin/reports/{id}` | Delete a report |
| POST | `/api/admin/announcements` | Add announcement |
| PUT | `/api/admin/announcements/{id}` | Edit announcement |
| DELETE | `/api/admin/announcements/{id}` | Delete announcement |
| GET | `/api/admin/residents` | List all residents |
| PUT | `/api/admin/residents/{id}` | Edit resident details |
| DELETE | `/api/admin/residents/{id}` | Delete resident (and their reports) |

### 7.4 Security Rules
- Send the token in the header: `Authorization: <token>`.
- `/api/admin/*` endpoints return **403** if the role is not admin.
- Resident endpoints return **401** if no valid token.
- A resident can only see their own reports.

### 7.5 Standard Responses
```json
{ "success": true,  "message": "Report submitted", "data": { } }
{ "success": false, "message": "Username already exists" }
```

---

## 8. Validation Rules

| Field | Rule |
|---|---|
| Full name | Required, 2 to 50 characters |
| Username | Required, 4 to 20 characters, must be unique |
| Password | Required, at least 6 characters |
| Contact number | Required, digits only, 10 to 11 characters |
| Address | Required |
| Report title | Required, max 100 characters |
| Report description | Required, max 500 characters |
| Report location | Required |
| Category | Must be in the category list |
| Announcement title/content | Required |

Validate on **both** the front end (JS) and back end (Java).

---

## 9. Suggested Project Structure

```
community-reporting-system/
├── SPECS.md
├── frontend/
│   ├── index.html
│   ├── about.html
│   ├── login.html
│   ├── register.html
│   ├── resident-dashboard.html
│   ├── submit-report.html
│   ├── my-reports.html
│   ├── announcements.html
│   ├── admin-dashboard.html
│   ├── manage-reports.html
│   ├── css/
│   │   └── style.css
│   └── js/
│       ├── api.js          (shared fetch helper + token handling)
│       ├── auth.js         (login, register, logout)
│       ├── resident.js     (resident pages)
│       └── admin.js        (admin pages)
└── backend/
    └── src/
        ├── Main.java            (starts the server)
        ├── Database.java        (the arrays + seed data)
        ├── models/
        │   ├── Resident.java
        │   ├── Admin.java
        │   ├── Report.java
        │   └── Announcement.java
        └── handlers/
            ├── AuthHandler.java
            ├── ReportHandler.java
            ├── AnnouncementHandler.java
            ├── ProfileHandler.java
            └── AdminHandler.java
```

---

## 10. Build Plan and Progress Tracker

Work top to bottom. Tick each box when it is done **and tested**.

### Phase 0: Setup
- [ ] Install JDK 17+ and a code editor (VS Code or IntelliJ)
- [ ] Create the folder structure above
- [ ] Create `Main.java` that starts a server and responds "Hello" at `/api/test`
- [ ] Add CORS headers so the front end can call the back end
- [ ] Create `style.css` with base colors, fonts, buttons, form styles, navbar

### Phase 1: Database and Models
- [ ] Create model classes (Resident, Admin, Report, Announcement)
- [ ] Create `Database.java` with ArrayLists and id counters
- [ ] Add seed data: 1 admin, 2 sample residents, 3 sample reports, 2 announcements
- [ ] Add a simple JSON helper (build JSON strings by hand, or a tiny parser)

### Phase 2: Public Pages
- [ ] `index.html` (Home)
- [ ] `about.html`
- [ ] Shared navbar and footer on all pages

### Phase 3: Authentication
- [ ] **R1** `POST /api/register` + `register.html` with validation
- [ ] **R2 / A1** `POST /api/login` + `login.html` (redirect by role)
- [ ] Session token storage (`localStorage` on the front end, `HashMap` on the back end)
- [ ] **R10 / A9** `POST /api/logout` + logout button on all private pages
- [ ] Page guard in JS: redirect to login if no token or wrong role

### Phase 4: Resident Features
- [ ] **R3** `resident-dashboard.html` (welcome + latest announcements)
- [ ] **R4** `GET /api/categories` + dropdown
- [ ] **R5** `POST /api/reports` + `submit-report.html`
- [ ] **R6** `GET /api/reports/mine` + `my-reports.html` table
- [ ] **R7** Status badges (color coded) and admin remark shown in My Reports
- [ ] **R8** `GET /api/announcements` + `announcements.html`
- [ ] **R9** `GET/PUT /api/profile` + Edit Profile section

### Phase 5: Admin Features
- [ ] **A2** `GET /api/admin/stats` + `admin-dashboard.html` cards
- [ ] **A3** `GET /api/admin/reports` + `manage-reports.html` table with filters
- [ ] **A4 / A5** Update status modal + `PUT /api/admin/reports/{id}/status`
- [ ] **A6** `DELETE /api/admin/reports/{id}` with confirm dialog
- [ ] **A7** Announcements: add, edit, delete
- [ ] **A8** Residents: list, edit, delete

### Phase 6: Polish and Testing
- [ ] Make pages responsive (works on phone width)
- [ ] Show friendly error and success messages
- [ ] Run the test checklist in Section 11
- [ ] Clean up code and add comments
- [ ] Write a short README on how to run the project
- [ ] Prepare a demo walkthrough for your instructor

---

## 11. Test Checklist

### Resident
- [ ] Register with valid data works
- [ ] Register with a duplicate username is rejected
- [ ] Login with wrong password shows an error
- [ ] Login with correct data opens the Resident Dashboard
- [ ] Submit a report: it appears in My Reports as **Pending**
- [ ] Empty fields in Submit Report are blocked
- [ ] Resident cannot see other residents' reports
- [ ] Edit Profile saves and shows updated name
- [ ] Logout returns to login, and the Back button does not reopen private pages

### Admin
- [ ] Admin login opens the Admin Dashboard
- [ ] A resident account cannot open admin pages
- [ ] Dashboard counts match the actual reports
- [ ] Changing status updates what the resident sees
- [ ] Deleting a report removes it from both admin and resident views
- [ ] Announcement added by admin appears on the resident's page
- [ ] Deleting a resident also removes their reports

---

## 12. Simple UI Guidelines

- **Colors:** one main color (e.g., blue `#2563eb`), white background, light gray cards.
- **Status badge colors:** Pending = yellow, In Progress = blue, Resolved = green, Rejected = red.
- **Layout:** navbar on top, centered content, cards for forms and tables.
- **Font:** a clean sans-serif (system font is fine).
- Keep forms short and labels clear.

---

## 13. Known Limitations (mention in your presentation)

- Data is stored in memory, so it is **lost when the server stops**.
- Passwords are stored as plain text (acceptable for a school demo, not for production).
- Single server, no real security hardening.

**Possible improvements:** save arrays to a text/CSV file, hash passwords, add search and pagination, connect a real database.

---

## 14. How to Avoid Getting Overwhelmed

1. **Only work on one checkbox at a time.**
2. Build the smallest working version first (ugly is fine), then style it later.
3. Test after every checkbox using the browser and the console.
4. If stuck, write down the exact error and the exact step you were doing.
5. Do Phase 3 (login) well, because everything else depends on it.
