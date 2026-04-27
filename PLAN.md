# Implementation Plan – arete-web-api

## Technology Stack
- Java 21
- Spring Boot 3.3.4 (Web, Data JPA, Validation, Logback, Async)
- H2 Embedded Database (file-based, persistent)
- OpenAI Java SDK 4.32.0 (for Azure AI Foundry)
- Maven (fat-jar via spring-boot-maven-plugin)
- JaCoCo (≥60% line coverage enforcement)
- Docker (Eclipse Temurin 21 Alpine)

## API Endpoints
Base path: `/api/msf` (server.servlet.context-path)

| Method | Path                     | Purpose                                          |
|--------|--------------------------|--------------------------------------------------|
| POST   | /api/msf/room-cost       | Return average daily private room cost           |
| POST   | /api/msf/save-leave-email| Save form state for later resumption             |
| POST   | /api/msf/save-form       | Persist form, start async AI offer calculation   |
| GET    | /api/msf/offer/{uuid}    | Poll for offer result (waits 10s if not ready)   |

## Async Offer Flow
1. `POST /save-form` saves form data to DB (premium fields null), triggers `OfferWorkerService.computeOffer()` on a Spring async thread, returns the UUID immediately.
2. Worker thread calls Azure AI Foundry model, saves `monthly_premium`, `annual_premium`, `currency`, `coverage_details` back to the same DB row.
3. `GET /offer/{uuid}` reads the row; if premium is null it waits 10 s (`offer.wait.ms`), reads again, and returns 500 if still null — allowing the React UI to retry up to 5 times.

## Database Schema

### `room_cost_config`
| Column                  | Type          | Notes                     |
|-------------------------|---------------|---------------------------|
| id                      | BIGINT PK     | Auto-increment            |
| average_daily_room_cost | DECIMAL(10,2) | Room cost in EUR          |
| valid_upto_date         | DATE          | Expiry date for the cost  |
| currency                | VARCHAR(3)    | e.g. EUR                  |

### `form_record`
| Column                    | Type          | Notes                                        |
|---------------------------|---------------|----------------------------------------------|
| id                        | BIGINT PK     | Auto-increment                               |
| form_number               | VARCHAR(50)   | Unique UUID per submission                   |
| email_address             | VARCHAR(255)  | From save-leave-email or save-form           |
| profile                   | VARCHAR(50)   |                                              |
| cover_partner             | BOOLEAN       |                                              |
| cover_children            | BOOLEAN       |                                              |
| number_of_children        | INT           |                                              |
| age                       | VARCHAR(10)   |                                              |
| postcode                  | VARCHAR(10)   |                                              |
| optical_needs             | VARCHAR(50)   |                                              |
| dental_needs              | VARCHAR(50)   |                                              |
| alternative_medicine      | VARCHAR(50)   |                                              |
| hospitalisation_preference| VARCHAR(50)   |                                              |
| doctor_choice             | VARCHAR(50)   |                                              |
| phone_number              | VARCHAR(20)   |                                              |
| monthly_premium           | DECIMAL(10,2) | Null until worker completes                  |
| annual_premium            | DECIMAL(10,2) | Null until worker completes                  |
| currency                  | VARCHAR(3)    | Set by worker (EUR)                          |
| coverage_details          | CLOB          | JSON array of coverage strings, set by worker|
| created_at                | TIMESTAMP     | Auto-set on insert                           |

## Database Initialization
- Normal startup: `CREATE TABLE IF NOT EXISTS` (idempotent)
- Seed data: `MERGE INTO room_cost_config` (upsert, safe on restart)
- Force reinit: set env var `REINITIALIZE_DB=true` → drops tables first, then recreates

## Environment Variables

| Variable                  | Default                        | Purpose                          |
|---------------------------|--------------------------------|----------------------------------|
| H2_DATABASE_PATH          | /data/aretedb                  | H2 file path                     |
| REINITIALIZE_DB           | false                          | Force DB re-init on startup      |
| MODEL_API_URL             | (required for AI)              | Azure AI Foundry endpoint URL    |
| MODEL_API_KEY             | (required for AI)              | Azure AI Foundry API key         |
| MODEL_NAME                | Phi-4-reasoning-1              | Model name to use                |
| MODEL_SYSTEM_PROMPT       | (built-in default)             | Configurable system prompt       |
| MODEL_USER_PROMPT_TEMPLATE| (built-in default)             | Templated user prompt            |

## Package Structure
```
com.arete.webapi
├── AreteWebApiApplication.java       (@SpringBootApplication, @EnableAsync)
├── config/
│   ├── CorsConfig.java
│   └── DatabaseConfig.java
├── controller/
│   └── FormController.java
├── dto/
│   ├── FormData.java
│   ├── OfferResponse.java
│   ├── RoomCostResponse.java
│   ├── SaveFormResponse.java
│   ├── SaveLeaveEmailRequest.java
│   └── ai/
│       └── PremiumResult.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── OfferNotFoundException.java   (→ 404)
│   ├── OfferNotReadyException.java   (→ 500, client retries)
│   └── RoomCostExpiredException.java (→ 500)
├── model/
│   ├── FormRecord.java
│   └── RoomCostConfig.java
├── repository/
│   ├── FormRecordRepository.java
│   └── RoomCostConfigRepository.java
└── service/
    ├── AiModelService.java           (OpenAI SDK wrapper for Azure Foundry)
    ├── FormService.java              (saveLeaveEmail, saveForm, getOffer)
    ├── OfferWorkerService.java       (@Async worker: AI call + DB update)
    └── RoomCostService.java
```

## Business Rules
- **room-cost**: reads single active config row; if `valid_upto_date` < today → 500
- **save-leave-email**: generates UUID form_number, saves form data + email; returns 204
- **save-form**: generates UUID, saves form (premiums null), triggers async worker; returns `{uuid}`
- **offer worker**: calls Azure Foundry `Phi-4-reasoning-1`, builds coverage detail strings, saves premium + currency + coverage back to DB row
- **offer/{uuid}**: reads row; if premium null waits 10 s; if still null returns 500 for client retry; if ready returns full offer

## Testing Strategy
- `FormControllerTest` – MockMvc tests for all 4 endpoints (happy path + error cases)
- `RoomCostServiceTest` – Unit tests (valid date, expired date, no config)
- `FormServiceTest` – Unit tests (saveLeaveEmail, saveForm, getOffer variants)
- `OfferWorkerServiceTest` – Unit tests (computeOffer, buildCoverageDetails all branches)
- `AiModelServiceTest` – Unit tests (fetchPremium, parseModelResponse, buildUserPrompt)
- Target: ≥60% line coverage (JaCoCo check on `mvn verify`)

## Docker
- Multi-stage build: Maven 3.9/Temurin-21-Alpine → Eclipse Temurin 21 JRE Alpine
- Exposes port 3000
- Volume `/data` for H2 database files
- ENV defaults: `H2_DATABASE_PATH=/data/aretedb`, `REINITIALIZE_DB=false`, `MODEL_NAME=Phi-4-reasoning-1`
