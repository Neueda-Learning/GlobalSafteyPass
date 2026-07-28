# Global Safety Pass

Alias: **Travel Lion**

An English, mobile-first travel payment safety MVP. It checks travel readiness,
automatically processes simulated bank transactions, explains declined payments,
and detects suspicious overseas activity.

## Stack

- React + Vite frontend
- Spring Boot REST API
- MySQL

## Run the H2 demo

The H2 demo does not require MySQL or Docker. H2 runs in memory, so data is
reset when the backend stops.

1. Start the API on port `18080`:

   ```bash
   cd backend
   PORT=18080 mvn spring-boot:run -Dspring-boot.run.profiles=demo
   ```

   Wait until the terminal shows:

   ```text
   Tomcat started on port 18080
   Started GlobalSafetyPassApplication
   ```

2. Start the web app in another terminal and point its API proxy to the same
   backend port:

   ```bash
   cd frontend
   npm install
   API_PROXY=http://127.0.0.1:18080 npm run dev
   ```

3. Open `http://localhost:5173`.

If either service was already running, stop it with `Control + C` before using
these commands. Vite reads `API_PROXY` only when it starts, so the frontend must
be restarted after changing the backend port.

## Run with MySQL

Requirements: Node.js 20+, Java 17+, Maven 3.9+ and Docker.

1. Start MySQL:

   ```bash
   docker compose up -d mysql
   ```

2. Start the API:

   ```bash
   cd backend
   PORT=18080 mvn spring-boot:run
   ```

3. Start the web app in another terminal:

   ```bash
   cd frontend
   npm install
   API_PROXY=http://127.0.0.1:18080 npm run dev
   ```

Open `http://localhost:5173`. The API runs on `http://localhost:18080`.

Database settings can be overridden with `DB_URL`, `DB_USERNAME` and
`DB_PASSWORD`. See [REQUIREMENTS.md](./REQUIREMENTS.md) for the agreed MVP scope.
