# Global Safety Pass — MVP Product Requirements

## 1. Product summary

Global Safety Pass (working name: Travel Lion) is a mobile-first web application
that helps international travellers prepare their payment cards, automatically
understand overseas card activity, and respond to suspicious transactions.

The MVP is an English-language web experience presented in a phone-sized layout.
It uses simulated bank connectivity, but its transaction ingestion boundary is
designed so a real banking provider can replace the simulator later.

## 2. Product goals

1. Give a traveller a clear readiness result before departure.
2. Automatically associate incoming bank transactions with an active trip.
3. Explain declined payments in plain English and recommend a next action.
4. Detect suspicious overseas activity and let the user respond immediately.

## 3. Scope and assumptions

- The MVP uses one seeded demo user; registration and real authentication are out
  of scope.
- Cards, balances and payments are simulated. No real card credentials are stored.
- Exchange rates are included in simulated bank events.
- Risk detection is deterministic and rule-based.
- Fraud reports are internal records and are not sent to a bank or authorities.
- The UI is entirely in English.
- MySQL is the system of record. The browser is not the primary data store.

## 4. Functional requirements

### 4.1 Trip setup and travel readiness

The user can create a trip with a destination country/city, start and end dates,
budget, budget currency and preferred card. The app automatically suggests and
selects the destination's local currency while still allowing a manual override.
The return date must be later than the departure date; this rule is enforced in
both the browser and the server. The server also validates that the budget is
positive.

The readiness check produces a score from 0 to 100:

| Rule | Points |
| --- | ---: |
| Trip dates are valid | 20 |
| Card remains valid through the trip | 20 |
| Overseas payments are enabled | 20 |
| Card is not frozen | 15 |
| Card balance covers the trip budget | 15 |
| Daily payment limit is reasonable | 10 |

The result is labelled **Ready** (80–100), **Needs attention** (60–79), or
**Action required** (0–59). Each failed rule generates a plain-English warning
and an actionable recommendation. The card settings page allows the demo user to
enable overseas payments, change limits, add simulated funds, freeze or unfreeze
the card, and then rerun the check.

### 4.2 Automatic bank transaction recognition

Normal users do not enter expenses manually. A bank/provider sends transaction
events to the ingestion API. Every event contains a provider name, an external
transaction ID and an event ID. These fields are used for idempotency so duplicate
delivery does not create duplicate records or double count spending.

The server:

1. validates and deduplicates the event;
2. resolves the user's card;
3. automatically matches a trip by card, transaction date and destination;
4. stores the successful or declined transaction;
5. updates the card balance and trip spending for successful transactions;
6. maps decline codes to customer-friendly explanations;
7. runs suspicious-activity rules.

Transactions that cannot be associated with a trip remain visible as
**Unmatched**. The MVP includes a separate **Demo Lab** that can generate
simulated payments and bank events. This is a testing surface, not a manual
expense-entry workflow.

Supported decline reasons:

- insufficient balance;
- frozen card;
- overseas payments disabled;
- single transaction limit exceeded;
- daily limit exceeded;
- expired card.

### 4.3 Suspicious transaction monitoring

The rules engine creates an alert when it detects:

- a transaction outside trip dates;
- a transaction outside the trip destination;
- a probable duplicate charge;
- an unusually high-value transaction;
- a high-value or repeated ATM withdrawal.

Alerts have Low, Medium or High severity and explain why the activity was
flagged. A user can:

- confirm that the transaction is safe;
- report it as suspicious and create a fraud report;
- freeze the associated card;
- dismiss the alert.

Freezing a card affects subsequent payment decisions immediately.

## 5. Primary screens

- **Home** — active trip, readiness score, spend/remaining budget, recent activity
  and outstanding-alert summary.
- **Trips** — trip list, trip creation and readiness details.
- **Activity** — automatically imported successful, declined and unmatched
  transactions.
- **Alerts** — outstanding and resolved security alerts with response actions.
- **Profile / My Cards** — card status, overseas use, balance and payment limits.
- **Demo Lab** — simulated payment and inbound bank-event controls.

Bottom navigation uses Home, Trips, Activity, Alerts and Profile.

## 6. UI requirements

- Mobile-first layout, approximately 390 px wide on desktop and full-width on
  mobile browsers.
- Clean, restrained visual language with generous spacing and minimal decoration.
- Light neutral background, deep navy/green primary colour, green success,
  amber warning and red reserved for failures/high risk.
- English labels, messages, validation and empty states.
- Touch-friendly controls, visible keyboard focus and accessible labels.
- Confirmation before freezing a card or reporting a transaction.
- Concise toast/status feedback after user actions.

## 7. System architecture

- **Frontend:** React, Vite, JavaScript, React Router and CSS.
- **Backend:** Java 17+, Spring Boot, Spring Web, Spring Data JPA and validation.
- **Database:** MySQL 8+.
- **Integration boundary:** REST endpoint that represents a bank webhook/provider
  feed, with a provider adapter abstraction.

Core entities are User, Card, Trip, ReadinessCheck, Transaction, RiskAlert and
FraudReport.

## 8. MVP acceptance criteria

- A trip can be created and persists after a page refresh.
- A readiness score and individual warnings are calculated by the backend.
- Fixing card settings and rerunning the check changes the result.
- A simulated inbound bank event automatically appears in Activity.
- Duplicate events are safely ignored.
- A successful payment changes card balance and trip budget figures.
- A declined payment is not counted as spend and includes an explanation/action.
- Transactions are automatically matched to a trip or marked Unmatched.
- All five suspicious-activity scenarios can produce an alert.
- Alerts can be confirmed, dismissed or reported; cards can be frozen.
- A payment on a frozen card is declined by the backend.
- All customer-facing UI is English and presented as a compact mobile app.
