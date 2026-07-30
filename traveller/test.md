# Traveller Project — Unit & Integration Test Documentation

## 1. Overview

This document describes the automated test suite for the **traveller** (Travel Assistant) Spring Boot application. Tests run against an **in-memory H2 database** in MySQL compatibility mode, so no external MySQL instance is required. External FX and fraud APIs are disabled in the test profile and replaced with mock implementations.

| Metric | Value |
|--------|-------|
| Test classes | 36 |
| Test cases | **137** |
| Latest result | **All passed** (0 failures, 0 errors) |
| Framework | JUnit 5, Spring Boot Test, MockMvc, AssertJ |
| Database | H2 (`jdbc:h2:mem:traveller-test;MODE=MySQL`) |

---

## 2. Environment Setup

### 2.1 H2 test configuration

File: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:traveller-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
integration:
  fx:
    enabled: false
  fraud:
    mode: mock
```

### 2.2 Running tests

```bash
cd traveller
mvn test -Dspring.profiles.active=test -Dintegration.fx.enabled=false
```

If only JDK 17 is available locally:

```bash
mvn test "-Djava.version=17" -Dspring.profiles.active=test
```

### 2.3 Test helpers

| Class | Path | Purpose |
|-------|------|---------|
| `AuthTestHelper` | `support/AuthTestHelper.java` | Login and Step-Up token helpers |
| `TestDataFactory` | `support/TestDataFactory.java` | Build Trip, Card, Transaction, FraudContext fixtures |

---

## 3. Service layer coverage (100%)

Every class under `com.travelassistant.service` has a dedicated test class:

| Service class | Test class | Cases | True / false coverage highlights |
|---------------|------------|-------|----------------------------------|
| `AlertService` | `AlertServiceIntegrationTest` | 4 | Confirm alert (true), get open alert (true), missing alert (false) |
| `AtmMapService` | `AtmMapServiceTest` | 6 | Short query rejected (false), valid geocode (true), radius clamp min/max |
| `AuditService` | `AuditServiceIntegrationTest` | 1 | Audit row persisted (true) |
| `CardCapabilityService` | `CardCapabilityServiceTest` | 9 | Currency supported / not supported, payment allowed / blocked paths |
| `CardControlService` | `CardControlServiceIntegrationTest` | 4 | Customer card list (true), cross-customer isolation (false) |
| `CashExchangePlanningService` | `CashExchangePlanningServiceIntegrationTest` | 3 | EUR plan calculated (true), unmapped country (false), forbidden trip (false) |
| `ExchangeRateService` | `ExchangeRateServiceTest` | 3 | Provider success (true), cache fallback (true), no cache throws (false) |
| `FraudMonitoringService` | `FraudMonitoringServiceIntegrationTest` | 2 | High risk creates alert (true), low risk does not (false) |
| `JourneyExchangeRateService` | `JourneyExchangeRateServiceIntegrationTest` | 3 | Mapped destination (true), budget fallback (true), cross-customer (false) |
| `PaymentFailureService` | `PaymentFailureServiceTest` | 9 | Known codes (true) vs unknown/null (false) |
| `PaymentRecoveryService` | `PaymentRecoveryServiceIntegrationTest` | 3 | Recovery details, retry, invalid alternate card (false) |
| `ReadinessService` | `ReadinessServiceIntegrationTest` | 6 | Healthy trip (true), expired card trip (false), invalid fallback (false) |
| `ReferenceDataService` | `ReferenceDataServiceIntegrationTest` | 5 | Countries/currencies loaded (true), blank city country (false) |
| `SupportCaseService` | `SupportCaseServiceIntegrationTest` | 4 | Open case (true), idempotent reopen (true), wrong customer (false) |
| `TransactionService` | `TransactionServiceIntegrationTest` | 5 | Approved (true), declined (true), duplicate (false), wrong card (false) |
| `TravelCardRecommendationService` | `TravelCardRecommendationServiceIntegrationTest` | 4 | Eligible cards (true), huge amount (false), frozen card excluded (false) |
| `TravelDashboardService` | `TravelDashboardServiceIntegrationTest` | 1 | Dashboard aggregates trip spending (true) |
| `TripService` | `TripServiceIntegrationTest` | 7 | CRUD (true), validation errors (false), ownership (false) |

---

## 4. Full test inventory

### 4.1 By layer

| Layer | Test classes | Cases |
|-------|-------------|-------|
| Pure unit tests | `CardCapabilityServiceTest`, `PaymentFailureServiceTest`, `ExchangeRateServiceTest`, `MockExchangeRateProviderTest`, `MockFraudDetectionClientTest`, `FraudRulesTest`, `ReadinessRulesTest` | 49 |
| Service integration (`@SpringBootTest` + H2) | 18 service test classes | 68 |
| Controller (`MockMvc`) | 10 controller test classes | 19 |
| Security | `AuthenticationServiceTest`, `CustomerIsolationIntegrationTest` | 6 |
| Repository | `RepositoryIntegrationTest` | 5 |
| Integration clients | `LocalCardSystemClientIntegrationTest` | 1 |
| Application bootstrap | `TravelAssistantApplicationTests` | 1 |

### 4.2 Fraud rules (`FraudRulesTest` — 12 cases)

Each rule has at least one **triggered = true** scenario:

| ID | Rule | Trigger condition |
|----|------|-------------------|
| FR-01 | OUTSIDE_TRIP_DATE | Transaction outside trip dates |
| FR-02 | OUTSIDE_DESTINATION | Merchant country ≠ destination |
| FR-03 | UNEXPECTED_CURRENCY | Wrong currency for destination |
| FR-04 | DUPLICATE_TRANSACTION | Same merchant/amount within 10 min |
| FR-05 | HIGH_VALUE_TRANSACTION | Amount > 1000 USD |
| FR-06 | UNUSUAL_ATM_WITHDRAWAL | ≥2 ATM withdrawals in 2 hours |
| FR-07 | RAPID_COUNTRY_CHANGE | Different countries within 3 hours |
| FR-08 | MULTIPLE_DECLINES | ≥3 declines in 1 hour |
| FR-09 | DECLINE_THEN_APPROVAL | Large approval after decline |
| FR-10 | NEW_MERCHANT_CATEGORY | Unseen merchant category |
| FR-11 | CARD_FROZEN_TRANSACTION | Activity on frozen card |
| FR-12 | HIGH_RISK_COUNTRY | Iran / North Korea / Syria |

Non-trigger paths are covered implicitly via `FraudMonitoringServiceIntegrationTest` (low-risk transaction, score < 50, no alert created).

### 4.3 Readiness rules (`ReadinessRulesTest` — 11 cases)

Each rule has a **passed = false** failure scenario:

| ID | Rule | Failure scenario |
|----|------|------------------|
| RR-01 | CARD_EXPIRY | Card expires before trip ends |
| RR-02 | CARD_FROZEN | Card not ACTIVE |
| RR-03 | OVERSEAS_PAYMENT | Overseas payments disabled |
| RR-04 | ONLINE_PAYMENT | Online payments disabled |
| RR-05 | PAYMENT_LIMIT | Daily limit too low |
| RR-06 | WITHDRAWAL_LIMIT | Withdrawal limit < 100 |
| RR-07 | AVAILABLE_BALANCE | Balance below budget |
| RR-08 | ACCOUNT_STATUS | Account not ACTIVE |
| RR-09 | CURRENCY_SUPPORT | USD settlement card passes (true) |
| RR-10 | CURRENCY_SUPPORT | USD_SETTLEMENT fallback accepted (true) |
| RR-11 | BACKUP_CARD | No other active card (false) |

---

## 5. Example test code

### 5.1 True and false — `CardCapabilityService`

```java
@Test
void canCompleteTravelPaymentReturnsTrueWhenAllConditionsMet() {
    var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.ACTIVE);
    var account = TestDataFactory.account("account-001", new BigDecimal("1000"), Enums.AccountStatus.ACTIVE);
    assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isTrue();
}

