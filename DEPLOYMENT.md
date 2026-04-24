# Deployment Guide

This guide walks through deploying Smart Campus Hub to free/low-cost hosting. The recommended split:

| Piece | Host | Why |
|---|---|---|
| **PostgreSQL** | [Supabase](https://supabase.com/) (free) or [Neon](https://neon.tech/) (free) | Managed Postgres with generous free tier, TLS-ready |
| **Backend (Spring Boot)** | [Render](https://render.com/) (free web service) or [Railway](https://railway.app/) (usage-based) | Supports Docker + JAR runtime, good for Spring Boot |
| **Frontend (Vite/React)** | [Vercel](https://vercel.com/) or [Netlify](https://www.netlify.com/) | Fast global CDN, free for static builds |

> The free tier on Render spins the service down after 15 min idle — first request after that may take ~30s. Fine for a viva demo; warm it up 1 min before you start.

---

## 1. Provision the Database (Supabase)

1. Create a free Supabase project at [supabase.com](https://supabase.com/).
2. Go to **Project Settings → Database** and copy the **connection string** (the "URI" tab, not pooled).
   - It looks like `postgresql://postgres.<ref>:<password>@<host>:5432/postgres`
3. Make sure **SSL is enforced** (Supabase requires this).
4. Note: Supabase databases are named `postgres`, not `smart_campus`. That's fine — Hibernate `ddl-auto: update` will create the tables in the default schema.

> **Alternative (Neon):** sign up at neon.tech, create a project, copy the connection string. Same fit.

---

## 2. Deploy the Backend (Render)

### Option A — Deploy from a Dockerfile (recommended)

Add this `Dockerfile` at `smart-campus-api/Dockerfile` (only needed once; skip if one already exists):

```dockerfile
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Render steps

1. Push your repo to GitHub (main branch must be green).
2. On Render → **New → Web Service**, connect your repo.
3. Set:
   - **Root Directory:** `smart-campus-api`
   - **Environment:** Docker
   - **Instance Type:** Free
   - **Branch:** `main`
4. Add environment variables (under "Environment" tab):

   | Key | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require` |
   | `DB_USERNAME` | `postgres.<project-ref>` |
   | `DB_PASSWORD` | (from Supabase) |
   | `JWT_SECRET_KEY` | random 32+ chars — generate with `openssl rand -base64 48` |
   | `GOOGLE_CLIENT_ID` | your OAuth client id |
   | `GOOGLE_CLIENT_SECRET` | your OAuth client secret |
   | `EMAIL_USERNAME` | your Gmail address |
   | `EMAIL_PASSWORD` | Gmail **app password** (not your account password) |
   | `OPENAI_API_KEY` | your OpenAI key (if using AI provider) |
   | `APP_OAUTH2_REDIRECT_URI` | `https://<your-frontend>.vercel.app/oauth2/redirect` |
   | `APP_FRONTEND_URL` | `https://<your-frontend>.vercel.app` |
   | `SEED_ADMIN_EMAIL` | your admin email |
   | `SEED_ADMIN_PASSWORD` | strong admin password |

5. Click **Create Web Service**. First build takes 5–10 minutes (Maven downloads). Subsequent deploys are faster.
6. Note the Render URL, e.g. `https://smart-campus-api.onrender.com`.

### Important: update the datasource URL

The backend currently hard-codes `jdbc:postgresql://localhost:5432/smart_campus` in [application.yml](smart-campus-api/src/main/resources/application.yml). Override it in prod by setting `SPRING_DATASOURCE_URL` (Spring Boot reads this automatically).

### Update Google OAuth2 redirect URI

In Google Cloud Console, add this additional authorized redirect URI:

```
https://<your-backend>.onrender.com/login/oauth2/code/google
```

---

## 3. Deploy the Frontend (Vercel)

1. Push to GitHub.
2. On Vercel → **Add New Project**, import your repo.
3. Set:
   - **Root Directory:** `smart-campus-ui`
   - **Framework Preset:** Vite
   - **Build Command:** `npm run build` (default)
   - **Output Directory:** `dist` (default)
4. Add environment variables:

   | Key | Value |
   |---|---|
   | `VITE_API_URL` | `https://<your-backend>.onrender.com/api` |
   | `VITE_GOOGLE_MAPS_API_KEY` | your Maps JS key |
   | `VITE_SUPABASE_URL` | your Supabase URL (for avatar/file uploads) |
   | `VITE_SUPABASE_ANON_KEY` | your Supabase anon key |

5. Deploy. Vercel will give you a URL like `https://smart-campus-ui.vercel.app`.

### Update backend CORS / frontend URL

Now that you have the Vercel URL, go back to Render and make sure these env vars point at it:

- `APP_OAUTH2_REDIRECT_URI=https://smart-campus-ui.vercel.app/oauth2/redirect`
- `APP_FRONTEND_URL=https://smart-campus-ui.vercel.app`

Then **redeploy** the backend so it picks up the change.

---

## 4. Post-Deploy Smoke Test

1. Visit the frontend URL.
2. Log in with the seed admin (`SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD`).
3. Check that the **Admin Dashboard** loads, KPI cards show real counts (from the seeded data), and the analytics charts render.
4. Try:
   - Google OAuth sign-in (new browser / incognito)
   - Creating a booking
   - Filing a ticket
   - Asking CampusBot a question

If anything fails, check Render logs for the backend and the browser console for the frontend.

---

## 5. Common Issues

- **Frontend shows "Network Error" everywhere** — `VITE_API_URL` missing or CORS. Confirm backend sets `APP_FRONTEND_URL` and that the URL matches Vercel exactly (no trailing slash).
- **Google login redirects to a 400** — OAuth redirect URI doesn't include the Render URL. Add it in Google Cloud Console.
- **Backend boots then crashes with `relation does not exist`** — database connection worked but ddl-auto didn't run. Check `spring.jpa.hibernate.ddl-auto=update` in application.yml.
- **Render cold-start latency** — expected on free tier. Upgrade to paid ($7/mo) for always-on.
- **CampusBot returns "provider error"** — `OPENAI_API_KEY` not set or wrong. Make sure you set it as a Render env var (not committed).
- **Google Maps tile quota exceeded** — restrict your Maps key to the Vercel domain.

---

## 6. Pre-viva Checklist

- [ ] Warm up the Render instance 1 minute before starting (visit backend `/actuator/health` or any page).
- [ ] Confirm seed admin login works on the live URL.
- [ ] Confirm at least one student, lecturer, and technician account can log in.
- [ ] Verify charts render with seeded data.
- [ ] Try the CampusBot prompt "Show me pending bookings" as an admin.
- [ ] Have Render logs open in another tab in case of issues.
