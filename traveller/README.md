# Group 2 - Js & Ms - Milly, Molly, Joanne, Jessie

# Travel Assistant

A Java 21 and Spring Boot 3 mobile-banking travel support demo. It helps customers prepare cards before departure, monitor travel spending, recover failed payments, and review suspicious overseas transactions.

> This project uses simulated banking data and never moves real money. Live exchange rates are indicative and are used only for travel planning. The final posted amount must always come from the bank transaction system's `billingAmount`.

## Highlights

- Create, view, update, and cancel trips with strict date, budget, and card-ownership validation
- Run 10 explainable travel-readiness checks with a score from 0 to 100
- Recommend a suitable travel card based on status, funds, destination currency, and payment capability
- Enable overseas or online payments, freeze or unfreeze a card, and change payment or ATM limits
- Protect sensitive card actions with step-up authentication
- Process transaction events idempotently and match them to registered trips
- Track trip budgets, spending categories, recent activity, and open security alerts
- Retrieve live indicative rates from Frankfurter with a three-second timeout, in-memory caching, and a safe fallback
- Explain declined payments and provide guided recovery through retry or an eligible alternate card
- Open and track bank support cases for unresolved payment problems
- Evaluate transaction risk with 12 configurable internal rules, an optional external score, and a historical customer profile
- Confirm legitimate transactions, report fraud, and freeze the affected card from an alert
- Authenticate with a trusted device, app PIN, or SMS OTP and isolate every customer's data
- Explore the responsive banking UI, Swagger API documentation, seeded scenarios, and an in-app transaction test lab
- Run with MySQL in development/production or H2 for tests and the optional `dev` profile

## Main customer journeys

### Prepare for a trip

Create a journey, select a preferred card, and run the readiness check. The assistant reviews card expiry, card status, overseas and online payment settings, available balance, daily limits, destination-currency support, and backup payment options. Each failed check includes a recommended action that can be completed from the UI.

If the preferred card does not support the destination currency, the customer can choose a better eligible card or record a plan to exchange cash after arrival. The journey view also shows the card-specific indicative exchange rate and the trip budget.

### Recover a failed payment

A declined transaction receives a plain-English explanation and a recovery timeline. Depending on the failure, the customer can safely retry the original authorization or complete it with an eligible alternate card. Recovery updates the original transaction instead of creating a duplicate payment, and unresolved issues can be converted into a tracked support case.

### Respond to suspected fraud

Each transaction is evaluated by internal rules, an optional external fraud service, and the customer's historical behavior. A generated alert explains the risk signals and lets the customer confirm the payment, report fraud, or securely freeze the card. Audit records are append-only from the public API.

## Architecture

```text
Controllers and security context
        |
        +-- Trip and dashboard services
        |     +-- trip lifecycle and validation
        |     +-- readiness assessment
        |     +-- budget and transaction summary
        |     +-- card recommendation
        |     +-- journey exchange-rate guidance
        |
        +-- Card services
        |     +-- supported currencies and eligibility
        |     +-- overseas and online payment controls
        |     +-- freeze, unfreeze, and limit changes
        |
        +-- Transaction and recovery services
        |     +-- idempotent event ingestion
        |     +-- decline explanations
        |     +-- retry and alternate-card recovery
        |     +-- support-case tracking
        |
        +-- Fraud services
              +-- configurable internal rules
              +-- external scoring adapter
              +-- historical customer profile
              +-- alerts and audit logging
```

Controllers handle the authenticated customer context and HTTP contract. Business decisions live in services, while repositories provide persistence. The `recovery_status` field uses an extensible `VARCHAR` so the recovery workflow can add states without being constrained by a MySQL enum.

Source code is under `src/main/java/com/travelassistant` and is organized into `controller`, `service`, `repository`, `model`, `dto`, `rule`, `integration`, `exception`, `config`, and `security` packages. Monetary values use `BigDecimal`, dates use `LocalDate`, and event timestamps use `Instant`. The application does not store CVVs, full card numbers, or passwords.

## Technology

- Java 21
- Spring Boot 3.5
- Spring MVC, WebFlux client, Security, Validation, and Data JPA
- MySQL 8.4 and H2
- Springdoc OpenAPI / Swagger UI
- Maven and Docker Compose
- Plain HTML, CSS, and JavaScript responsive UI

## Data model and demo scenarios

The main tables are `trips`, `cards`, `accounts`, `card_fx_rates`, `travel_transactions`, `fraud_alerts`, `support_cases`, `readiness_assessments`, and `audit_logs`.

The default seed customer is `customer-001`. Seed data includes:

- Tokyo: payment recovery and card-readiness issues
- Paris: a normal journey with spending and live exchange-rate guidance
- Nice: card-expiry preparation
- Refunds, reversals, duplicate charges, out-of-trip transactions, destination mismatches, high-value purchases, repeated declines, network errors, and overseas ATM activity

The default application profile and Docker setup use MySQL. H2 is reserved for automated tests and runs using the explicitly selected `dev` profile.

## Run locally

Requirements:

- Java 21
- Maven 3.9 or later
- MySQL, either installed locally or started through Docker

Start MySQL:

```bash
docker compose up -d mysql
```

