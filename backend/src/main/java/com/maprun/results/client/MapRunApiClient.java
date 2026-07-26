package com.maprun.results.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maprun.results.exception.MapRunEmptyBodyException;
import com.maprun.results.exception.MapRunHttpErrorException;
import com.maprun.results.exception.MapRunMissingFieldsException;
import com.maprun.results.exception.MapRunParseException;
import com.maprun.results.exception.MapRunTimeoutException;
import com.maprun.results.model.ControlVisit;
import com.maprun.results.model.DayResult;
import com.maprun.results.model.MapRunParticipantRaw;
import com.maprun.results.model.MapRunResultsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client wrapping Spring {@link RestClient} for the MapRun public results API.
 *
 * <p>Configured with a 30-second connect and read timeout. All HTTP and
 * network-level errors are translated to typed {@link com.maprun.results.exception.MapRunClientException}
 * subclasses so that callers never need to handle raw Spring/HTTP exceptions.</p>
 */
@Component
public class MapRunApiClient {

    private static final Logger logger = LoggerFactory.getLogger(MapRunApiClient.class);

    private static final String BASE_URL =
            "https://p.fne.com.au:8886/resultsGetPublicForEventv2";

    private static final int TIMEOUT_MS = 30_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public MapRunApiClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this(builder, objectMapper, BASE_URL, TIMEOUT_MS);
    }

    /**
     * Package-private constructor for testing: allows overriding the base URL and
     * timeout so that tests can point the client at a WireMock server with a
     * short timeout.
     */
    MapRunApiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                    String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        this.restClient = builder
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches results for a single event day from the MapRun API and converts
     * each participant entry to a {@link DayResult}.
     *
     * @param eventName the MapRun event name used as the {@code eventName} query parameter
     * @param day       which day this fetch represents (1 or 2) — used only for error context
     * @return one {@link DayResult} per participant in the MapRun response
     * @throws MapRunHttpErrorException      if the API returns a 4xx or 5xx status
     * @throws MapRunTimeoutException        if the connection or read exceeds 30 seconds
     * @throws MapRunEmptyBodyException      if the response body is empty or blank
     * @throws MapRunParseException          if the body cannot be parsed as valid JSON
     * @throws MapRunMissingFieldsException  if required fields are absent in the parsed JSON
     */
    public List<DayResult> fetchDay(String eventName, int day) {
        logger.info("MapRun API request: day={}, eventName={}", day, eventName);
        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("eventName", eventName)
                            .build())
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new MapRunHttpErrorException(
                                        day,
                                        response.getStatusCode().value(),
                                        "MapRun API returned HTTP " + response.getStatusCode().value()
                                                + " for day " + day);
                            })
                    .body(String.class);
        } catch (MapRunHttpErrorException e) {
            logger.info("MapRun API response: day={}, eventName={} → HTTP {}", day, eventName, e.getHttpStatus());
            throw e;
        } catch (ResourceAccessException e) {
            logger.info("MapRun API response: day={}, eventName={} → timeout", day, eventName);
            throw new MapRunTimeoutException(day,
                    "Request to MapRun API timed out after " + (TIMEOUT_MS / 1000)
                            + " seconds for day " + day, e);
        }

        if (body == null || body.isBlank()) {
            logger.info("MapRun API response: day={}, eventName={} → empty body", day, eventName);
            throw new MapRunEmptyBodyException(day,
                    "MapRun API returned an empty response body for day " + day
                            + " — event name may be invalid");
        }

        try {
            Object parsed = objectMapper.readValue(body, Object.class);
            logger.info("MapRun API response body: day={}, eventName={}\n{}",
                    day, eventName, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed));
        } catch (Exception ignored) {
            // If pretty-printing fails for any reason, fall back to the raw body.
            logger.info("MapRun API response body: day={}, eventName={}\n{}", day, eventName, body);
        }

        MapRunResultsResponse response;
        try {
            response = objectMapper.readValue(body, MapRunResultsResponse.class);
        } catch (Exception e) {
            logger.info("MapRun API response: day={}, eventName={} → parse error: {}", day, eventName, e.getMessage());
            throw new MapRunParseException(day,
                    "Failed to parse MapRun API response for day " + day + ": " + e.getMessage(), e);
        }

        if (response.resultsList() == null) {
            logger.info("MapRun API response: day={}, eventName={} → missing ResultsList field", day, eventName);
            throw new MapRunMissingFieldsException(day,
                    "MapRun API response for day " + day + " is missing the 'ResultsList' field");
        }

        List<DayResult> results = toDayResults(response.resultsList(), day);
        logger.info("MapRun API response: day={}, eventName={} → 200, {} participant(s)", day, eventName, results.size());
        return results;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<DayResult> toDayResults(List<MapRunParticipantRaw> rawList, int day) {
        List<DayResult> results = new ArrayList<>(rawList.size());
        for (MapRunParticipantRaw raw : rawList) {
            validateParticipant(raw, day);
            String name = (raw.firstname() + " " + raw.surname()).trim();
            // The real MapRun API does not provide per-control point values.
            // Controls are mapped with points=0; the gross/net scores are used
            // for all scoring calculations instead.
            List<ControlVisit> controls = raw.punchControlIds().stream()
                    .map(id -> new ControlVisit(id, idToScore(id), false))
                    .toList();
            int penalty = raw.grossScore() - raw.netScore();
            results.add(new DayResult(name, controls, raw.grossScore(), penalty));
        }
        return results;
    }

    private int idToScore(String id) {
        if (id.contains("(Extra)")) return 0;
        try {
            int controlNumber = Integer.parseInt(id);
            return (controlNumber/10) * 10;
        }
        catch (NumberFormatException ex) {
            logger.warn("Invalid control number {}, setting score to zero", id);
            return 0;
        }
    }

    private void validateParticipant(MapRunParticipantRaw raw, int day) {
        List<String> missing = new ArrayList<>();
        if (raw.surname() == null || raw.surname().isBlank()) {
            missing.add("Surname");
        }
        if (raw.punchControlIds() == null) {
            missing.add("punchControlIds");
        }
        if (!missing.isEmpty()) {
            throw new MapRunMissingFieldsException(day,
                    "Participant entry in day " + day + " response is missing required fields: "
                            + String.join(", ", missing));
        }
    }
}
