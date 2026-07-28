# Traveller Project Test Documentation

> Branch: `Version1.0test-H2`  
> Generated: 2026-07-28  
> Test command: `mvn test "-Dspring.profiles.active=test"`

Local startup:
```powershell
cd traveller
mvn spring-boot:run
# H2 Console: http://localhost:8080/h2-console
```
Note: adjust the JDK version in `pom.xml` to match your environment if needed.

---

## 1. Overview

This change switches **MySQL to an H2 in-memory database** for easier local development and unit/integration testing without running a MySQL instance. It also adds **107 unit/integration tests** covering core business logic, fraud rules, travel readiness rules, authentication, payment recovery, and related modules.

---

## 2. H2 Database Configuration

### 2.1 Default Profile Change

The default profile in `application.yml` was changed from `mysql` to `dev` (H2 in-memory):

```yaml
spring:
  profiles:
    default: dev
```

### 2.2 Development Environment (dev) — H2

File: `src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:traveller;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: true
      path: /h2-console
integration:
  fx:
    enabled: false
  fraud:
    mode: mock
```

**Key parameters:**

| Parameter | Purpose |
|-----------|---------|
| `MODE=MySQL` | Run H2 in MySQL compatibility mode |
| `DB_CLOSE_DELAY=-1` | Keep the in-memory database alive while the JVM is running |
| `DATABASE_TO_LOWER=TRUE` | Lowercase table/column names, aligned with JPA defaults |
| `CASE_INSENSITIVE_IDENTIFIERS=TRUE` | Case-insensitive identifiers |

### 2.3 Test Environment (test) — H2

File: `src/main/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:traveller-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
integration:
  fx:
    enabled: false
  fraud:
    mode: mock
```

Tests use a separate in-memory database `traveller-test`; `create-drop` cleans up automatically after each test run.

### 2.4 MySQL Compatibility Migration

`DatabaseCompatibilityMigration.java` runs `ALTER TABLE` only when MySQL is detected; it is skipped automatically on H2:

```java
if (product.toLowerCase().contains("mysql")) {
    jdbc.execute("ALTER TABLE travel_transactions MODIFY COLUMN recovery_status VARCHAR(40) NULL");
}
// H2 uses VARCHAR columns by default via JPA; no migration needed
```

### 2.5 Production/MySQL Still Available

- `application-mysql.yml` — local MySQL
- `application-prod.yml` — Docker/production MySQL

Switch with: `--spring.profiles.active=mysql`

### 2.6 Java Version

`java.version` in `pom.xml` is set to **17** to match the current development JDK, so `mvn test` can run directly.

---

## 3. Test Framework and Approach

| Item | Choice |
|------|--------|
| Test framework | JUnit 5 (Jupiter) |
| Mock framework | Mockito |
| Assertion library | AssertJ |
| Spring testing | `@SpringBootTest`, `@DataJpaTest` |
| Parameterized tests | `@ParameterizedTest` + `@CsvSource` |
| Database | H2 in-memory (`test` profile) |
| Build tool | Maven Surefire 3.5.3 |

### 3.1 Test Layers

```
┌─────────────────────────────────────────────────────┐
│  Integration tests (@SpringBootTest / @DataJpaTest + H2) │
│  TravelAssistantApplicationTests                     │
│  ReadinessServiceIntegrationTest                     │
│  RepositoryIntegrationTest                           │
├─────────────────────────────────────────────────────┤
│  Service-layer unit tests (Mockito-isolated deps)    │
│  TripService, AlertService, PaymentRecoveryService…  │
├─────────────────────────────────────────────────────┤
│  Pure logic unit tests (no Spring container)         │
│  FraudRules, ReadinessRules, ExchangeRateService…    │
│  AuthenticationService, PaymentFailureService        │
├─────────────────────────────────────────────────────┤
│  Mock integration component tests                    │
│  MockExchangeRateProvider, MockFraudDetectionClient  │
└─────────────────────────────────────────────────────┘
```

### 3.2 Test Data Factory

`TestFixtures.java` provides reusable builders for Trip, Card, Account, Transaction, and FraudAlert to avoid duplicating test data across classes.

---

## 4. Test Class Inventory (18 classes, 107 tests)

