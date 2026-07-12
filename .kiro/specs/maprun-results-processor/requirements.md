# Requirements Document

## Introduction

This feature delivers a two-component application for processing and displaying orienteering results from the MapRun platform. A Java back-end service (deployed in Docker) fetches raw results from the MapRun public API for each day of a two-day event and applies deduplication scoring logic: any control visited on day 2 that already contributed to a participant's day 1 score is excluded from the day 2 gross score before calculating the net score. A mobile-friendly Vue.js web UI then presents the processed results in a tabular format showing per-day and combined totals for each participant.

## Glossary

- **System**: The MapRun Results Processor application as a whole.
- **Backend**: The Java REST service running inside a Docker container.
- **UI**: The Vue.js single-page application served to the user's browser.
- **MapRun_API**: The external MapRun results endpoint at `https://p.fne.com.au:8886/resultsGetPublicForEventv2`.
- **Event**: A two-day orienteering event identified by a pair of MapRun event IDs (Day 1 and Day 2).
- **Participant**: A competitor identified by name who has results in one or both days of the Event.
- **Control**: A numbered checkpoint that a Participant visits during a day's course. Each Control visit carries a point value.
- **Gross_Score**: The raw total points earned by a Participant on a single day before deduplication adjustments.
- **Duplicate_Control**: A Control visited on Day 2 whose point value was already included in the Participant's Day 1 Gross_Score.
- **Deduction**: The total points removed from a Day 2 Gross_Score because of Duplicate_Controls.
- **Net_Score**: The Gross_Score minus any Deduction for a given day. Day 1 Net_Score equals Day 1 Gross_Score (no deduction possible on Day 1).
- **Total_Score**: The sum of the Day 1 Net_Score and the Day 2 Net_Score for a Participant.
- **Penalty**: A time-based or rule-based point deduction applied by MapRun to a single day's result (sourced directly from the MapRun_API response).
- **Results_Table**: The tabular display in the UI listing all Participants with their scores.

## Requirements

### Requirement 1: Fetch Day Results from MapRun API

**User Story:** As an event administrator, I want the system to retrieve raw results for each day of the event from the MapRun API, so that the application always shows current data without manual data entry.

#### Acceptance Criteria

1. WHEN a results request is received by the Backend, THE Backend SHALL make an HTTP GET request to the MapRun_API endpoint for the Day 1 event ID.
2. WHEN a results request is received by the Backend, THE Backend SHALL make an HTTP GET request to the MapRun_API endpoint for the Day 2 event ID.
3. WHEN the MapRun_API returns an HTTP 200 response, THE Backend SHALL parse the response into a list of Participant results including Controls visited, Gross_Score, and Penalty for that day.
4. IF the MapRun_API returns an HTTP error status for either day, THEN THE Backend SHALL return an error response to the caller with a descriptive message identifying which day's fetch failed.
5. IF the MapRun_API returns a 200 OK response with an empty body or a body that cannot be parsed as valid JSON, THEN THE Backend SHALL return an error response to the caller with a descriptive message indicating the parsing failure for the affected day.
6. IF the MapRun_API connection does not respond within 30 seconds, THEN THE Backend SHALL abort the request and return an error response to the caller with a descriptive message indicating the timeout for the affected day.
7. THE Backend SHALL not cache or persist API responses between requests.
8. IF the MapRun_API returns a 200 OK response with valid JSON that is missing one or more required fields (Controls, Gross_Score, or Penalty) for a Participant, THEN THE Backend SHALL return an error response to the caller with a descriptive message identifying the missing fields and the affected day.

### Requirement 2: Deduplicate Controls Across Days

**User Story:** As an event administrator, I want the system to remove from a participant's Day 2 score any controls that were already scored on Day 1, so that participants cannot earn double points for visiting the same control across both days.

#### Acceptance Criteria

1. WHEN a score calculation is requested, THE Backend SHALL identify every Control (matched by Control ID) visited by a Participant on Day 2 that also appears in that same Participant's Day 1 visited Controls list.
2. WHEN a Duplicate_Control is identified, THE Backend SHALL exclude that Control's point value from the Participant's Day 2 Net_Score calculation.
3. WHEN calculating the Day 2 Net_Score, THE Backend SHALL compute it as: Day 2 Gross_Score minus the sum of point values of all Duplicate_Controls minus any Penalty for Day 2.
4. WHEN calculating the Day 1 Net_Score, THE Backend SHALL compute it as: Day 1 Gross_Score minus any Penalty for Day 1.
5. WHEN a Participant has no Controls recorded in the dataset for Day 2, THE Backend SHALL set the Day 2 Gross_Score, Deduction, and Day 2 Net_Score to zero for that Participant.
6. WHEN a Participant has no Controls recorded in the dataset for Day 1, THE Backend SHALL set the Day 1 Gross_Score, Deduction, and Day 1 Net_Score to zero for that Participant, and the Day 2 Net_Score SHALL equal the Day 2 Gross_Score minus any Penalty for Day 2.

### Requirement 3: Calculate Total Score

**User Story:** As a participant, I want to see my combined score across both days, so that I can understand my overall standing in the event.

#### Acceptance Criteria

