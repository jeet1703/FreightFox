# Alumni LinkedIn Profile Searcher

Spring Boot REST API that searches LinkedIn for alumni of a given university through the
PhantomBuster API and saves the results in PostgreSQL.

- Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation)
- PostgreSQL (H2 in tests and in the optional `local` profile)
- PhantomBuster v2 API with a "LinkedIn Search Export" phantom

## Running

### 1. Start PostgreSQL

```
docker compose up -d
```

This starts PostgreSQL 16 on `localhost:5433` with database, user and password all set to `alumni`.
To use an existing server instead, set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.
The `alumni_profiles` table is created automatically on startup.

### 2. Configure PhantomBuster

1. In PhantomBuster, add the **LinkedIn Search Export** phantom to your workspace and connect it
   to your LinkedIn account (this stores your LinkedIn session cookie in the phantom's setup).
2. Copy the phantom's id (the number in its URL) and your API key (Workspace settings > API keys).
3. Set the environment variables:

| Variable | Required | Meaning |
| --- | --- | --- |
| `PHANTOMBUSTER_API_KEY` | yes | PhantomBuster API key |
| `PHANTOMBUSTER_AGENT_ID` | yes | Id of the LinkedIn Search Export phantom |
| `LINKEDIN_SESSION_COOKIE` | no | LinkedIn `li_at` cookie; overrides the one saved in the phantom |
| `PHANTOMBUSTER_MOCK` | no | `true` returns sample profiles without calling PhantomBuster |

### 3. Start the application

PowerShell:

```
$env:PHANTOMBUSTER_API_KEY = "your-key"
$env:PHANTOMBUSTER_AGENT_ID = "your-agent-id"
.\mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

### Running everything in Docker

The `Dockerfile` builds the API image, and `docker-compose.yml` has an `app` service for it that
is started only when the `app` profile is requested:

```
docker compose --profile app up -d --build
```

This starts PostgreSQL and the API together. The PhantomBuster variables above are passed through
from your shell, and `APP_PORT` changes the host port (default 8080).

Swagger UI is at `http://localhost:8080/swagger-ui/index.html`.

### Trying it without PostgreSQL or PhantomBuster

The `local` profile uses an in-memory H2 database and sample profiles:

```
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

## API

### POST /api/alumni/search

```
curl -X POST http://localhost:8080/api/alumni/search ^
  -H "Content-Type: application/json" ^
  -d "{\"university\": \"University of XYZ\", \"designation\": \"Software Engineer\", \"passoutYear\": \"2020\"}"
```

`university` and `designation` are required. `passoutYear` is optional and may be a number or a
numeric string. A real search takes as long as the phantom runs (typically 30 seconds to a couple
of minutes); the request waits up to `phantombuster.timeout` (3 minutes by default).

```json
{
  "status": "success",
  "data": [
    {
      "name": "John Doe",
      "currentRole": "Software Engineer",
      "university": "University of XYZ",
      "location": "New York, NY",
      "linkedinHeadline": "Passionate Software Engineer at XYZ Corp",
      "passoutYear": 2020
    }
  ]
}
```

### GET /api/alumni/all

Returns every saved profile in the same format.

### Errors

Errors use the same envelope: `{"status": "error", "message": "..."}`.

| Status | When |
| --- | --- |
| 400 | Missing `university`/`designation`, non-numeric `passoutYear`, malformed JSON |
| 502 | PhantomBuster is not configured, failed, or timed out |

## How it works

1. `AlumniController` validates the request and calls `AlumniService`.
2. `HttpPhantomBusterClient` builds a LinkedIn people-search URL from the designation, university
   and year, launches the phantom (`POST /agents/launch`), polls `GET /containers/fetch` until the
   run finishes, and reads the profiles from `GET /containers/fetch-result-object` (falling back to
   the phantom's `result.json` file).
3. `AlumniService` saves the profiles. A profile is identified by its LinkedIn URL, so repeating a
   search updates existing rows instead of duplicating them.

Notes on the data:

- The search is keyword based, so LinkedIn decides relevance. When a result includes education
  dates, profiles whose end year differs from the requested `passoutYear` are dropped.
- When a result has no school or education dates, `university` and `passoutYear` in the response
  are taken from the request.

## Project structure

```
src/main/java/com/example/alumni
  controller/   REST endpoints (AlumniController)
  service/      search-and-save logic (AlumniService)
  client/       PhantomBuster integration (real HTTP client and a sample-data client)
  repository/   Spring Data JPA repository
  model/        JPA entity (alumni_profiles table)
  dto/          request and response bodies
  config/       PhantomBuster settings, Swagger/OpenAPI metadata
  exception/    error types and the global error handler
src/main/resources
  application.properties         default configuration (PostgreSQL, PhantomBuster)
  application-local.properties   `local` profile (H2 + sample data)
src/test/java/com/example/alumni  tests, mirroring the packages above
Dockerfile, docker-compose.yml    container image and local PostgreSQL
.env.example                      environment variables to copy into .env
```

## Tests

```
.\mvnw test
```

Tests run against H2 with PhantomBuster mocked; they do not need PostgreSQL or network access.
