# ShortifyAPI 🔗

A production-ready distributed URL shortening service built with Java and Spring Boot — similar to Bitly.
Designed with scalability, fault tolerance, and high-throughput in mind.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Redis](https://img.shields.io/badge/Redis-Cache-red)
![Kafka](https://img.shields.io/badge/Kafka-Event--Driven-black)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue)

---

## Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Design Decisions](#design-decisions)
- [API Endpoints](#api-endpoints)
- [How to Run Locally](#how-to-run-locally)
- [Project Structure](#project-structure)
- [Interview Talking Points](#interview-talking-points)

---

## Features

- **URL Shortening** — converts long URLs to short 6-character codes
- **Fast Redirects** — sub-10ms response via Redis Cache-aside pattern
- **Click Analytics** — tracks click count per short URL asynchronously
- **Rate Limiting** — Token Bucket pattern, 5 requests/min per IP via Redis
- **Fault Tolerant** — Circuit Breaker and exponential backoff across services
- **Containerized** — fully dockerized with docker-compose for local setup

---

## Architecture

```
Client
  │
  ├── POST /api/shorten
  │     ├── Check rate limit (Redis Token Bucket)
  │     ├── Save to MySQL (auto-increment ID)
  │     ├── Base62 encode ID → short code
  │     ├── Cache in Redis (TTL 24hrs)
  │     └── Return short URL
  │
  ├── GET /{shortCode}
  │     ├── Check Redis cache (Cache-aside pattern)
  │     ├── Cache HIT  → return original URL directly (sub-10ms)
  │     ├── Cache MISS → fetch from MySQL → store in Redis
  │     ├── Publish click event to Kafka (async, non-blocking)
  │     └── 302 Redirect to original URL
  │
  └── GET /api/stats/{shortCode}
        └── Fetch click count + metadata from MySQL

Kafka Consumer (async)
  └── Consumes click events → increments click count in MySQL
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Database | MySQL (Spring Data JPA) |
| Cache | Redis (Cache-aside pattern) |
| Messaging | Apache Kafka (event-driven) |
| Containerization | Docker + Docker Compose |
| Validation | Spring Validation |
| Build Tool | Maven |

---

## Design Decisions

### 1. Base62 Encoding (DSA core)
- Characters: `a-z A-Z 0-9` = 62 characters
- Auto-increment MySQL ID is Base62 encoded
- 6-character code = 62⁶ = **56 billion unique URLs**
- Shorter and cleaner than UUID, collision-free with auto-increment

```java
// ID 1 → "b", ID 100 → "bM", ID 3844 → "baa"
String encode(long id) {
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder();
    while (id > 0) {
        sb.append(chars.charAt((int)(id % 62)));
        id /= 62;
    }
    return sb.reverse().toString();
}
```

### 2. Redis Cache-aside Pattern
- On redirect: check Redis first → if miss, fetch MySQL → store in Redis
- TTL set to 24 hours — hot URLs stay cached, cold URLs auto-expire
- Prevents DB overload on high-traffic short URLs
- Achieves sub-10ms redirect response time for cached URLs

### 3. Kafka Async Click Tracking
- Click count update is decoupled from the redirect path
- Redirect publishes event to `url-clicks` topic — non-blocking
- Kafka consumer updates MySQL asynchronously
- Redirect latency is not affected by DB write operations

### 4. Redis Token Bucket Rate Limiting
- Uses Redis `INCR` + `EXPIRE` commands
- 5 requests per minute per IP on `POST /api/shorten`
- First request sets TTL of 60 seconds (window resets automatically)
- Returns `429 Too Many Requests` with `retryAfter: 60 seconds`

---

## API Endpoints

### POST `/api/shorten`
Shorten a long URL.

**Request:**
```json
{
    "url": "https://www.github.com/gagandeepsingh"
}
```

**Response `201 Created`:**
```json
{
    "shortUrl": "http://localhost:8080/b",
    "originalUrl": "https://www.github.com/gagandeepsingh"
}
```

**Rate limit exceeded `429`:**
```json
{
    "error": "Rate limit exceeded. Max 5 requests per minute allowed.",
    "retryAfter": "60 seconds"
}
```

---

### GET `/{shortCode}`
Redirect to original URL.

**Response:** `302 Found` → redirects to original URL

**Not found `404`:**
```json
{
    "error": "Short URL not found: xyz"
}
```

---

### GET `/api/stats/{shortCode}`
Get click analytics for a short URL.

**Response `200 OK`:**
```json
{
    "id": 1,
    "shortCode": "b",
    "originalUrl": "https://www.github.com/gagandeepsingh",
    "clickCount": 42,
    "createdAt": "2026-03-20T10:00:00"
}
```

---

## How to Run Locally

### Prerequisites
- Java 21
- Maven
- Docker Desktop

### Step 1 — Clone the repo
```bash
git clone https://github.com/yourusername/shortifyapi.git
cd shortifyapi
```

### Step 2 — Start infrastructure (MySQL + Redis + Kafka)
```bash
docker-compose up -d
```

Wait 20 seconds for all services to initialize.

### Step 3 — Update application.properties
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/shortifydb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.kafka.bootstrap-servers=localhost:29092
app.base-url=http://localhost:8080
```

### Step 4 — Run the app
```bash
mvn spring-boot:run
```

### Step 5 — Test with Postman
```bash
# Shorten a URL
POST http://localhost:8080/api/shorten
Body: { "url": "https://www.github.com" }

# Redirect
GET http://localhost:8080/b

# Stats
GET http://localhost:8080/api/stats/b
```

### Run with Docker
```bash
docker build -t shortifyapi .
docker run -p 8080:8080 shortifyapi
```

---

## Project Structure

```
src/main/java/com/shortifyapi/
├── controller/
│   └── UrlController.java          # REST endpoints
├── service/
│   ├── UrlService.java             # Core business logic
│   └── RateLimiterService.java     # Token Bucket rate limiting
├── repository/
│   └── UrlRepository.java          # JPA repository
├── entity/
│   └── Url.java                    # URL database entity
├── dto/
│   ├── ShortenRequest.java         # Request DTO with validation
│   └── ShortenResponse.java        # Response DTO
├── kafka/
│   ├── ClickEventProducer.java     # Kafka producer
│   └── ClickEventConsumer.java     # Kafka consumer
├── config/
│   └── KafkaConfig.java            # Kafka beans configuration
├── exception/
│   ├── GlobalExceptionHandler.java # Centralized error handling
│   └── RateLimitExceededException.java
└── util/
    └── Base62Encoder.java          # DSA — Base62 encoding
```
