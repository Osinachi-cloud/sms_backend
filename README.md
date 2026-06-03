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
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Deploy to Railway

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
| `DATABASE_URL` | Yes | PostgreSQL connection string (Railway auto-injects this) |
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