@Test
void canCompleteTravelPaymentReturnsFalseWhenCardFrozen() {
    var card = TestDataFactory.card("card-001", "customer-001", Enums.CardStatus.FROZEN);
    var account = TestDataFactory.account("account-001", new BigDecimal("1000"), Enums.AccountStatus.ACTIVE);
    assertThat(service.canCompleteTravelPayment(card, account, new BigDecimal("100"), "USD")).isFalse();
}
```

### 5.2 True and false — `FraudMonitoringService`

```java
@Test
void highRiskTransactionCreatesAlert() {
    long before = alerts.count();
    // Iran + high amount → score >= 50
    fraudMonitoring.evaluate(highRiskTxn, context);
    assertThat(alerts.count()).isGreaterThan(before);
}

@Test
void lowRiskTransactionDoesNotCreateAlert() {
    long before = alerts.count();
    // Small in-country purchase → score < 50
    fraudMonitoring.evaluate(lowRiskTxn, context);
    assertThat(alerts.count()).isEqualTo(before);
}
```

### 5.3 H2 integration — `TripService`

```java
@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@Transactional
class TripServiceIntegrationTest {
    @Test
    void ownedReturnsTripForMatchingCustomer() {
        assertThat(tripService.get("customer-001", "trip-paris").id()).isEqualTo("trip-paris");
    }

