package com.maprun.results.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maprun.results.exception.MapRunEmptyBodyException;
import com.maprun.results.exception.MapRunHttpErrorException;
import com.maprun.results.exception.MapRunMissingFieldsException;
import com.maprun.results.exception.MapRunParseException;
import com.maprun.results.exception.MapRunTimeoutException;
import com.maprun.results.model.ControlVisit;
import com.maprun.results.model.DayResult;
import com.maprun.results.model.MapRunControlRaw;
import com.maprun.results.model.MapRunParticipantRaw;
import com.maprun.results.model.MapRunResultsResponse;
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

    private static final String BASE_URL =
            "https://p.fne.com.au:8886/resultsGetPublicForEventv2";

    private static final int TIMEOUT_MS = 30_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

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
        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("eventName", eventName)
                            .build())
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            (request, response) -> {
                                throw new MapRunHttpErrorException(
                                        day,
                                        response.getStatusCode().value(),
                                        "MapRun API returned HTTP " + response.getStatusCode().value()
                                                + " for day " + day);
                            })
                    .body(String.class);
        } catch (MapRunHttpErrorException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new MapRunTimeoutException(day,
                    "Request to MapRun API timed out after " + (TIMEOUT_MS / 1000)
                            + " seconds for day " + day, e);
        }

        if (body == null || body.isBlank()) {
            throw new MapRunEmptyBodyException(day,
                    "MapRun API returned an empty response body for day " + day
                            + " — event name may be invalid");
        }

        MapRunResultsResponse response;
        try {
            response = objectMapper.readValue(body, MapRunResultsResponse.class);
        } catch (Exception e) {
            throw new MapRunParseException(day,
                    "Failed to parse MapRun API response for day " + day + ": " + e.getMessage(), e);
        }

        if (response.resultsList() == null) {
            throw new MapRunMissingFieldsException(day,
                    "MapRun API response for day " + day + " is missing the 'ResultsList' field");
        }

        return toDayResults(response.resultsList(), day);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<DayResult> toDayResults(List<MapRunParticipantRaw> rawList, int day) {
        List<DayResult> results = new ArrayList<>(rawList.size());
        for (MapRunParticipantRaw raw : rawList) {
            validateParticipant(raw, day);
            List<ControlVisit> controls = raw.scoreControls().stream()
                    .map(c -> new ControlVisit(c.controlId(), c.points()))
                    .toList();
            results.add(new DayResult(raw.name(), controls, raw.grossScore(), raw.penalty()));
        }
        return results;
    }

    private void validateParticipant(MapRunParticipantRaw raw, int day) {
        List<String> missing = new ArrayList<>();
        if (raw.name() == null || raw.name().isBlank()) {
            missing.add("Name");
        }
        if (raw.scoreControls() == null) {
            missing.add("ScoreControls");
        }
        if (!missing.isEmpty()) {
            throw new MapRunMissingFieldsException(day,
                    "Participant entry in day " + day + " response is missing required fields: "
                            + String.join(", ", missing));
        }
    }
}