Start the application:

```bash
mvn spring-boot:run
```

Open:

- Banking UI: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>

The default database is `travel_assistant`, with username `travel_user` and password `travel_password`. Override these values with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.

To run temporarily with the in-memory H2 database:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Run the test suite:

```bash
mvn test
```

## Run with Docker

```bash
docker compose up --build
```

Docker Compose starts MySQL 8.4 and the `travel-assistant-app` service. The application is exposed on port `8080`. The first build downloads the required Maven and Docker dependencies.

## Demo authentication

Start an authentication challenge:

```bash
curl -X POST http://localhost:8080/api/auth/start \
  -H "Content-Type: application/json" \
  -d '{"customerId":"customer-001"}'
```

Use the returned `challengeId` to verify the trusted demo device:

```bash
curl -X POST http://localhost:8080/api/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"challengeId":"CHALLENGE_ID","method":"TRUSTED_DEVICE","credential":"trusted-device-demo"}'
```

Demo credentials:

- Trusted device assertion: `trusted-device-demo`
- App PIN: `2580`
- SMS OTP: `246810`

Use the returned short-lived access token on protected endpoints:

```text
Authorization: Bearer ACCESS_TOKEN
```

Production deployments should replace the demo methods with a real identity provider, WebAuthn or passkeys, trusted-device binding, and an SMS provider.

## API examples

Create a trip:

```bash
curl -X POST http://localhost:8080/api/travel/trips \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ACCESS_TOKEN" \
  -d '{"destinationCountry":"Japan","destinationCity":"Osaka","startDate":"2026-10-01","endDate":"2026-10-08","budget":2200,"budgetCurrency":"USD","preferredCardId":"card-001"}'
```

Run its readiness check:

```bash
curl -X POST http://localhost:8080/api/travel/trips/TRIP_ID/readiness-check \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

Get a live indicative rate:

```bash
curl "http://localhost:8080/api/travel/exchange-rates/live?base=USD&quote=JPY" \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

Submit a suspicious transaction:

```bash
curl -X POST http://localhost:8080/api/travel/transactions/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ACCESS_TOKEN" \
  -d '{"transactionId":"txn-demo-risk","customerId":"customer-001","cardId":"card-002","merchantName":"Paris Luxury","merchantCountry":"France","merchantCity":"Paris","merchantCategory":"SHOPPING","originalAmount":1800,"originalCurrency":"USD","transactionTime":"2026-08-12T13:30:00Z","transactionType":"PURCHASE","status":"APPROVED"}'
```

Inspect recovery options for a failed payment:

```bash
curl http://localhost:8080/api/travel/transactions/TRANSACTION_ID/recovery \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

The UI's **Demo suspicious payment** action submits a high-value payment in France while the registered journey is in Japan. The normal transaction pipeline stores it in MySQL and produces an explainable risk decision.

## External service configuration

| Variable | Purpose | Default |
| --- | --- | --- |
| `FX_API_BASE_URL` | Exchange-rate provider base URL | `https://api.frankfurter.dev/v1` |
| `FX_API_ENABLED` | Enable live exchange-rate requests | `true` |
| `FRAUD_API_MODE` | Fraud adapter mode: `mock` or `http` | `mock` |
| `FRAUD_API_BASE_URL` | External fraud service base URL | Empty |
| `FRAUD_API_KEY` | Optional fraud service Bearer token | Empty |
| `DATABASE_URL` | JDBC database URL | Local MySQL URL |
| `DATABASE_USERNAME` | Database username | `travel_user` |
| `DATABASE_PASSWORD` | Database password | `travel_password` |

`ExchangeRateProvider` isolates the FX vendor. When a live request fails, the service first uses the most recent cached quote for the same key, such as `FX:USD:JPY:2026-08-12`. If no quote is available, it returns `1.0` and marks the result as estimated.

`FraudDetectionClient` isolates the external fraud provider. In the default `mock` mode the project works offline. In `http` mode it sends a three-second request to `FRAUD_API_BASE_URL/score` with the optional API key as a Bearer token. If that service is unavailable, the transaction is still stored and scored with local data.

## Scoring

Readiness starts at 100 and subtracts points for failed checks. The result is bounded to 0-100:

- `READY`: 80-100
- `ACTION_REQUIRED`: 50-79
- `NOT_READY`: 0-49

The normal fraud score combines internal rules (50%), the external risk service (30%), and the historical customer profile (20%). The profile is calculated from previous transactions, including average spend, frequently visited countries, and common merchant categories.

When the external service is unavailable, scoring automatically changes to internal rules (70%) and customer profile (30%). Decisions are:

- 0-24: allow
- 25-49: monitor
- 50-74: require confirmation
- 75-100: block and alert

Weights, thresholds, and rule scores are configurable in `src/main/resources/application.yml`.

## Possible next steps

- Replace the in-memory exchange-rate cache with Redis and a defined TTL
- Integrate OAuth 2.0/OIDC and a production customer identity context
- Manage the MySQL schema with Flyway
- Move transaction ingestion to Kafka with an outbox pattern
- Add signed WORM storage for sensitive audit records
- Connect to production card-core, fraud-scoring, and card-network services
