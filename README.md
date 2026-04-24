# Smart Campus Hub

A full-stack campus management platform built for SLIIT's IT3030 Programming Applications Frameworks module. It covers student academics, facility bookings, maintenance ticketing, shuttle tracking, announcements, admin analytics, and an AI assistant ("CampusBot").

- **Backend** — Spring Boot 4 (Java 25) + PostgreSQL + JWT/OAuth2 → [smart-campus-api/](smart-campus-api/)
- **Frontend** — React 19 + Vite + TypeScript + Tailwind → [smart-campus-ui/](smart-campus-ui/)

## Modules

| # | Module | Summary |
|---|---|---|
| A | **Facilities & Assets Catalogue** | Manage resources (lecture halls, labs, meeting rooms, equipment), amenities, assets, and locations. |
| B | **Booking Management** | Students and staff request resource bookings; admins approve/reject with conflict detection. |
| C | **Maintenance & Incident Ticketing** | Report issues with photos, priority and category routing, technician assignment, comment threads. |
| D | **Notifications** | Scheduled announcements + real-time in-app and Web Push delivery over WebSocket. |
| E | **Authentication & Authorization** | Google OAuth2, email/password with OTP, password reset, JWT access/refresh tokens, role-based access (`STUDENT`, `LECTURER`, `ADMIN`, `TECHNICAL_STAFF`). |
| F | **Academics** | Course offerings, sections, enrollment, grading, transcripts, lecturer dashboard. |
| G | **Shuttle Tracking** | Real-time shuttle routes, stops, and live bus positions on Google Maps. |
| H | **AI Assistant (CampusBot)** | Chatbot with tool-calling (OpenAI or Groq) that can browse and act on campus data. |
| I | **Audit Log** | Admin-visible trail of privileged actions for compliance and traceability. |
| J | **Admin Console & Analytics** | User lifecycle management plus KPI cards and charts (user roles, bookings, tickets, top resources, peak booking hours, course offerings) with per-chart filter tabs. |

## Prerequisites

Install these before you start:

| Tool | Version | Notes |
|---|---|---|
| **JDK** | 25 | Required by the backend `pom.xml` |
| **Node.js** | 20.x or 22.x LTS | Needed by Vite 8 |
| **npm** | 10+ | Ships with Node |
| **PostgreSQL** | 14+ | Backend connects to `localhost:5432` by default |
| **Git** | any | — |

You do **not** need Maven installed globally; the backend ships with the Maven Wrapper (`./mvnw`).

## Repository Layout

```
PAF_Y3S2/
├── smart-campus-api/    Spring Boot backend (port 8080)
├── smart-campus-ui/     React + Vite frontend (port 5173)
└── README.md            You are here
```

## 1. Database Setup

Create the PostgreSQL database once:

```sql
CREATE DATABASE smart_campus;
```

The backend uses Hibernate `ddl-auto: update`, so tables are created/migrated automatically on first run. A default admin user is seeded at startup (see [Seed Admin](#seed-admin)).

## 2. Backend Setup

```bash
cd smart-campus-api
cp .env.example .env
```

Open `.env` and fill in the required values (see the table below), then run:

```bash
# Windows
./mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

The API will boot on **http://localhost:8080**.

### Required environment variables

The committed `.env.example` only lists the auth-critical variables. The full set the backend reads is:

| Variable | Required | Purpose |
|---|---|---|
| `DB_USERNAME` | yes | PostgreSQL user |
| `DB_PASSWORD` | yes | PostgreSQL password |
| `JWT_SECRET_KEY` | yes | HS256 secret, **min 32 chars** |
| `GOOGLE_CLIENT_ID` | yes | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | yes | Google OAuth2 client secret |
| `EMAIL_USERNAME` | yes | Gmail SMTP username (OTP + password reset) |
| `EMAIL_PASSWORD` | yes | Gmail **App Password**, not your account password |
| `AI_PROVIDER` | no | `openai` (default) or `groq` |
| `OPENAI_API_KEY` | if provider=openai | Enables CampusBot |
| `GROQ_API_KEY` | if provider=groq | Alternative CampusBot provider |
| `SEED_ADMIN_EMAIL` | no | Defaults to `smartcampus.team@gmail.com` |
| `SEED_ADMIN_PASSWORD` | no | Defaults to `Admin@1234!` |
| `PUSH_VAPID_PUBLIC_KEY` | no | Web-push; auto-generated on first run if blank |
| `PUSH_VAPID_PRIVATE_KEY` | no | Web-push; auto-generated on first run if blank |

> **Google OAuth2 redirect URI:** in the Google Cloud Console, add
> `http://localhost:8080/login/oauth2/code/google` as an authorized redirect URI.

### Seed admin

On first boot the backend creates an admin account using `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD`. Use those credentials to log in at **http://localhost:5173** after starting the frontend.

## 3. Frontend Setup

```bash
cd smart-campus-ui
cp .env.example .env.local
npm install
npm run dev
```

The dev server runs at **http://localhost:5173** and proxies API calls to the backend via `VITE_API_URL`.

### Frontend environment variables

| Variable | Required | Purpose |
|---|---|---|
| `VITE_API_URL` | yes | Backend base URL, e.g. `http://localhost:8080/api` |
| `VITE_GOOGLE_MAPS_API_KEY` | for shuttle tracking | Google Maps JS API key with Maps JavaScript + Places enabled |
| `VITE_SUPABASE_URL` | for file uploads (avatars etc.) | Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | for file uploads | Supabase anon key |

### Frontend scripts

| Command | Purpose |
|---|---|
| `npm run dev` | Start Vite dev server with HMR |
| `npm run build` | Type-check (`tsc -b`) and produce a production bundle in `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run ESLint |

## Features

- **Authentication** — Google OAuth2, email/password with OTP verification, password reset, JWT access + refresh tokens
- **Role-based access** — `STUDENT`, `LECTURER`, `ADMIN`, `TECHNICAL_STAFF`
- **Academics** — Course offerings, section enrollment, grading, transcripts, lecturer dashboard
- **Facility bookings** — Browse resources, check conflicts, admin approval workflow
- **Maintenance ticketing** — Report issues with photos, technician assignment, comment threads
- **Shuttle tracking** — Real-time bus locations on Google Maps, routes, stops
- **Announcements & notifications** — Scheduled announcements, in-app + Web Push delivery over WebSocket
- **Admin dashboard** — KPI cards, analytics (user roles, bookings, tickets, resources, top resources, peak booking hours, course offerings) with per-chart filter tabs
- **CampusBot** — AI assistant (OpenAI or Groq) with tool-calling to browse and act on campus data
- **Audit log** — Admin-visible record of privileged actions

## Course Info

- Module: **IT3030 — Programming Applications Frameworks**
- Institution: SLIIT, Y3S2
- Team: Smart Campus Hub
