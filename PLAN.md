# Implementation Plan – arete-web-api

## Technology Stack
- Java 21
- Spring Boot 3.3.4 (Web, Data JPA, Validation, Logback)
- H2 Embedded Database (file-based, persistent)
- Maven (fat-jar via spring-boot-maven-plugin)
- JaCoCo (≥60% line coverage enforcement)
- Docker (Eclipse Temurin 21 Alpine)

## API Endpoints
Base path: `/api` (matches frontend fetch calls)

| Method | Path                  | Purpose                               |
|--------|-----------------------|---------------------------------------|
| POST   | /api/room-cost        | Return average daily private room cost |
| POST   | /api/save-leave-email | Save form state for later resumption  |
| POST   | /api/offer            | Calculate and return insurance offer  |

## Database Schema

### `room_cost_config`
| Column                  | Type          | Notes                     |
|-------------------------|---------------|---------------------------|
| id                      | BIGINT PK     | Auto-increment            |
| average_daily_room_cost | DECIMAL(10,2) | Room cost in EUR          |
| valid_upto_date         | DATE          | Expiry date for the cost  |
| currency                | VARCHAR(3)    | e.g. EUR                  |

### `form_record`
| Column                    | Type          | Notes                             |
|---------------------------|---------------|-----------------------------------|
| id                        | BIGINT PK     | Auto-increment                    |
| form_number               | VARCHAR(50)   | Unique UUID per submission        |
| email_address             | VARCHAR(255)  | From save-leave-email or offer    |
| profile                   | VARCHAR(50)   |                                   |
| cover_partner             | BOOLEAN       |                                   |
| cover_children            | BOOLEAN       |                                   |
| number_of_children        | INT           |                                   |
| age                       | VARCHAR(10)   |                                   |
| postcode                  | VARCHAR(10)   |                                   |
| optical_needs             | VARCHAR(50)   |                                   |
| dental_needs              | VARCHAR(50)   |                                   |
| alternative_medicine      | VARCHAR(50)   |                                   |
| hospitalisation_preference| VARCHAR(50)   |                                   |
| doctor_choice             | VARCHAR(50)   |                                   |
| phone_number              | VARCHAR(20)   |                                   |
| monthly_premium           | DECIMAL(10,2) | Null for save-leave-email records |
| annual_premium            | DECIMAL(10,2) | Null for save-leave-email records |
| created_at                | TIMESTAMP     | Auto-set on insert                |

## Database Initialization
- Normal startup: `CREATE TABLE IF NOT EXISTS` (idempotent)
- Seed data: `MERGE INTO room_cost_config` (upsert, safe on restart)
- Force reinit: set env var `REINITIALIZE_DB=true` → drops tables first, then recreates

## Package Structure
```
com.arete.webapi
├── AreteWebApiApplication.java
├── config/
│   ├── CorsConfig.java
│   └── DatabaseConfig.java
├── controller/
│   └── FormController.java
├── dto/
│   ├── FormData.java
│   ├── OfferResponse.java
│   ├── RoomCostResponse.java
│   └── SaveLeaveEmailRequest.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── RoomCostExpiredException.java
├── model/
│   ├── FormRecord.java
│   └── RoomCostConfig.java
├── repository/
│   ├── FormRecordRepository.java
│   └── RoomCostConfigRepository.java
└── service/
    ├── FormService.java
    └── RoomCostService.java
```

## Business Rules
- **room-cost**: reads the single active config row; if `valid_upto_date` < today → 500 error
- **save-leave-email**: generates a UUID form_number, saves form data + email; returns 204
- **offer**: fixed 100 EUR/month, 1000 EUR/year; builds coverage detail strings from form selections; saves all data to DB; returns offer

## Testing Strategy
- `FormControllerTest` – MockMvc integration tests for all 3 endpoints (happy path + error cases)
- `RoomCostServiceTest` – Unit tests with Mockito (valid date, expired date, no config)
- `FormServiceTest` – Unit tests with Mockito (save email, build offer, coverage details)
- Target: ≥60% line coverage (JaCoCo check on `mvn verify`)

## Docker
- Multi-stage build: Maven 3.9/Temurin-21-Alpine → Eclipse Temurin 21 JRE Alpine
- Exposes port 3000
- Volume `/data` for H2 database files
- ENV defaults: `H2_DATABASE_PATH=/data/aretedb`, `REINITIALIZE_DB=false`
