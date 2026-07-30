# Global Safety Pass - Group 2 (Js & Ms)

Global Safety Pass is a mobile-banking travel support demo built with Java 21 and Spring Boot. It helps customers prepare their cards for a trip, monitor travel spending, recover declined payments, and respond to suspicious overseas transactions.


## Group Members
- Milly Li
- Jessie Han
- Joanne Wang
- Molly Yang

## Core features

The product supports three stages of the travel payment journey:

### 1. Prepare

- Create, edit, and cancel trips with country, city, currency, budget, and preferred card selection
- Run a travel-readiness check across card expiry, freeze status, overseas and online payment settings, daily limits, available balance, account status, destination currency support, and backup-card coverage
- Apply recommended fixes in the app, including enabling overseas payments, unfreezing cards, raising limits, and switching to a better travel card
- View live journey exchange rates and search nearby ATMs with map-based geocoding for backup cash planning

### 2. Spend & protect

- Monitor trip spending, category breakdowns, recent payments, and remaining budget on a travel dashboard
- Receive idempotent transaction events and link them to the active trip
- Detect suspicious overseas activity with internal fraud rules and an external fraud-score integration
- Review fraud alerts, confirm legitimate payments, report fraud, and freeze the affected card when needed
- Manage cards from a wallet view: overseas and online payment controls, freeze and unfreeze, and limit changes protected by step-up verification
- Explore travel history with spending insights and a world-map recap of past journeys

### 3. Recover

- Explain why a payment was declined or interrupted, with recovery checks and a recommended next step
- Retry safely on the original payment record or complete the purchase with an eligible alternate card
- Prevent duplicate charges by preserving the original authorization record
- Open and track support cases for unresolved payment issues and fraud investigations
- Use the payment test lab to simulate approved, interrupted, limit, and suspicious-payment scenarios for demos

Across all three stages, SMS-based sign-in isolates customer data and step-up verification protects sensitive card actions. The project also includes a responsive web interface and Swagger API documentation.

## Tech stack

- Java 21 and Spring Boot 3.5
- Spring MVC, Security, Data JPA, Validation, and WebClient
- MySQL 8.4; H2 for tests and the optional development profile
- Docker (multi-stage image build) and Docker Compose for local deployment
- Maven and Springdoc OpenAPI
- Plain HTML, CSS, and JavaScript frontend

The application source is in `traveller/src/main`. Business logic is separated into controllers, services, repositories, rules, integrations, and security components.


## Run with Docker

```bash
cd traveller
docker compose up --build
```

This starts MySQL and the application on port `8080`.

## Test Users
The following users are available for testing:
- Jessie Han
- Milly Li

*Note: Attempting to log in with "Joanne Wang" or "Molly Yang" will return a "user not found" error.*
