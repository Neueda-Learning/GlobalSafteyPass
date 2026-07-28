# Global Safety Pass - Travel Lion

A full-stack Spring Boot 3 travel assistant covering all three phases from the project requirements:

1. **Phase 1 - Travel Readiness**: Create trips, card checks, readiness score and warnings
2. **Phase 2 - Payment Tracking**: Record payments, budget tracking, failure explanations
3. **Phase 3 - Security Monitoring**: Suspicious transaction detection, alerts and card freeze

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Spring Data JPA
- MySQL 8 (port 3306)
- Thymeleaf + Bootstrap 5

## Requirements

- JDK 17 (`JAVA_HOME`: `D:\dev\jdk\jdk-17.0.13+11`)
- Maven 3.9 (`MAVEN_HOME`: `D:\dev\maven\apache-maven-3.9.16`)
- MySQL running on `localhost:3306`

## Quick Start

### 1. Database

The database is created automatically on first run. Alternatively, run `sql/init.sql` manually.

### 2. Configuration

`src/main/resources/application.properties`:

- URL: `jdbc:mysql://localhost:3306/travel_assistant`
- Username: `root`
- Password: `qianqian825`
- Login: `admin` / `admin123`

### 3. Build & Run

```powershell
$env:JAVA_HOME = "D:\dev\jdk\jdk-17.0.13+11"
$env:MAVEN_HOME = "D:\dev\maven\apache-maven-3.9.16"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

mvn spring-boot:run
```

### 4. Access

- Login page: http://localhost:8080/login
- Home page: http://localhost:8080 (after login)
- REST API: http://localhost:8080/api

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/cards` | List all cards |
| GET | `/api/trips` | List all trips |
| POST | `/api/trips` | Create a trip |
| GET | `/api/trips/{id}/readiness` | Readiness check |
| POST | `/api/payments` | Process payment |
| GET | `/api/trips/{id}/dashboard` | Dashboard data |
| GET | `/api/alerts` | List alerts |
| POST | `/api/alerts/{id}/confirm` | Confirm alert |
| POST | `/api/alerts/{id}/report` | Report fraud |
| POST | `/api/cards/{id}/freeze` | Freeze card |

## Test Scenarios

Three sample cards are seeded on startup:

- **Global Travel Card**: Sufficient balance, overseas payments enabled
- **Debit Card**: Overseas payments disabled, low balance
- **Business Platinum Card**: Frozen status

Use the Debit Card or Business Platinum Card to create a trip and see readiness warnings.

On the dashboard, simulate failure scenarios: insufficient balance, frozen card, limit exceeded, etc.

Enter a location that does not match the trip destination, or make a high-value payment, to trigger Phase 3 security alerts.


Start the application:
$env:JAVA_HOME = "D:\dev\jdk\jdk-17.0.13+11"
$env:MAVEN_HOME = "D:\dev\maven\apache-maven-3.9.16"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
cd "d:\ALL\HSBCResources\GlobalSafteyPass"
mvn spring-boot:run