# Global Safety Pass - Group 2

Global Safety Pass is a mobile-banking travel support demo built with Java 21 and Spring Boot. It helps customers prepare their cards for a trip, monitor travel spending, recover declined payments, and respond to suspicious overseas transactions.


## Group Members
- Milly Li
- Jessie Han
- Joanne Wang
- Molly Yang

## Core features

The product supports three stages of the travel payment journey:

### 1. Prepare

- Create and manage a trip, budget, and preferred travel card
- Check card readiness, including status, expiry, balance, limits, and payment settings
- Receive recommended actions and choose a more suitable card when needed
- Review indicative exchange rates and find nearby ATMs for backup cash planning

### 2. Spend & protect

- Track travel spending, categories, recent payments, and remaining budget
- Process transaction events once and associate them with the relevant trip
- Detect unusual overseas activity and explain why a payment was flagged
- Confirm a legitimate payment, report fraud, or securely freeze the affected card

### 3. Recover

- Explain why a payment was declined or interrupted
- Recommend a safe retry or an eligible alternate card
- Preserve the original payment record to prevent duplicate charges
- Open and track a support case when the issue cannot be resolved immediately

Across all three stages, authentication isolates customer data and step-up verification protects sensitive card actions. The project also includes a responsive web interface and Swagger API documentation.

## Tech stack

- Java 21 and Spring Boot 3.5
- Spring MVC, Security, Data JPA, Validation, and WebClient
- MySQL 8.4; H2 for tests and the optional development profile
- Maven, Docker Compose, and Springdoc OpenAPI
- Plain HTML, CSS, and JavaScript frontend

The application source is in `traveller/src/main`. Business logic is separated into controllers, services, repositories, rules, integrations, and security components.


## Run with Docker

```bash
cd traveller
docker compose up --build
```

This starts MySQL and the application on port `8080`.