| Test class | Tests | Type | Coverage |
|------------|-------|------|----------|
| `TravelAssistantApplicationTests` | 1 | Integration | Spring context loading |
| `ReadinessServiceIntegrationTest` | 1 | Integration | Full travel readiness assessment on H2 |
| `RepositoryIntegrationTest` | 4 | Integration | Trip/Card/Account/Transaction/Alert persistence |
| `FraudRulesTest` | 13 | Unit | 13 fraud rules (destination, date, currency, duplicate, high value, etc.) |
| `ReadinessRulesTest` | 18 | Unit | 10 readiness rules (card, balance, limits, backup card, etc.) |
| `AuthenticationServiceTest` | 10 | Unit | Login, OTP, session, step-up verification |
| `ExchangeRateServiceTest` | 5 | Unit | Rate lookup, caching, fallback degradation |
| `PaymentFailureServiceTest` | 13 | Unit | 12+ payment failure codes with explanations and recommended actions |
| `CardCapabilityServiceTest` | 11 | Unit | Currency support and payment eligibility |
| `FraudMonitoringServiceTest` | 3 | Unit | Risk scoring, alert creation, external service degradation |
| `TripServiceTest` | 5 | Unit | Trip CRUD, validation, cash exchange planning |
| `PaymentRecoveryServiceTest` | 4 | Unit | Network retry, limit failures, alternate card recovery |
| `TravelDashboardServiceTest` | 1 | Unit | Budget spent/remaining calculation |
| `TravelCardRecommendationServiceTest` | 2 | Unit | Backup card recommendations |
| `CardControlServiceTest` | 4 | Unit | Freeze/unfreeze, overseas payments, limit changes |
| `AlertServiceTest` | 2 | Unit | Fraud alert confirmation and listing |
| `SupportCaseServiceTest` | 2 | Unit | Payment support / fraud investigation cases |
| `JourneyExchangeRateServiceTest` | 2 | Unit | Trip exchange rates, unknown destination handling |
| `MockIntegrationTest` | 6 | Unit | Mock exchange rate and fraud scoring clients |

---

## 5. Key Test Code Examples

### 5.1 Fraud Rule — Destination Mismatch

```java
@Test @DisplayName("outside destination triggers")
void outsideDestinationTriggers() {
    var rule = new OutsideDestinationRule(35);
    var t = txn(Instant.parse("2026-08-05T10:00:00Z"), "France", "EUR", new BigDecimal("100"));
    assertThat(rule.evaluate(t, ctx).triggered()).isTrue();
}
```

### 5.2 Payment Failure Codes — Parameterized Test

```java
@ParameterizedTest(name = "{0} -> {1}")
@CsvSource({
    "INSUFFICIENT_FUNDS, Not enough funds, ADD_FUNDS",
    "CARD_FROZEN, Card is frozen, UNFREEZE_CARD",
    "NETWORK_ERROR, Connection problem, RETRY"
})
void knownFailureCodes(String code, String title, ActionType action) {
    var result = service.explain("txn-x", code);
    assertThat(result.title()).isEqualTo(title);
    assertThat(result.actionType()).isEqualTo(action);
}
```

### 5.3 Authentication Service — Trusted Device Login

```java
@Test @DisplayName("trusted device verification")
void trustedDeviceVerification() {
    String id = auth.start("customer-001").challengeId();
    VerifyResponse r = auth.verify(new VerifyRequest(id, Method.TRUSTED_DEVICE, "trusted-device-demo"));
    assertThat(r.accessToken()).isNotBlank();
    assertThat(r.customerId()).isEqualTo("customer-001");
}
```

### 5.4 Exchange Rate Service — External API Failure Fallback

```java
@Test @DisplayName("external failure uses reference fallback")
void externalFailureUsesSafeEstimatedFallback() {
    ExchangeRateProvider failing = (a, b, d) -> { throw new RuntimeException("offline"); };
    var quote = new ExchangeRateService(failing).rate("JPY", "USD", LocalDate.of(2026, 8, 12));
    assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("0.00670017"));
    assertThat(quote.estimated()).isTrue();
    assertThat(quote.provider()).isEqualTo("reference-fallback");
}
```

### 5.5 H2 Repository Integration Test

```java
@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {
    @Autowired TripRepository trips;

    @Test @DisplayName("persists and finds trip")
    void persistsAndFindsTrip() {
        Trip trip = TestFixtures.trip("trip-h2-1", "customer-001");
        trips.save(trip);
        assertThat(trips.findById("trip-h2-1")).isPresent();
    }
}
```

### 5.6 Payment Recovery — Network Error Retry Success

```java
@Test @DisplayName("network error retry completes")
void networkErrorRetryCompletes() {
    txn.setStatus(Enums.TransactionStatus.DECLINED);
    txn.setFailureCode("NETWORK_ERROR");
    var result = service.retry("customer-001", "txn-1");
    assertThat(result.status()).isEqualTo(Enums.RecoveryStatus.COMPLETED_AFTER_RETRY);
    assertThat(txn.getStatus()).isEqualTo(Enums.TransactionStatus.APPROVED);
}
```

### 5.7 Travel Readiness Integration Test (H2 + DataSeeder)

```java
@SpringBootTest(properties = {"integration.fx.enabled=false"})
@ActiveProfiles("test")
class ReadinessServiceIntegrationTest {
    @Test @DisplayName("readiness check on H2 with seeded data")
    void readinessCheckOnH2() {
        var trip = tripService.create("customer-001", new TripRequest(
            "Japan", "Tokyo", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10),
            new BigDecimal("2000"), "USD", "card-001"));
        var result = readinessService.check("customer-001", trip.id());
        assertThat(result.score()).isBetween(0, 100);
        assertThat(result.checks()).isNotEmpty();
    }
}
```