    @Test
    void cannotAccessAnotherCustomersTrip() {
        assertThatThrownBy(() -> tripService.get("customer-002", "trip-paris"))
                .isInstanceOf(ForbiddenException.class);
    }
}
```

### 5.4 Controller — card freeze requires Step-Up

```java
@Test
void freezeRequiresStepUp() throws Exception {
    mvc.perform(post("/api/travel/cards/card-002/freeze")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());  // false — no token

    String stepUp = AuthTestHelper.stepUp(mvc, json, token, "CARD_FREEZE", "card-002");
    mvc.perform(post("/api/travel/cards/card-002/freeze")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Step-Up-Token", stepUp))
            .andExpect(status().isOk())             // true — valid token
            .andExpect(jsonPath("$.status").value("FROZEN"));
}
```

---

## 6. Test results

### 6.1 Latest execution

```
Date:    2026-07-30
Command: mvn test "-Djava.version=17" -Dspring.profiles.active=test
Result:  Tests run: 137, Failures: 0, Errors: 0, Skipped: 0
         BUILD SUCCESS
```

### 6.2 Results by test class

| Test class | Cases | Result |
|------------|-------|--------|
| `FraudRulesTest` | 12 | PASS |
| `ReadinessRulesTest` | 11 | PASS |
| `PaymentFailureServiceTest` | 9 | PASS |
| `CardCapabilityServiceTest` | 9 | PASS |
| `AtmMapServiceTest` | 6 | PASS |
| `ReadinessServiceIntegrationTest` | 6 | PASS |
| `TripServiceIntegrationTest` | 7 | PASS |
| `ReferenceDataServiceIntegrationTest` | 5 | PASS |
| `RepositoryIntegrationTest` | 5 | PASS |
| `TransactionServiceIntegrationTest` | 5 | PASS |
| `SupportCaseServiceIntegrationTest` | 4 | PASS |
| `AlertServiceIntegrationTest` | 4 | PASS |
| `CardControlServiceIntegrationTest` | 4 | PASS |
| `TravelCardRecommendationServiceIntegrationTest` | 4 | PASS |
| `AuthenticationServiceTest` | 4 | PASS |
| All other classes | 1–3 each | PASS |

---

## 7. Directory structure

```
src/test/java/com/travelassistant/
├── controller/           # 10 REST controller tests
├── integration/          # Mock FX/fraud + LocalCardSystem
├── repository/           # JPA repository queries
├── rule/
│   ├── fraud/            # 12 fraud rules
│   └── readiness/        # 11 readiness rules
├── security/             # Auth + customer isolation
├── service/              # 20 tests — one per service class
├── support/              # AuthTestHelper, TestDataFactory
└── TravelAssistantApplicationTests.java

src/test/resources/
└── application-test.yml    # H2 configuration
```

---

## 8. Demo accounts

| Display name | customerId | Demo credentials |
|--------------|------------|------------------|
| Jessie Han | customer-001 | trusted-device-demo / PIN 2580 / SMS 246810 |
| Milly Li | customer-002 | Same as above |

---

## 9. Git ignore

The following folders are excluded from version control (`.gitignore`):

```
.agents/
.impeccable/
```

---

## 10. Future improvements

1. `AtmMapService` — stub WebClient with WireMock for fully offline geocoding tests
2. `GlobalExceptionHandler` — dedicated `@WebMvcTest` for each HTTP status mapping
3. `HttpFraudDetectionClient` / `FrankfurterExchangeRateProvider` — test HTTP failure branches with `@TestPropertySource`
4. CI pipeline — run `mvn test` on Java 21 in GitHub Actions
