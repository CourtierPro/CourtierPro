# CourtierPro — Deployment Manual & Handoff Instructions

> **Prepared for:** Nabizada Courtier Inc.
> **Prepared by:** Shawn Nabizada, Amir Ghadimi, Olivier Goudreault, Isaac Nachate
> **Date:** February 2026
> **Version:** 2.0

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Production Deployment Architecture (C4 Diagram)](#2-production-deployment-architecture-c4-diagram)
3. [Accounts & Credentials](#3-accounts--credentials)
   - 3.1 [DigitalOcean (Hosting)](#31-digitalocean-hosting)
   - 3.2 [Cloudflare (DNS, Tunnel, R2 Storage)](#32-cloudflare-dns-tunnel-r2-storage)
   - 3.3 [GitHub (Source Code & Container Registry)](#33-github-source-code--container-registry)
   - 3.4 [Auth0 (Authentication)](#34-auth0-authentication)
   - 3.5 [AWS SES (Email Delivery)](#35-aws-ses-email-delivery)
   - 3.6 [Gmail (SMTP Fallback)](#36-gmail-smtp-fallback)
   - 3.7 [Discord (Deployment Notifications)](#37-discord-deployment-notifications)
4. [How the System is Deployed](#4-how-the-system-is-deployed)
   - 4.1 [Automatic Deployment (CI/CD)](#41-automatic-deployment-cicd)
   - 4.2 [Manual Deployment](#42-manual-deployment)
   - 4.3 [What Each Container Does](#43-what-each-container-does)
5. [Environment Configuration (.env File)](#5-environment-configuration-env-file)
6. [Monitoring the System](#6-monitoring-the-system)
   - 6.1 [Checking if the System is Running](#61-checking-if-the-system-is-running)
   - 6.2 [Viewing Logs](#62-viewing-logs)
   - 6.3 [Deployment Notifications (Discord)](#63-deployment-notifications-discord)
   - 6.4 [GitHub Actions Dashboard](#64-github-actions-dashboard)
7. [Restarting & Redeploying](#7-restarting--redeploying)
   - 7.1 [Restarting a Single Service](#71-restarting-a-single-service)
   - 7.2 [Restarting All Services](#72-restarting-all-services)
   - 7.3 [Full Redeployment](#73-full-redeployment)
   - 7.4 [Rolling Back to a Previous Version](#74-rolling-back-to-a-previous-version)
8. [Database Management](#8-database-management)
   - 8.1 [Database Overview](#81-database-overview)
   - 8.2 [Accessing the Database](#82-accessing-the-database)
   - 8.3 [Database Migrations](#83-database-migrations)
   - 8.4 [Backups & Recovery](#84-backups--recovery)
   - 8.5 [Data Corruption Recovery](#85-data-corruption-recovery)
9. [Domain & SSL/TLS](#9-domain--ssltls)
10. [CI/CD Pipeline Details](#10-cicd-pipeline-details)
    - 10.1 [Continuous Integration (CI)](#101-continuous-integration-ci)
    - 10.2 [Continuous Deployment (CD)](#102-continuous-deployment-cd)
    - 10.3 [PR Coverage Checks](#103-pr-coverage-checks)
    - 10.4 [Branch & PR Naming Validation](#104-branch--pr-naming-validation)
    - 10.5 [Stale Branch Cleanup](#105-stale-branch-cleanup)
11. [Troubleshooting](#11-troubleshooting)
12. [Key File Locations](#12-key-file-locations)

---

## 1. System Overview

CourtierPro is a bilingual (English/French) broker–client management platform for Nabizada Courtier Inc. The production system consists of five Docker containers running on a single DigitalOcean Droplet (virtual machine), with traffic routed through a Cloudflare Tunnel for secure, zero-trust HTTPS access.

**Live URL:** [https://www.courtier-pro.ca](https://www.courtier-pro.ca)

**Technology Stack:**

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Frontend | React 19, Vite, TypeScript, Tailwind CSS | Single-page application (SPA) |
| Backend API | Spring Boot 3, Java 17 | REST API, business logic, email, file management |
| Database | PostgreSQL 15 | Persistent data storage |
| Reverse Proxy | Caddy (Alpine) | Routes `/api/*` to backend, everything else to frontend |
| Tunnel | Cloudflare cloudflared | Secure ingress from the internet to the VM |
| CI/CD | GitHub Actions | Automated build, test, and deployment pipeline |

---

## 2. Production Deployment Architecture (C4 Diagram)

The following diagram illustrates the production deployment topology. The PlantUML source file is located at [`docs/diagrams/CourtierProC4L3Deployment.puml`](diagrams/CourtierProC4L3Deployment.puml) in the repository. Render it with any PlantUML-compatible tool (e.g., the PlantUML VS Code extension, [plantuml.com](https://www.plantuml.com/plantuml/uml), or IntelliJ's built-in renderer) to produce the diagram image.

**Figure 1 — C4 Level 3 Production Deployment Diagram**

> *Render `docs/diagrams/CourtierProC4L3Deployment.puml` and insert the resulting image here.*

The diagram shows the following components and their relationships:

- **User Device** — The end user's browser running the React SPA.
- **Cloudflare Edge Network** — Handles DNS resolution, DDoS protection, and SSL/TLS termination for `courtier-pro.ca`.
- **DigitalOcean Droplet (Ubuntu 24.04 LTS)** — The single production VM running Docker Engine with five containers:
  - **Cloudflare Tunnel** (`cloudflare/cloudflared`) — Receives traffic from the Cloudflare edge via an encrypted, outbound-only tunnel.
  - **Caddy** (`caddy:alpine`) — Reverse proxy listening on port 80. Routes `/api/*` requests to the backend and all other requests to the frontend.
  - **CourtierPro Frontend** (Nginx + React SPA) — Serves the static single-page application on port 80.
  - **CourtierPro Backend** (Spring Boot 3 / Java 17) — REST API and business logic on port 8080.
  - **PostgreSQL 15** — Relational database with persistent data stored in a Docker named volume.
- **External Services:**
  - **Auth0** — Identity provider for authentication and role-based access control.
  - **Cloudflare R2** — S3-compatible object storage for uploaded documents.
  - **AWS SES** — Transactional email delivery service.
  - **GitHub Container Registry (GHCR)** — Hosts the built Docker images pulled during deployment.

### How Traffic Flows

1. A user opens `https://www.courtier-pro.ca` in their browser.
2. Cloudflare DNS resolves the domain and routes the request through the **Cloudflare Edge Network**, which handles SSL/TLS termination and DDoS protection.
3. The encrypted request enters the DigitalOcean Droplet via the **Cloudflare Tunnel** (`cloudflared` container). This means the VM does not need any public-facing ports — all ingress is through the secure tunnel.
4. The tunnel forwards traffic to **Caddy** (reverse proxy), which inspects the URL path:
   - Requests to `/api/*` are forwarded to the **Backend** (Spring Boot on port 8080).
   - All other requests are forwarded to the **Frontend** (Nginx serving the React SPA on port 80).
5. The **Backend** connects to the **PostgreSQL** database over the internal Docker network (never exposed to the internet).
6. The Backend also communicates with external services as needed:
   - **Auth0** for token validation and user management.
   - **Cloudflare R2** for document storage (S3-compatible API).
   - **AWS SES** for sending transactional emails (notifications, reminders).

---

## 3. Accounts & Credentials

> **Note:** For the submitted version of this document, credentials are shown as `<placeholder>`. The client copy includes actual credentials.

### 3.1 DigitalOcean (Hosting)

The application runs on a DigitalOcean Droplet (virtual private server).

| Item | Value |
|------|-------|
| Provider | [DigitalOcean](https://cloud.digitalocean.com) |
| Droplet OS | Ubuntu 24.04 LTS |
| Region | (as configured in your DO dashboard) |
| SSH Access | `ssh <your-ssh-user>@<droplet-ip-address>` |
| SSH Key | `<path-to-private-ssh-key>` |
| App Directory | `/home/<your-ssh-user>/app/` |
| Backups | DigitalOcean automated Droplet backups enabled |

**How to access the VM:**

```bash
ssh -i <path-to-private-ssh-key> <your-ssh-user>@<droplet-ip-address>
```

Once connected, the application files are located at:

```
/home/<your-ssh-user>/app/
├── docker-compose.prod.yml    # Docker Compose orchestration file
├── Caddyfile                  # Caddy reverse proxy configuration
└── .env                       # Environment variables and secrets
```

### 3.2 Cloudflare (DNS, Tunnel, R2 Storage)

Cloudflare manages DNS, the secure tunnel into the server, and R2 object storage for documents.

| Item | Value |
|------|-------|
| Provider | [Cloudflare Dashboard](https://dash.cloudflare.com) |
| Account Email | `<cloudflare-account-email>` |
| Account Password | `<cloudflare-account-password>` |
| Domain | `courtier-pro.ca` |
| Tunnel Name | (visible in Cloudflare Zero Trust > Networks > Tunnels) |
| Tunnel Token | `<tunnel-token>` |
| R2 Bucket Name | `courtierpro-prod` |
| R2 Endpoint | `https://<account-id>.r2.cloudflarestorage.com` |
| R2 Access Key ID | `<r2-access-key-id>` |
| R2 Secret Access Key | `<r2-secret-access-key>` |

**Cloudflare Tunnel** creates a secure, outbound-only connection from the Droplet to Cloudflare's edge. This means:
- No public ports (80, 443) need to be open on the VM firewall.
- All traffic is encrypted end-to-end.
- DDoS protection is handled by Cloudflare automatically.

**Cloudflare R2** is an S3-compatible object storage service used for storing client-uploaded documents (financing letters, inspection reports, offer attachments, etc.). The backend generates pre-signed URLs for secure uploads and downloads.

### 3.3 GitHub (Source Code & Container Registry)

Source code is hosted on GitHub. Docker images are automatically built and published to the GitHub Container Registry (GHCR) on every push to `main`.

| Item | Value |
|------|-------|
| Repository | [https://github.com/CourtierPro/CourtierPro](https://github.com/CourtierPro/CourtierPro) |
| Organization | CourtierPro |
| Backend Image | `ghcr.io/courtierpro/courtierpro-backend:<commit-sha>` |
| Frontend Image | `ghcr.io/courtierpro/courtierpro-frontend:<commit-sha>` |
| Backend Packages | [ghcr.io/courtierpro/courtierpro-backend](https://github.com/CourtierPro/CourtierPro/pkgs/container/courtierpro-backend) |
| Frontend Packages | [ghcr.io/courtierpro/courtierpro-frontend](https://github.com/CourtierPro/CourtierPro/pkgs/container/courtierpro-frontend) |
| GitHub Account Email | `<github-account-email>` |
| GitHub Account Password | `<github-account-password>` |

**GitHub App** (used for user feedback integration):

| Item | Value |
|------|-------|
| App ID | `<github-app-id>` |
| Installation ID | `<github-app-installation-id>` |
| Private Key | `<github-app-private-key>` (RSA PEM format) |

Images are tagged with the Git commit SHA (e.g., `356698e5...`). The `.env` file on the server tracks which version is currently deployed via the `IMAGE_TAG` variable.

### 3.4 Auth0 (Authentication)

Auth0 handles all user authentication, role management, and access control.

| Item | Value |
|------|-------|
| Provider | [Auth0 Dashboard](https://manage.auth0.com) |
| Tenant Domain | `<auth0-tenant>.us.auth0.com` |
| Dashboard Email | `<auth0-dashboard-email>` |
| Dashboard Password | `<auth0-dashboard-password>` |
| API Audience | `https://api.courtierpro.dev` |
| Frontend Client ID | `<auth0-frontend-client-id>` |
| Management API Client ID | `<auth0-mgmt-client-id>` |
| Management API Client Secret | `<auth0-mgmt-client-secret>` |
| Management API Audience | `https://<auth0-tenant>.us.auth0.com/api/v2/` |

**User Roles in Auth0:**
- **ADMIN** — System administrator with full access.
- **BROKER** — Licensed real estate broker managing transactions.
- **CLIENT** — Buyer or seller interacting with the brokerage.

To manage users (add, remove, change roles), log into the Auth0 Dashboard, navigate to **User Management > Users**, and adjust as needed.

### 3.5 AWS SES (Email Delivery)

In production, transactional emails (document requests, stage updates, appointment confirmations, reminders) are sent via AWS Simple Email Service (SES).

| Item | Value |
|------|-------|
| Provider | [AWS Console — SES](https://console.aws.amazon.com/ses/) |
| AWS Region | `us-east-1` |
| SES Access Key ID | `<aws-ses-access-key-id>` |
| SES Secret Access Key | `<aws-ses-secret-access-key>` |
| From Address | `<email-from-address>` |

### 3.6 Gmail (SMTP Fallback)

A Gmail account is configured as a fallback/dev email provider.

| Item | Value |
|------|-------|
| Email | `<gmail-address>` |
| App Password | `<gmail-app-password>` |

> In production, the `application-prod.yml` profile sets `app.email.provider: ses`, so AWS SES is used. Gmail is the default for local development.

### 3.7 Discord (Deployment Notifications)

A Discord webhook is configured to send deployment status notifications to a team channel.

| Item | Value |
|------|-------|
| Webhook URL | Stored as GitHub Secret `DISCORD_WEBHOOK_URL` |

Notifications are sent for:
- **Successful deployments** (green)
- **Rollback after failed deployment** (orange)
- **Complete deployment failure** (red)

---

## 4. How the System is Deployed

### 4.1 Automatic Deployment (CI/CD)

The production system is deployed **automatically** every time code is pushed to the `main` branch on GitHub. The workflow is defined in `.github/workflows/deploy.yml` and performs the following steps:

1. **Build Phase** (runs on GitHub's servers):
   - Checks out the source code.
   - Builds the **backend** Docker image from `backend/Dockerfile` (multi-stage: Gradle build → JRE runtime).
   - Builds the **frontend** Docker image from `frontend/Dockerfile` (multi-stage: Node build → Nginx serving static files). Vite environment variables (API URL, Auth0 config) are injected as build arguments.
   - Pushes both images to GHCR, tagged with the commit SHA.

2. **Deploy Phase** (runs on the DigitalOcean VM via SSH):
   - Copies the latest `docker-compose.prod.yml` and `Caddyfile` to the server.
   - Saves the current `IMAGE_TAG` from `.env` as a rollback target.
   - Updates `.env` with the new commit SHA.
   - Logs into GHCR on the server.
   - Pulls the new images and restarts all containers with `docker compose up -d`.
   - Waits 60 seconds for the Spring Boot application to start.
   - Performs a health check against `http://localhost:80/api/actuator/health` (up to 15 retries, 15 seconds apart).
   - If healthy: deployment succeeds, a green Discord notification is sent.
   - If unhealthy: **automatic rollback** — the previous version is restored, containers are restarted, and an orange/red Discord notification is sent.

3. **Can also be triggered manually** via GitHub Actions > "Deploy to EC2 with Rollback" > "Run workflow".

### 4.2 Manual Deployment

If you need to deploy manually (e.g., the CI/CD pipeline is unavailable), SSH into the server and run:

```bash
ssh -i <key> <user>@<ip>
cd ~/app

# 1. Log into the container registry
echo "<github-personal-access-token>" | docker login ghcr.io -u <github-username> --password-stdin

# 2. Update the IMAGE_TAG in .env to the desired commit SHA
# (Find commit SHAs at https://github.com/CourtierPro/CourtierPro/commits/main)
nano .env
# Change IMAGE_TAG=<new-commit-sha>

# 3. Pull and start
docker compose --env-file .env -f docker-compose.prod.yml pull
docker compose --env-file .env -f docker-compose.prod.yml up -d --remove-orphans

# 4. Verify health
curl http://localhost:80/api/actuator/health
```

### 4.3 What Each Container Does

The production `docker-compose.prod.yml` defines five services:

| Container Name | Image | Purpose |
|---------------|-------|---------|
| `courtierpro_tunnel` | `cloudflare/cloudflared:latest` | Secure tunnel from Cloudflare Edge to the VM |
| `courtierpro_caddy` | `caddy:alpine` | Reverse proxy — routes `/api/*` to backend, everything else to frontend |
| `courtierpro_backend` | `ghcr.io/courtierpro/courtierpro-backend:<sha>` | Spring Boot REST API (port 8080 internally) |
| `courtierpro_frontend` | `ghcr.io/courtierpro/courtierpro-frontend:<sha>` | Nginx serving the React SPA (port 80 internally) |
| `courtierpro_db` | `postgres:15` | PostgreSQL database (port 5432, internal only) |

**Startup order:** PostgreSQL starts first (with a health check). The backend waits until the database is healthy. Caddy depends on both the backend and frontend. The tunnel depends on Caddy.

---

## 5. Environment Configuration (.env File)

The `.env` file on the server (`/home/<user>/app/.env`) contains all secrets and configuration. Below is the complete list of variables with descriptions:

| Variable | Description |
|----------|-------------|
| `IMAGE_TAG` | Git commit SHA of the currently deployed version |
| `GITHUB_REPO_OWNER` | GitHub organization name (`courtierpro`) |
| `GITHUB_REPO_NAME` | GitHub repository name (`courtierpro`) |
| `TUNNEL_TOKEN` | Cloudflare Tunnel authentication token |
| `DB_HOST` | Database hostname (`db` — Docker internal service name) |
| `DB_PORT` | Database port (`5432`) |
| `DB_USER` | PostgreSQL username |
| `DB_PASS` | PostgreSQL password |
| `AWS_ACCESS_KEY_ID` | Cloudflare R2 access key (S3-compatible) |
| `AWS_SECRET_ACCESS_KEY` | Cloudflare R2 secret key |
| `AWS_REGION` | Cloud region (`auto` for R2) |
| `AWS_S3_BUCKET` | R2 bucket name (`courtierpro-prod`) |
| `AWS_S3_ENDPOINT` | R2 endpoint URL |
| `AUTH0_DOMAIN` | Auth0 tenant domain |
| `AUTH0_AUDIENCE` | Auth0 API audience identifier |
| `AUTH0_MGMT_CLIENT_ID` | Auth0 Management API client ID |
| `AUTH0_MGMT_CLIENT_SECRET` | Auth0 Management API client secret |
| `AUTH0_MGMT_AUDIENCE` | Auth0 Management API audience |
| `GMAIL_USERNAME` | Gmail address for SMTP fallback |
| `GMAIL_PASSWORD` | Gmail app password |
| `GITHUB_APP_ID` | GitHub App ID for feedback integration |
| `GITHUB_APP_INSTALLATION_ID` | GitHub App installation ID |
| `GITHUB_APP_PRIVATE_KEY` | GitHub App RSA private key (PEM format) |
| `VITE_API_URL` | Frontend API base URL (`https://www.courtier-pro.ca/api`) |
| `VITE_AUTH0_CALLBACK_URL` | Auth0 callback URL (`https://www.courtier-pro.ca`) |
| `VITE_AUTH0_AUDIENCE` | Auth0 audience for the frontend |
| `VITE_AUTH0_DOMAIN` | Auth0 domain for the frontend |
| `VITE_AUTH0_CLIENT_ID` | Auth0 client ID for the frontend SPA |
| `FRONTEND_URL` | Full frontend URL (used in email links) |
| `EMAIL_FROM_ADDRESS` | Sender address for outbound emails |
| `AWS_SES_ACCESS_KEY_ID` | AWS SES access key for email sending |
| `AWS_SES_SECRET_ACCESS_KEY` | AWS SES secret key |

> **Important:** Never commit the `.env` file to Git. It is listed in `.gitignore`. The CI/CD pipeline manages it on the server via GitHub Secrets.

---

## 6. Monitoring the System

### 6.1 Checking if the System is Running

**From anywhere (public health check):**

Open [https://www.courtier-pro.ca/api/actuator/health](https://www.courtier-pro.ca/api/actuator/health) in a browser. A healthy response looks like:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

If you get a timeout or error, the system may be down.

**From the server (SSH):**

```bash
ssh -i <key> <user>@<ip>
cd ~/app

# Check all container statuses
docker compose -f docker-compose.prod.yml ps

# Expected output: all 5 containers should show "Up" status
# courtierpro_tunnel    - Up
# courtierpro_caddy     - Up
# courtierpro_backend   - Up
# courtierpro_frontend  - Up
# courtierpro_db        - Up (healthy)

# Quick health check from inside the server
curl http://localhost:80/api/actuator/health
```

### 6.2 Viewing Logs

SSH into the server and use Docker Compose to view logs:

```bash
cd ~/app

# View logs for ALL containers (last 100 lines, follow in real-time)
docker compose -f docker-compose.prod.yml logs --tail=100 -f

# View logs for a SPECIFIC container
docker compose -f docker-compose.prod.yml logs --tail=100 -f backend
docker compose -f docker-compose.prod.yml logs --tail=100 -f frontend
docker compose -f docker-compose.prod.yml logs --tail=100 -f db
docker compose -f docker-compose.prod.yml logs --tail=100 -f caddy
docker compose -f docker-compose.prod.yml logs --tail=100 -f tunnel

# View logs since a specific time
docker compose -f docker-compose.prod.yml logs --since="2026-02-20T10:00:00" backend

# Search logs for errors
docker compose -f docker-compose.prod.yml logs backend | grep -i "error"
```

Press `Ctrl+C` to stop following logs.

**What to look for in the logs:**
- **Backend:** Java stack traces, `ERROR` or `WARN` level messages, database connection issues.
- **Frontend:** Nginx access/error logs (404 errors, upstream timeouts).
- **Database:** Connection refused errors, out-of-memory warnings, slow queries.
- **Caddy:** Upstream connection failures, timeout errors.
- **Tunnel:** Cloudflare connectivity issues, token expiry warnings.

### 6.3 Deployment Notifications (Discord)

Every deployment sends a notification to the configured Discord channel:

- **Green (Success):** The new version was deployed and passed the health check.
- **Orange (Rollback):** The new version failed the health check, but the system was automatically rolled back to the previous working version. The system is operational but running the older version.
- **Red (Failure):** Both the deployment and rollback failed. Manual intervention is required (see [Troubleshooting](#11-troubleshooting)).

### 6.4 GitHub Actions Dashboard

You can monitor all CI/CD pipeline runs at:
[https://github.com/CourtierPro/CourtierPro/actions](https://github.com/CourtierPro/CourtierPro/actions)

This shows:
- Build and deployment status for each commit.
- CI test results for pull requests.
- Code coverage reports.
- Stale branch cleanup logs.

---

## 7. Restarting & Redeploying

### 7.1 Restarting a Single Service

If only one part of the system is misbehaving (e.g., the backend is unresponsive but the database is fine):

```bash
ssh -i <key> <user>@<ip>
cd ~/app

# Restart just the backend
docker compose -f docker-compose.prod.yml restart backend

# Restart just the frontend
docker compose -f docker-compose.prod.yml restart frontend

# Restart just the database (WARNING: causes brief downtime)
docker compose -f docker-compose.prod.yml restart db

# Restart the tunnel (if site is unreachable externally)
docker compose -f docker-compose.prod.yml restart tunnel

# Restart Caddy (if routing seems broken)
docker compose -f docker-compose.prod.yml restart caddy
```

### 7.2 Restarting All Services

To restart every container without changing versions:

```bash
cd ~/app
docker compose -f docker-compose.prod.yml down
docker compose --env-file .env -f docker-compose.prod.yml up -d
```

### 7.3 Full Redeployment

To redeploy the latest version from scratch (pulling fresh images):

```bash
cd ~/app

# Log into the registry
echo "<github-token>" | docker login ghcr.io -u <github-username> --password-stdin

# Pull latest images and recreate containers
docker compose --env-file .env -f docker-compose.prod.yml pull
docker compose --env-file .env -f docker-compose.prod.yml up -d --remove-orphans

# Verify
curl http://localhost:80/api/actuator/health
```

Alternatively, trigger a redeployment from GitHub Actions without any code changes:
1. Go to [Actions > Deploy to EC2 with Rollback](https://github.com/CourtierPro/CourtierPro/actions/workflows/deploy.yml).
2. Click **"Run workflow"** > select `main` branch > click **"Run workflow"**.

### 7.4 Rolling Back to a Previous Version

If the latest deployment has issues and you need to go back to a known-good version:

```bash
cd ~/app

# 1. Find a working commit SHA from the GitHub packages page:
#    https://github.com/CourtierPro/CourtierPro/pkgs/container/courtierpro-backend
#    Each tagged version shows the commit SHA.

# 2. Update the .env file
nano .env
# Change IMAGE_TAG=<known-good-commit-sha>

# 3. Pull and restart
docker compose --env-file .env -f docker-compose.prod.yml pull
docker compose --env-file .env -f docker-compose.prod.yml up -d --remove-orphans

# 4. Verify health
curl http://localhost:80/api/actuator/health
```

> The CI/CD pipeline also performs automatic rollback if a deployment fails the health check.

---

## 8. Database Management

### 8.1 Database Overview

| Item | Value |
|------|-------|
| Engine | PostgreSQL 15 |
| Database Name | `courtierpro` |
| Username | `<db-user>` |
| Password | `<db-password>` |
| Container Name | `courtierpro_db` |
| Data Volume | Docker named volume `pgdata` |
| Port | 5432 (internal Docker network only — not exposed to the internet) |

The database schema is managed by **Flyway** migrations. The backend automatically runs pending migrations on startup. Current migrations:

| Migration | Description |
|-----------|-------------|
| `V1__init_schema.sql` | Initial database schema |
| `V2__house_visits.sql` | House visits feature |
| `V3__selling_house_visits.sql` | Selling-side house visits |
| `V4__add_analytics_export_audit_table.sql` | Analytics export audit log |
| `V5__add_weekly_digest_toggle.sql` | Weekly digest email preference |

### 8.2 Accessing the Database

**From the server via command line:**

```bash
ssh -i <key> <user>@<ip>

# Open a PostgreSQL interactive shell
docker exec -it courtierpro_db psql -U <db-user> -d courtierpro

# Useful commands once connected:
# \dt             — List all tables
# \d <table>      — Describe a table's columns
# SELECT * FROM flyway_schema_history;  — View migration history
# \q              — Quit
```

**From the server via a one-off query:**

```bash
docker exec -it courtierpro_db psql -U <db-user> -d courtierpro -c "SELECT count(*) FROM users;"
```

### 8.3 Database Migrations

Database migrations are **automatic**. When the backend container starts, Spring Boot + Flyway checks for any new migration files in `backend/src/main/resources/migration/` and applies them in order.

**If a migration fails:**
1. Check the backend logs: `docker compose -f docker-compose.prod.yml logs backend`
2. The failing migration will be recorded in the `flyway_schema_history` table with a `success = false` entry.
3. Fix the migration SQL, then clear the failed entry:

```sql
DELETE FROM flyway_schema_history WHERE success = false;
```

4. Restart the backend: `docker compose -f docker-compose.prod.yml restart backend`

**To add a new migration:**
1. Create a new file following the naming convention: `V<next-number>__<description>.sql`
2. Place it in `backend/src/main/resources/migration/`
3. Commit and push to `main` — the CI/CD pipeline will deploy and Flyway will run the new migration automatically.

### 8.4 Backups & Recovery

**DigitalOcean Automated Backups:**

DigitalOcean automated Droplet backups are enabled. These create full snapshots of the entire VM (including the Docker volume where PostgreSQL stores its data) on a regular schedule.

To restore from a DigitalOcean backup:
1. Log into [DigitalOcean Dashboard](https://cloud.digitalocean.com).
2. Navigate to the Droplet > **Backups** tab.
3. Select the backup snapshot you want to restore.
4. Click **Restore Droplet**.

**Manual Database Backup (pg_dump):**

For more granular database-only backups, run:

```bash
# Create a backup
docker exec courtierpro_db pg_dump -U <db-user> -d courtierpro > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore from a backup
docker exec -i courtierpro_db psql -U <db-user> -d courtierpro < backup_20260220_120000.sql
```

**Recommended backup routine:** Run a `pg_dump` before any major deployment or database migration as an extra safety net.

### 8.5 Data Corruption Recovery

If the database becomes corrupted or data is lost:

1. **Stop the backend** to prevent further writes:
   ```bash
   docker compose -f docker-compose.prod.yml stop backend
   ```

2. **Assess the damage:**
   ```bash
   docker exec -it courtierpro_db psql -U <db-user> -d courtierpro
   # Run queries to check data integrity
   ```

3. **Option A — Restore from pg_dump backup:**
   ```bash
   # Drop and recreate the database
   docker exec -it courtierpro_db psql -U <db-user> -c "DROP DATABASE courtierpro;"
   docker exec -it courtierpro_db psql -U <db-user> -c "CREATE DATABASE courtierpro;"
   docker exec -i courtierpro_db psql -U <db-user> -d courtierpro < backup_file.sql
   ```

4. **Option B — Restore from DigitalOcean snapshot** (restores the entire VM).

5. **Restart the backend:**
   ```bash
   docker compose -f docker-compose.prod.yml start backend
   ```

---

## 9. Domain & SSL/TLS

| Item | Value |
|------|-------|
| Domain | `courtier-pro.ca` |
| Registrar | Cloudflare |
| DNS Provider | Cloudflare |
| SSL/TLS | Managed automatically by Cloudflare (edge certificates) |

The domain is registered and managed through Cloudflare. SSL/TLS certificates are provisioned and renewed automatically by Cloudflare — no manual certificate management is required.

**DNS Configuration:**

The domain's DNS records point to the Cloudflare Tunnel (not directly to the Droplet's IP). This means:
- The server's real IP address is never exposed publicly.
- SSL termination happens at the Cloudflare edge.
- The tunnel handles the encrypted connection from Cloudflare to the server.

**If the domain expires or DNS needs to be updated:**
1. Log into [Cloudflare Dashboard](https://dash.cloudflare.com).
2. Select the `courtier-pro.ca` domain.
3. Navigate to **DNS** to view/edit records, or **Domain Registration** for renewal.

---

## 10. CI/CD Pipeline Details

The repository uses five GitHub Actions workflows located in `.github/workflows/`:

### 10.1 Continuous Integration (CI)

**File:** `.github/workflows/ci.yml`
**Triggers:** Push to `main` or pull requests targeting `main` (only when `backend/` or `frontend/` files change).

Runs two jobs in parallel:
- **backend-check:** Sets up JDK 17, runs `./gradlew clean build` (compiles and runs all tests).
- **frontend-check:** Sets up Node 20, runs `npm ci`, `npm run lint`, and `npm run build` (linting, type checking, and production build).

### 10.2 Continuous Deployment (CD)

**File:** `.github/workflows/deploy.yml`
**Triggers:** Push to `main` or manual dispatch.

See [Section 4.1](#41-automatic-deployment-cicd) for a detailed explanation of the deployment process including the automatic rollback mechanism.

**GitHub Secrets required for deployment:**

| Secret | Purpose |
|--------|---------|
| `EC2_HOST` | Droplet IP address |
| `EC2_USER` | SSH username |
| `EC2_SSH_KEY` | SSH private key |
| `GITHUB_TOKEN` | Auto-provided by GitHub for GHCR access |
| `VITE_API_URL` | Frontend API URL (build arg) |
| `VITE_AUTH0_AUDIENCE` | Auth0 audience (build arg) |
| `VITE_AUTH0_DOMAIN` | Auth0 domain (build arg) |
| `VITE_AUTH0_CLIENT_ID` | Auth0 client ID (build arg) |
| `VITE_AUTH0_CALLBACK_URL` | Auth0 callback URL (build arg) |
| `DISCORD_WEBHOOK_URL` | Discord notification webhook |
| `AWS_SES_ACCESS_KEY_ID` | SES credentials (injected into .env) |
| `AWS_SES_SECRET_ACCESS_KEY` | SES credentials (injected into .env) |

**GitHub Variables:**

| Variable | Purpose |
|----------|---------|
| `FRONTEND_URL` | Full frontend URL for email links |
| `EMAIL_FROM_ADDRESS` | Sender address for emails |

### 10.3 PR Coverage Checks

**File:** `.github/workflows/pr-coverage.yml`
**Triggers:** Pull request opened, synchronized, or reopened.

Runs backend tests with JaCoCo code coverage and posts a coverage report as a PR comment. Enforces a minimum of **90% overall coverage** and **90% changed-files coverage**.

### 10.4 Branch & PR Naming Validation

**File:** `.github/workflows/validate-naming.yml`
**Triggers:** Pull request opened, edited, synchronized, or reopened.

Enforces naming conventions:
- **Branch names:** `<type>/CP-<ticket>_<Description>` (e.g., `feat/CP-44_AddDocumentUpload`) or `untracked/<Description>`
- **PR titles:** `<Type>(CP-<ticket>): Description` (e.g., `Feat(CP-44): Add document upload`) or `Untracked: Description`
- Valid types: `bug`, `feat`, `fix`, `story`, `task`, `test`

### 10.5 Stale Branch Cleanup

**File:** `.github/workflows/cleanup-stale-branches.yml`
**Triggers:** Daily at 3:00 AM UTC (dry-run only) or manual dispatch.

Automatically identifies and cleans up branches that have been inactive for 14+ days. Protects `main`, `master`, `develop`, `staging`, and `production` branches. Skips branches with open pull requests.

---

## 11. Troubleshooting

| Problem | Likely Cause | Solution |
|---------|-------------|----------|
| Site is completely unreachable | Cloudflare Tunnel is down | SSH in and run `docker compose -f docker-compose.prod.yml restart tunnel` |
| Site loads but shows a blank page | Frontend container crashed | `docker compose -f docker-compose.prod.yml restart frontend` |
| API calls return 502 Bad Gateway | Backend is not running or still starting | Check backend logs: `docker compose -f docker-compose.prod.yml logs backend`. Wait 60–90 seconds after restart for Spring Boot startup. |
| "Unable to connect to database" in logs | PostgreSQL container crashed | `docker compose -f docker-compose.prod.yml restart db`, then `docker compose -f docker-compose.prod.yml restart backend` |
| Login redirects fail | Auth0 configuration mismatch | Verify `VITE_AUTH0_DOMAIN` and `VITE_AUTH0_CLIENT_ID` in `.env`. Check the Auth0 Dashboard for correct callback URLs. |
| Document uploads fail | Cloudflare R2 credentials expired/wrong | Verify `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_S3_ENDPOINT` in `.env`. Check R2 bucket exists in Cloudflare dashboard. |
| Emails not being sent | AWS SES credentials or sending limits | Check `AWS_SES_ACCESS_KEY_ID` and `AWS_SES_SECRET_ACCESS_KEY`. Verify the sender email is verified in AWS SES console. |
| Deployment fails in GitHub Actions | SSH key or host mismatch | Check `EC2_HOST`, `EC2_USER`, and `EC2_SSH_KEY` in GitHub repository Secrets. Ensure the Droplet's SSH service is running. |
| Disk space full on VM | Docker images or database volume | Run `docker system prune -a --volumes` to clean unused images (careful: removes unused volumes). Check with `df -h`. |
| Backend takes too long to start | Not enough memory / JVM issues | Check Droplet memory usage with `free -h`. Consider upgrading the Droplet size. |

**General diagnostic commands:**

```bash
# Check overall system resources
htop                              # CPU and memory (install with: apt install htop)
df -h                             # Disk usage
free -h                           # Memory usage

# Check Docker resources
docker system df                  # Docker disk usage
docker stats --no-stream          # Container resource usage (CPU, memory)

# Check all containers
docker compose -f docker-compose.prod.yml ps

# Nuclear restart (stop everything, clean up, restart)
cd ~/app
docker compose -f docker-compose.prod.yml down
docker system prune -f            # Remove dangling images and stopped containers
docker compose --env-file .env -f docker-compose.prod.yml pull
docker compose --env-file .env -f docker-compose.prod.yml up -d
```

---

## 12. Key File Locations

### On the Server (`/home/<user>/app/`)

| File | Purpose |
|------|---------|
| `docker-compose.prod.yml` | Defines all 5 production containers and their configuration |
| `Caddyfile` | Reverse proxy routing rules (`/api/*` → backend, `/*` → frontend) |
| `.env` | All environment variables and secrets |
| `.env.bak` | Automatic backup created during deployments (deleted on success) |

### In the GitHub Repository

| Path | Purpose |
|------|---------|
| `.github/workflows/deploy.yml` | CI/CD deployment pipeline with rollback |
| `.github/workflows/ci.yml` | Continuous integration (build + test) |
| `.github/workflows/pr-coverage.yml` | PR code coverage enforcement |
| `.github/workflows/validate-naming.yml` | Branch and PR naming convention check |
| `.github/workflows/cleanup-stale-branches.yml` | Stale branch auto-cleanup |
| `backend/Dockerfile` | Backend multi-stage Docker build (Gradle → JRE) |
| `frontend/Dockerfile` | Frontend multi-stage Docker build (Node → Nginx) |
| `frontend/nginx.conf` | Nginx config for SPA routing (fallback to `index.html`) |
| `frontend/docker-entrypoint.sh` | Runtime injection of environment variables into `config.js` |
| `backend/src/main/resources/application.yml` | Default Spring Boot config |
| `backend/src/main/resources/application-prod.yml` | Production Spring Boot config overrides |
| `backend/src/main/resources/migration/` | Flyway database migration SQL files |
| `docs/diagrams/CourtierProC4L3Deployment.puml` | C4 Level 3 Deployment diagram (PlantUML source) |

---

> **For questions or emergencies**, contact the development team via the CourtierPro GitHub repository [Issues page](https://github.com/CourtierPro/CourtierPro/issues) or reach out to the team members listed in the repository README.
