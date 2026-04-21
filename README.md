# NotifyHub — Backend

A production-grade, real-time event-driven notification system built with Spring Boot 3. Publishes events via a REST API, routes them asynchronously through RabbitMQ, and delivers notifications to subscribers over WebSocket and Email.

---

## Architecture

```
REST API  ──▶  PostgreSQL (persist event)
          ──▶  RabbitMQ   (publish after commit)
                  │
                  ▼
            Consumer Service
                  │
          ┌───────┴────────┐
          ▼                ▼
     WebSocket          Email
  (SockJS/STOMP)    (Gmail SMTP)
          │
          ▼
     Browser (live)
```

Events are persisted before being queued. RabbitMQ publishing happens **after** the database transaction commits, eliminating the race condition where a consumer could read a non-existent row.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT) |
| Database | PostgreSQL 15 |
| Message Broker | RabbitMQ 3 |
| Cache + Rate Limit | Redis 7 |
| Real-time | WebSocket (SockJS + STOMP) |
| Email | Spring Mail (JavaMail / Gmail SMTP) |
| Containerisation | Docker + Docker Compose |

---

## Features

- **JWT Auth** — Stateless access + refresh token flow. Tokens validated on every request via a Spring Security filter.
- **Async Event Processing** — HTTP call returns immediately. RabbitMQ consumer handles delivery in the background.
- **WebSocket Delivery** — Subscribers connected to the browser receive live push notifications via STOMP topic `/topic/notifications/{userId}`.
- **Email Delivery** — HTML email sent via Gmail SMTP using JavaMailSender. Delivery status tracked in `notification_logs`.
- **Redis Caching** — Event lookups cached with 5-minute TTL. Subscription list cached with 10-minute TTL.
- **Rate Limiting** — Per-user sliding window rate limit (10 requests / 60 seconds) enforced in a Spring MVC interceptor using Redis `INCR` + `EXPIRE`.
- **Dead Letter Queue** — Failed deliveries are retried up to 3 times, then routed to a DLQ for inspection.
- **Transactional Outbox Pattern** — RabbitMQ publish is registered via `TransactionSynchronizationManager.afterCommit()` to guarantee DB visibility before the consumer runs.

---

## Project Structure

```
src/main/java/com/notifyhub/
├── config/
│   ├── RabbitMQConfig.java          # Exchange, queue, DLQ, binding setup
│   ├── RedisConfig.java             # RedisTemplate + CacheManager with TTLs
│   ├── SecurityConfig.java          # JWT filter chain + configurable CORS
│   └── WebMvcConfig.java            # Rate limit interceptor registration
├── controller/
│   ├── AuthController.java          # /api/auth/** (login, register, refresh)
│   ├── EventController.java         # /api/events/** (publish, list, get)
│   └── SubscriptionController.java  # /api/subscriptions/**
├── messaging/
│   ├── NotificationProducer.java    # Publishes messages to RabbitMQ
│   └── NotificationConsumer.java    # Listens, acks/nacks, routes to DLQ
├── model/
│   ├── User.java
│   ├── NotificationEvent.java       # Status: PENDING → QUEUED → DELIVERED/FAILED
│   ├── Subscription.java            # User + EventType + Channel
│   └── NotificationLog.java         # Per-delivery attempt log
├── ratelimit/
│   └── RateLimitInterceptor.java    # Redis INCR sliding window
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   └── WebSocketJwtHandshakeInterceptor.java
└── service/
    ├── AuthService.java
    ├── EventService.java            # Transactional publish + afterCommit hook
    ├── SubscriptionService.java
    ├── NotificationDeliveryService.java  # Routes to WebSocket or Email
    └── EmailService.java            # JavaMail HTML email builder
```

---

## Getting Started

### Prerequisites

- Docker + Docker Compose
- Gmail account with 2-Step Verification enabled (for email delivery)
- Gmail [App Password](https://myaccount.google.com/apppasswords)

### Setup

```bash
# Clone the repo
git clone https://github.com/mukulbhatia799/NotifyHub-Backend.git
cd NotifyHub-Backend

# Copy the example env file and fill in your values
cp .env.example .env
```

Edit `.env`:

```env
# JWT — use any long random string
JWT_SECRET=your_jwt_secret_here

# Mail — Gmail App Password (no spaces)
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=yourapppasword

# CORS — comma-separated list of allowed frontend origins
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### Run with Docker Compose

```bash
# Start all services (PostgreSQL, RabbitMQ, Redis, App)
docker compose up --build -d

# View logs
docker compose logs -f app
```

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| RabbitMQ Management | http://localhost:15672 |

### Stop

```bash
docker compose down
```

---

## API Reference

### Auth

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login, returns access + refresh tokens |
| POST | `/api/auth/refresh` | Exchange refresh token for new access token |

### Events

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/events/publish` | Publish a new notification event |
| GET | `/api/events` | List events (paginated, filterable by status/type) |
| GET | `/api/events/{id}` | Get event detail with delivery logs |

### Subscriptions

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/subscriptions` | List my active subscriptions |
| POST | `/api/subscriptions` | Create a subscription |
| DELETE | `/api/subscriptions/{id}` | Deactivate a subscription |

### Publish Event — Request Body

```json
{
  "eventType": "ORDER_PLACED",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "payload": "{\"orderId\": \"123\", \"amount\": 99.99}",
  "channels": ["WEBSOCKET", "EMAIL"]
}
```

---

## Deployment (Railway)

1. Create a new project on [railway.app](https://railway.app)
2. Add **PostgreSQL** and **Redis** plugins
3. Use [CloudAMQP](https://cloudamqp.com) (free tier) for RabbitMQ
4. Deploy this repo — Railway detects the `Dockerfile` automatically
5. Set all environment variables from `.env.example` in the Railway Variables tab
6. Set `CORS_ALLOWED_ORIGINS` to your Vercel frontend URL

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `notifyhub` |
| `DB_USERNAME` | Database user | `notifyhub` |
| `DB_PASSWORD` | Database password | — |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | — |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ user | — |
| `RABBITMQ_PASSWORD` | RabbitMQ password | — |
| `JWT_SECRET` | JWT signing secret | — |
| `JWT_EXPIRATION` | Access token TTL (ms) | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) | `604800000` (7 days) |
| `MAIL_USERNAME` | Gmail address | — |
| `MAIL_PASSWORD` | Gmail App Password | — |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173` |
