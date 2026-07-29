# Global Safety Pass — Project Plan

## Goal

Build a mobile banking travel assistant that helps users:

- Prepare their cards before travelling
- Track travel spending and exchange rates
- Review suspicious overseas transactions
- Recover from failed payments


## Core Features

1. Trip creation and management
2. Travel readiness score with recommended actions
3. Budget and transaction dashboard
4. Suspicious transaction alerts
5. Failed-payment explanation and recovery

## Technology

- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA
- **Database:** MySQL
- **Frontend:** HTML, CSS, JavaScript
- **Testing:** JUnit and Spring Boot Test
- **API documentation:** Frankfurter API, countriesnow.space, OpenStreetMap

## Schedule

### Monday Afternoon — Completed

- [x] Define the MVP and core user journeys
- [x] Design the data model and REST APIs
- [x] Create the Spring Boot project
- [x] Implement the basic Trip and Card entities

### Tuesday — Completed

- [x] Implement trip CRUD and card management
- [x] Implement travel-readiness rules and recommended actions
- [x] Build the budget dashboard and transaction flows
- [x] Connect the frontend to the backend APIs
- [x] Integrate external data services:
  - **Frankfurter API:** Real exchange rates and currency data instead of mock rates
  - **CountriesNow:** Country, region, city, and primary-currency data for the trip form
  - **OpenStreetMap (Nominatim and Overpass):** Geocoding and nearby ATM search for backup cash planning

### Wednesday Morning

- [ ] Complete fraud alerts and failed-payment recovery
- [ ] Add seed data, tests, and exception handling
- [ ] Run integration testing and freeze the code

### Wednesday Afternoon

- [ ] Fix demo-blocking bugs only
- [ ] Create the slide deck
- [ ] Prepare speaking notes and the demo script

### Thursday Morning

- [ ] Test the project in a clean environment
- [ ] Refine the slides
- [ ] Complete a timed rehearsal

### Thursday Afternoon

- [ ] Run final rehearsals and prepare Q&A