---

## 6. Test Execution Results

### 6.1 Run Command

```powershell
$env:JAVA_HOME = "D:\dev\jdk\jdk-17.0.13+11"
cd "D:\ALL\HSBCResources\GlobalSafteyPass\traveller"
mvn test "-Dspring.profiles.active=test"
```

### 6.2 Summary

```
[INFO] Results:
[INFO]
[INFO] Tests run: 107, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  19.069 s
[INFO] Finished at: 2026-07-28T14:03:34+08:00
```

### 6.3 Detailed Results by Class

| Test class | Passed | Failed | Errors | Skipped | Duration |
|------------|--------|--------|--------|---------|----------|
| MockIntegrationTest | 6 | 0 | 0 | 0 | 0.161s |
| ReadinessServiceIntegrationTest | 1 | 0 | 0 | 0 | 7.506s |
| RepositoryIntegrationTest | 4 | 0 | 0 | 0 | 0.486s |
| FraudRulesTest | 13 | 0 | 0 | 0 | 0.019s |
| ReadinessRulesTest | 18 | 0 | 0 | 0 | 0.113s |
| AuthenticationServiceTest | 10 | 0 | 0 | 0 | 0.017s |
| AlertServiceTest | 2 | 0 | 0 | 0 | 0.093s |
| CardCapabilityServiceTest | 11 | 0 | 0 | 0 | 0.006s |
| CardControlServiceTest | 4 | 0 | 0 | 0 | 0.011s |
| ExchangeRateServiceTest | 5 | 0 | 0 | 0 | 0.005s |
| FraudMonitoringServiceTest | 3 | 0 | 0 | 0 | 0.012s |
| JourneyExchangeRateServiceTest | 2 | 0 | 0 | 0 | 0.051s |
| PaymentFailureServiceTest | 13 | 0 | 0 | 0 | 0.051s |
| PaymentRecoveryServiceTest | 4 | 0 | 0 | 0 | 0.037s |
| SupportCaseServiceTest | 2 | 0 | 0 | 0 | 0.026s |
| TravelCardRecommendationServiceTest | 2 | 0 | 0 | 0 | 0.004s |
| TravelDashboardServiceTest | 1 | 0 | 0 | 0 | 0.024s |
| TripServiceTest | 5 | 0 | 0 | 0 | 0.011s |
| TravelAssistantApplicationTests | 1 | 0 | 0 | 0 | 0.837s |
| **Total** | **107** | **0** | **0** | **0** | **~19s** |

### 6.4 H2 Connection Confirmation (from test logs)

```
Database JDBC URL [jdbc:h2:mem:traveller-test;MODE=MySQL;DB_CLOSE_DELAY=-1;...]
Database driver: H2 JDBC Driver
Database version: 2.3.232
```

---

## 7. Local Startup (H2 Mode)

```powershell
cd "D:\ALL\HSBCResources\GlobalSafteyPass\traveller"
mvn spring-boot:run
# Default profile: dev (H2)
# H2 Console: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:traveller
# Username: sa  Password: (empty)
```

### Demo Accounts

| Customer ID | Auth method | Credential |
|-------------|-------------|------------|
| customer-001 | Trusted device | `trusted-device-demo` |
| customer-001/002 | App PIN | `2580` |
| customer-001 | SMS OTP | Call sendSms first, then enter `246810` |

---

## 8. File Structure

```
traveller/
├── src/main/resources/
│   ├── application.yml          # Default profile: dev (H2)
│   ├── application-dev.yml      # H2 development config
│   ├── application-test.yml     # H2 test config
│   ├── application-mysql.yml    # MySQL (retained)
│   └── application-prod.yml     # Production MySQL (retained)
├── src/test/java/com/travelassistant/
│   ├── TestFixtures.java
│   ├── TravelAssistantApplicationTests.java
│   ├── ReadinessServiceIntegrationTest.java
│   ├── integration/MockIntegrationTest.java
│   ├── repository/RepositoryIntegrationTest.java
│   ├── rule/fraud/FraudRulesTest.java
│   ├── rule/readiness/ReadinessRulesTest.java
│   ├── security/AuthenticationServiceTest.java
│   └── service/                 # 12 service test classes
└── test.md                      # This document
```

---

## 9. Summary

- **Database**: Default H2 in-memory with `MODE=MySQL` for SQL compatibility; MySQL profiles remain available for production.
- **Test scale**: 18 test classes, **107 test cases**, all passing.
- **Coverage**: Fraud rules, travel readiness, authentication, exchange rates, payment failure/recovery, card controls, alerts, support cases, dashboard, and H2 persistence.
- **Run cost**: No MySQL/Docker required; a single `mvn test` completes all verification in about 19 seconds.
