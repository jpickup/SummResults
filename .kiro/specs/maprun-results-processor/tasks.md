# Implementation Plan: MapRun Results Processor

## Overview

Implement a two-component application: a Spring Boot REST backend (Maven, Docker) that fetches and scores two-day MapRun orienteering results, and a Vue 3 + Vite + TypeScript frontend that displays them in a mobile-friendly table. The backend is the primary source of business logic; the frontend is a thin display layer.

## Tasks

- [x] 1. Initialise project structure and shared types
  - Create Maven project layout under `backend/` with `pom.xml` (Spring Boot 3, jqwik, WireMock, JUnit 5)
  - Create Vite + Vue 3 + TypeScript project under `frontend/` with `package.json` (Vitest, Vue Test Utils)
  - Define all Java record types: `MapRunResultsResponse`, `MapRunParticipantRaw`, `MapRunControlRaw`, `ControlVisit`, `DayResult`, `ParticipantResult`
  - Define all TypeScript interfaces: `ControlVisit`, `ParticipantResult`, `AppConfig` in `frontend/src/types.ts`
  - _Requirements: 4.2, 5.2_

- [x] 2. Implement the exception hierarchy
  - [x] 2.1 Create abstract `MapRunClientException` and all five concrete subclasses (`MapRunHttpErrorException`, `MapRunTimeoutException`, `MapRunEmptyBodyException`, `MapRunParseException`, `MapRunMissingFieldsException`), each carrying a `day` (`int`) field
    - _Requirements: 1.4, 1.5, 1.6, 1.8, 4.5_

- [ ] 3. Implement `ScoringEngine`
  - [x] 3.1 Write `ScoringEngine` as a `@Component` with a single public method `List<ParticipantResult> calculate(List<DayResult> day1Results, List<DayResult> day2Results)`
    - Build per-participant Day 1 control-ID sets
    - Compute `day2Deduction` as sum of points for controls appearing in both days
    - Compute `day1NetScore = day1GrossScore − day1Penalty`
    - Compute `day2NetScore = day2GrossScore − day2Deduction − day2Penalty`
    - Compute `totalScore = day1NetScore + day2NetScore`
    - Handle absent-Day-2 participants (zero all Day 2 fields)
    - Handle absent-Day-1 participants (zero Day 1 fields, no deduction)
    - Sort output descending by `totalScore`, then ascending by last name on tie
    - _Requirements: 2.1–2.6, 3.1–3.5_

  - [x] 3.2 Write property test: Property 1 — day2 deduction equals intersection sum
    - **Property 1: `day2DeductionEqualsIntersectionSum`**
    - Generate random Day 1 and Day 2 control sets; assert `deduction == sum(points of controls whose IDs appear in both lists)`
    - **Validates: Requirements 2.1, 2.2**

  - [x] 3.3 Write property test: Property 2 — Day 1 net score formula
    - **Property 2: `day1NetScoreFormula`**
    - Generate arbitrary gross score and penalty; assert `day1NetScore == grossScore − penalty`
    - **Validates: Requirements 2.4**

  - [x] 3.4 Write property test: Property 3 — Day 2 net score formula
    - **Property 3: `day2NetScoreFormula`**
    - Generate arbitrary gross score, deduction, and penalty; assert `day2NetScore == grossScore − deduction − penalty`
    - **Validates: Requirements 2.3**

  - [x] 3.5 Write property test: Property 4 — total score is sum of net scores
    - **Property 4: `totalScoreIsSumOfNetScores`**
    - Generate arbitrary `day1NetScore` and `day2NetScore`; assert `totalScore == day1NetScore + day2NetScore`
    - **Validates: Requirements 3.1, 3.2**

  - [x] 3.6 Write property test: Property 5 — results sorted descending score then ascending last name
    - **Property 5: `resultsAreSortedCorrectly`**
    - Generate random participant list; assert every adjacent pair `(r[i], r[i+1])` satisfies `r[i].totalScore > r[i+1].totalScore` OR `(r[i].totalScore == r[i+1].totalScore AND lastName(r[i]) ≤ lastName(r[i+1]))`
    - **Validates: Requirements 3.4, 3.5**

  - [x] 3.7 Write property test: Property 6 — absent Day 2 participant zeroes Day 2 fields
    - **Property 6: `absentDay2ZerosDay2Fields`**
    - Generate a participant present only in Day 1 data; assert `day2GrossScore == 0 && day2Deduction == 0 && day2NetScore == 0`
    - **Validates: Requirements 2.5**

  - [x] 3.8 Write property test: Property 7 — absent Day 1 participant has no deduction
    - **Property 7: `absentDay1ZeroesDay1AndNoDeduction`**
    - Generate a participant present only in Day 2 data; assert `day1GrossScore == 0 && day1NetScore == 0 && day2Deduction == 0`
    - **Validates: Requirements 2.6**

  - [x] 3.9 Write property test: Property 8 — unique Day 2 controls incur no deduction
    - **Property 8: `uniqueDay2ControlsIncurNoDeduction`**
    - Generate Day 2 controls with IDs guaranteed absent from Day 1; assert `deduction == 0`
    - **Validates: Requirements 2.1, 2.2**

  - [x] 3.10 Write unit tests for `ScoringEngine` (`ScoringEngineTest`)
    - Both days with overlapping controls
    - Day 1 only participant
    - Day 2 only participant
    - All controls overlap (maximum deduction)
    - No controls overlap (zero deduction)
    - Tie-breaking sort by last name
    - _Requirements: 2.1–2.6, 3.4, 3.5_

