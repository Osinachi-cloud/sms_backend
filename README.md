# SchoolSaaS Backend

Spring Boot 3 + Java 21 backend for the SchoolSaaS platform.

- REST API for multi-tenant school management
- PostgreSQL database with Flyway migrations
- JWT authentication & RBAC
- Paystack payment integration
- OpenAI AI Tutor integration

## Quick Start (Local)

```bash
# 1. Start PostgreSQL
docker run -d -p 5432:5432 -e POSTGRES_DB=schoolsaas -e POSTGRES_USER=schoolsaas -e POSTGRES_PASSWORD=localpass postgres:16-alpine

# 2. Copy env file
cp .env.example .env
# Edit .env with your secrets

# 3. Run
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Deploy to Render (Current)

The backend is currently hosted on **Render**.

1. Create a new Web Service on [render.com](https://render.com)
2. Connect your GitHub repo
3. Set the runtime to **Docker**
4. Add your environment variables from `.env.example`
5. Deploy

The `keep-alive.yml` GitHub Action pings the Render health endpoint to prevent the free-tier instance from sleeping during work hours.

## Deploy to Railway (Alternative)

> Railway deployment is currently **disabled** in this repo. To switch back to Railway, see [`RAILWAY_SETUP.md`](../RAILWAY_SETUP.md).

```bash
# Install Railway CLI
npm install -g @railway/cli
railway login

# Link and deploy
railway link
railway up
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | Yes | PostgreSQL connection string |
| `JWT_SECRET` | Yes | 64+ char random string |
| `PAYSTACK_SECRET_KEY` | Yes | `sk_test_...` or `sk_live_...` |
| `PAYSTACK_PUBLIC_KEY` | No | `pk_test_...` or `pk_live_...` |
| `PAYSTACK_CALLBACK_URL` | Yes | Frontend payment verify URL |
| `OPENAI_API_KEY` | No | For AI Tutor feature |
| `CORS_ALLOWED_ORIGINS` | Yes | Your frontend domain(s) |

## Health Check

`GET /api/health` → `{"status":"UP"}`

## API Documentation

See `docs/` folder or the controllers in `src/main/java/com/schoolsaas/controller/`.