1. THE Backend SHALL calculate the Total_Score for each Participant as the sum of the Day 1 Net_Score and the Day 2 Net_Score.
2. WHEN a Participant has no Net_Score for a given day (absent results), THE Backend SHALL treat that day's Net_Score as zero when calculating Total_Score.
3. WHEN constructing the processed results response, THE Backend SHALL include the Total_Score for every Participant.
4. WHEN the processed results list is returned, THE Backend SHALL sort it in descending order of Total_Score.
5. WHEN two Participants have equal Total_Scores, THE Backend SHALL sort those Participants alphabetically by last name as a secondary sort key.

### Requirement 4: Expose Processed Results via REST API

**User Story:** As a front-end developer, I want a REST endpoint that returns fully processed results for a two-day event, so that the UI can display them without performing any scoring logic itself.

#### Acceptance Criteria

1. THE Backend SHALL expose a REST GET endpoint at `/api/results` that accepts `day1EventId` and `day2EventId` as query parameters.
2. WHEN the `/api/results` endpoint is called with valid event IDs, THE Backend SHALL return an HTTP 200 response containing a JSON array of processed Participant result objects, where each object includes: Participant name, Day 1 controls visited, Day 1 Gross_Score, Day 1 Penalty, Day 1 Net_Score, Day 2 controls visited, Day 2 Gross_Score, Day 2 Penalty, Day 2 Deduction, Day 2 Net_Score, and Total_Score.
3. WHEN the `/api/results` endpoint is called with a missing `day1EventId` or `day2EventId` parameter, THE Backend SHALL return an HTTP 400 response with a descriptive error message before attempting any upstream API calls.
4. WHEN event ID parameters are present but the MapRun_API returns an empty body or unparseable response indicating the event does not exist, THE Backend SHALL return an HTTP 400 response with a descriptive error message indicating which event ID is invalid.
5. WHEN the Backend encounters an upstream HTTP error status, connection timeout, or parse failure on a non-empty body from the MapRun_API, THE Backend SHALL return an HTTP 502 response with a descriptive error message identifying the failure mode and affected day.
6. THE Backend SHALL include CORS headers in all responses to permit requests from the configured UI origin.

### Requirement 5: Display Results in Mobile-Friendly Web UI

**User Story:** As a participant or spectator, I want to view the processed results in a clear, mobile-friendly table on my phone or tablet, so that I can easily check standings at the event venue.

#### Acceptance Criteria

1. THE UI SHALL render a Results_Table displaying one row per Participant.
2. THE Results_Table SHALL include the following columns: Participant name, Day 1 controls visited, Day 1 Gross_Score, Day 1 Penalty, Day 1 Net_Score, Day 2 controls visited, Day 2 Gross_Score, Day 2 Penalty, Day 2 Deduction, Day 2 Net_Score, and Total_Score.
3. THE UI SHALL display the Results_Table sorted in descending order of Total_Score as returned by the Backend.
4. WHEN the UI is loaded, THE UI SHALL automatically fetch results from the Backend `/api/results` endpoint using the event IDs specified in the UI configuration.
5. IF the Backend returns an error response, THEN THE UI SHALL display a human-readable error message indicating that results could not be loaded, in place of the Results_Table.
6. WHEN the user activates the refresh button, THE UI SHALL re-fetch results from the Backend; the refresh button SHALL be disabled while a fetch is in progress.
7. WHILE a results fetch is in progress, THE UI SHALL display a loading indicator in place of the Results_Table.
8. WHEN the Backend returns a result set containing zero Participants, THE UI SHALL display a message indicating no results are available, rather than an empty table.
9. THE UI SHALL render correctly on viewport widths of 320px and above, with all Results_Table columns accessible without horizontal page scroll, either by fitting within the viewport width or via an independent horizontal scroll on the table element only.
10. WHERE the Participant's Day 2 Deduction is greater than zero, THE UI SHALL apply a distinct text colour or background colour to the Deduction cell in that Participant's row, differing visually from non-highlighted cells in the same column.

### Requirement 6: Configure Event IDs

**User Story:** As an event administrator, I want to configure the Day 1 and Day 2 MapRun event IDs, so that the application can be pointed at different events without code changes.

#### Acceptance Criteria

1. THE Backend SHALL read the Day 1 and Day 2 MapRun event IDs from environment variables named `DAY1_EVENT_ID` and `DAY2_EVENT_ID` respectively.
2. WHEN the Backend starts and a required environment variable (`DAY1_EVENT_ID` or `DAY2_EVENT_ID`) is absent or set to an empty string, THE Backend SHALL log an error message identifying which variable is missing or empty and exit with a non-zero exit code.
3. THE UI's Backend API base URL SHALL be settable at deployment time without modifying the UI source code, such that the same built artefact can be pointed at different Backend instances.
4. WHEN the UI loads and its Backend API base URL configuration value is absent or empty, THE UI SHALL display an error message indicating that the API URL is not configured and SHALL not attempt any Backend API calls.

### Requirement 7: Docker Deployment

**User Story:** As a system operator, I want the back-end service packaged as a Docker container, so that it can be deployed consistently across different host environments.

#### Acceptance Criteria

1. WHEN the Docker build command is run against the included Dockerfile, THE build SHALL complete without errors and produce a runnable image.
2. WHEN the Docker image is started with the required environment variables set, THE Backend SHALL start and emit a readiness log message to stdout within 30 seconds, indicating it is accepting HTTP requests on port 8080.
3. THE Dockerfile SHALL include an `EXPOSE 8080` instruction so that the container port can be mapped to a host port at runtime.
4. WHEN the Docker container starts and a required environment variable is absent or empty, THE Backend SHALL exit with a non-zero exit code and emit an error message to stderr identifying the missing variable(s).