- [x] 4. Checkpoint — scoring engine
  - Ensure all `ScoringEngine` tests and property tests pass; ask the user if questions arise.

- [ ] 5. Implement `MapRunApiClient`
  - [x] 5.1 Create `MapRunApiClient` as a `@Component` wrapping Spring `RestClient`
    - Configure 30-second connect + read timeout via `RestClient.Builder`
    - Implement `DayResult fetchDay(String eventName, int day)` method
    - Parse `MapRunResultsResponse` from response body using Jackson
    - Throw `MapRunHttpErrorException` on 4xx/5xx upstream status
    - Throw `MapRunTimeoutException` on connect/read timeout
    - Throw `MapRunEmptyBodyException` on empty response body
    - Throw `MapRunParseException` on malformed JSON
    - Throw `MapRunMissingFieldsException` when `ResultsList`, `Name`, `GrossScore`, `Penalty`, or `ScoreControls` are missing
    - _Requirements: 1.1–1.8_

  - [x] 5.2 Write WireMock tests for `MapRunApiClient` (`MapRunApiClientTest`)
    - 200 response parsed correctly
    - HTTP 4xx maps to `MapRunHttpErrorException`
    - HTTP 5xx maps to `MapRunHttpErrorException`
    - Timeout maps to `MapRunTimeoutException`
    - Empty body maps to `MapRunEmptyBodyException`
    - Malformed JSON maps to `MapRunParseException`
    - Missing required fields maps to `MapRunMissingFieldsException`
    - _Requirements: 1.3–1.8_

- [ ] 6. Implement `ResultsService`
  - [x] 6.1 Create `ResultsService` as a `@Service` that calls `MapRunApiClient.fetchDay()` for Day 1 and Day 2 sequentially and passes results to `ScoringEngine.calculate()`
    - Return sorted `List<ParticipantResult>`
    - _Requirements: 1.1, 1.2, 2.1–2.6, 3.1–3.5_

- [ ] 7. Implement `GlobalExceptionHandler` and `ResultsController`
  - [x] 7.1 Create `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping:
    - `MissingServletRequestParameterException` → `400 { "error": "..." }`
    - `MapRunEmptyBodyException` → `400 { "error": "..." }`
    - `MapRunHttpErrorException | MapRunTimeoutException | MapRunParseException | MapRunMissingFieldsException` → `502 { "error": "..." }`
    - _Requirements: 4.3, 4.4, 4.5_

  - [x] 7.2 Create `ResultsController` handling `GET /api/results?day1EventId=&day2EventId=`
    - Validate both parameters are present and non-empty (return 400 if not)
    - Delegate to `ResultsService`
    - Return 200 with `List<ParticipantResult>` serialised as JSON
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 7.3 Write `@WebMvcTest` for `ResultsController` (`ResultsControllerTest`)
    - 400 when `day1EventId` absent
    - 400 when `day2EventId` absent
    - 400 when upstream returns empty body
    - 502 for upstream HTTP error
    - 200 with correct JSON shape on success
    - _Requirements: 4.1–4.5_

- [ ] 8. Implement `AppStartupValidator` and `CorsConfig`
  - [x] 8.1 Create `AppStartupValidator` as an `ApplicationListener<ApplicationReadyEvent>`
    - Read `DAY1_EVENT_ID` and `DAY2_EVENT_ID` from environment
    - Log an error identifying each missing or blank variable and call `System.exit(1)` if either is absent or blank
    - _Requirements: 6.1, 6.2, 7.4_

  - [x] 8.2 Create `CorsConfig` implementing `WebMvcConfigurer`
    - Read `ALLOWED_ORIGIN` environment variable (default `*`)
    - Register the allowed origin for `GET /api/**`
    - _Requirements: 4.6_

- [x] 9. Checkpoint — backend complete
  - Ensure all backend unit tests, property tests, and WireMock tests pass; ask the user if questions arise.

- [ ] 10. Implement `config.ts` and runtime configuration
  - [x] 10.1 Write `frontend/src/config.ts` that reads `window.__APP_CONFIG__` and exports typed `AppConfig`
    - Throw a descriptive error if `apiBaseUrl` is absent or empty
    - _Requirements: 6.3, 6.4_

  - [x] 10.2 Write Vitest unit tests for `config.ts` (`config.spec.ts`)
    - Error thrown when `apiBaseUrl` absent from `window.__APP_CONFIG__`
    - Correct values extracted when config is fully populated
    - _Requirements: 6.4_

- [ ] 11. Implement `useResults` composable
  - [x] 11.1 Write `frontend/src/composables/useResults.ts` using Vue 3 `ref` and `computed`
    - Expose `results: Ref<ParticipantResult[]>`, `loading: Ref<boolean>`, `error: Ref<string | null>`, `refresh(): void`
    - Use `fetch()` with base URL and event ID query params from config
    - Set `loading = true` for the duration of the request
    - Disable refresh button (`isRefreshing`) while in-flight
    - Populate `error` on non-2xx response or network failure
    - _Requirements: 5.4, 5.6, 5.7_

  - [x] 11.2 Write Vitest unit tests for `useResults` composable (`useResults.spec.ts`)
    - `loading` is `true` during fetch, `false` after
    - `error` set on non-2xx response
    - `results` populated on 200
    - Refresh button disabled during in-flight request
    - _Requirements: 5.5, 5.6, 5.7_

- [ ] 12. Implement `ResultsTable.vue`
  - [x] 12.1 Write `frontend/src/components/ResultsTable.vue` accepting `results: ParticipantResult[]` prop
    - Render one table row per participant
    - Include all required columns: name, day1Controls count, day1GrossScore, day1Penalty, day1NetScore, day2Controls count, day2GrossScore, day2Penalty, day2Deduction, day2NetScore, totalScore
    - Apply a CSS highlight class to `day2Deduction` cell when `day2Deduction > 0`
    - Wrap table in a `div` with `overflow-x: auto` for narrow viewports
    - Ensure layout is correct at 320 px viewport width
    - _Requirements: 5.1, 5.2, 5.3, 5.9, 5.10_

  - [x] 12.2 Write Vitest + Vue Test Utils tests for `ResultsTable.vue` (`ResultsTable.spec.ts`)
    - Correct number of rows rendered
    - Highlight class applied only when `day2Deduction > 0`
    - All required columns present in the header
    - _Requirements: 5.1, 5.2, 5.10_

- [ ] 13. Implement `App.vue` and wire frontend together
  - [x] 13.1 Write `frontend/src/App.vue` as the root component
    - Read `window.__APP_CONFIG__` via `config.ts`; render config-error state if `apiBaseUrl` is absent
    - Use `useResults` composable to manage fetch lifecycle
    - Render `LoadingSpinner` while `loading == true`
    - Render `ErrorMessage` when `error` is non-null
    - Render an empty-state message when results array is empty
    - Render `ResultsTable` on success
    - Render a refresh button that is disabled while `loading == true`
    - _Requirements: 5.4–5.8, 6.4_

  - [x] 13.2 Create `LoadingSpinner.vue`, `ErrorMessage.vue`, and `EmptyState.vue` stub components used by `App.vue`
    - _Requirements: 5.5, 5.7, 5.8_

  - [x] 13.3 Add `window.__APP_CONFIG__` block to `frontend/index.html` with placeholder event IDs and `apiBaseUrl`
    - _Requirements: 6.3_

- [x] 14. Checkpoint — frontend complete
  - Ensure all frontend Vitest tests pass; ask the user if questions arise.

- [ ] 15. Write multi-stage Dockerfile for the backend
  - [x] 15.1 Create `backend/Dockerfile` with a Maven build stage (produces the fat JAR) and a slim JRE runtime stage
    - Copy only the built JAR into the runtime image
    - Include `EXPOSE 8080`
    - Set the entrypoint to run the Spring Boot JAR
    - _Requirements: 7.1, 7.2, 7.3_

- [x] 16. Final checkpoint — full build
  - Ensure `mvn verify` passes all backend tests, `npm run test -- --run` passes all frontend tests, and `docker build` completes without errors; ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik with a minimum of 100 samples each
- WireMock tests for `MapRunApiClient` require a local WireMock server started per test class
- The `window.__APP_CONFIG__` pattern means the frontend can be redeployed to a new backend without a rebuild
- Checkpoints ensure incremental validation at backend, frontend, and full-stack boundaries

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["2.1"] },
    { "id": 1, "tasks": ["3.1", "10.1"] },
    { "id": 2, "tasks": ["3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "3.8", "3.9", "3.10", "5.1", "10.2"] },
    { "id": 3, "tasks": ["5.2", "6.1"] },
    { "id": 4, "tasks": ["7.1", "7.2", "11.1"] },
    { "id": 5, "tasks": ["7.3", "8.1", "8.2", "11.2", "12.1"] },
    { "id": 6, "tasks": ["12.2", "13.1"] },
    { "id": 7, "tasks": ["13.2", "13.3"] },
    { "id": 8, "tasks": ["15.1"] }
  ]
}
```
