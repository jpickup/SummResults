package com.maprun.results.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.maprun.results.exception.MapRunEmptyBodyException;
import com.maprun.results.exception.MapRunHttpErrorException;
import com.maprun.results.exception.MapRunMissingFieldsException;
import com.maprun.results.exception.MapRunParseException;
import com.maprun.results.exception.MapRunTimeoutException;
import com.maprun.results.model.DayResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WireMock-backed integration tests for {@link MapRunApiClient}.
 *
 * <p>Each test configures WireMock to stub the root path ({@code /}) —
 * the base URL already contains the full endpoint path, so the URI
 * builder in {@code fetchDay} appends only the query string.</p>
 *
 * <p>Validates: Requirements 1.3–1.8</p>
 */
class MapRunApiClientTest {

    /** WireMock on a random port, restarted for each test class. */
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    /** Short timeout (ms) used for tests — avoids waiting 30 s for timeout cases. */
    private static final int TEST_TIMEOUT_MS = 1_000;

    private MapRunApiClient client;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + wireMock.getPort();
        client = new MapRunApiClient(
                RestClient.builder(),
                new ObjectMapper(),
                baseUrl,
                TEST_TIMEOUT_MS
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Loads a classpath resource as a UTF-8 string. */
    private static String loadResource(String path) throws IOException {
        try (InputStream is = MapRunApiClientTest.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // -------------------------------------------------------------------------
    // 1. Successful 200 response is parsed into correct DayResult list
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.3 — 200 response parsed into Participant list
     * with controls, GrossScore, and derived penalty for that day.
     *
     * <p>Assertions are grounded in the real fixture (maprun-response.json):
     * <ul>
     *   <li>12 participants total</li>
     *   <li>First entry: John Pickup — GrossScore=1090, NetScore=1090 → penalty=0, 33 controls</li>
     *   <li>Third entry: Dan Martyn — GrossScore=1000, NetScore=720 → penalty=280, 29 controls</li>
     * </ul>
     */
    @Test
    void successfulResponseIsParsedCorrectly() throws IOException {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadResource("maprun-response.json"))));

        List<DayResult> results = client.fetchDay("SUMM Day 1 v2 ScoreP420", 1);

        assertThat(results).hasSize(12);

        // First participant: John Pickup — no penalty, 33 controls
        DayResult pickup = results.get(0);
        assertThat(pickup.participantName()).isEqualTo("John Pickup");
        assertThat(pickup.grossScore()).isEqualTo(1090);
        assertThat(pickup.penalty()).isEqualTo(0);
        assertThat(pickup.controls()).hasSize(33);
        assertThat(pickup.controls().get(0).controlId()).isEqualTo("45");
        // Per-control points are not provided by the API — always 0
        assertThat(pickup.controls().get(0).points()).isEqualTo(0);

        // Third participant: Dan Martyn — penalty = GrossScore(1000) - NetScore(720)
        DayResult martyn = results.get(2);
        assertThat(martyn.participantName()).isEqualTo("Dan Martyn");
        assertThat(martyn.grossScore()).isEqualTo(1000);
        assertThat(martyn.penalty()).isEqualTo(280);
        assertThat(martyn.controls()).hasSize(29);
    }

    // -------------------------------------------------------------------------
    // 2. HTTP 4xx maps to MapRunHttpErrorException
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.4 — HTTP error status triggers error response
     * identifying which day's fetch failed.
     */
    @Test
    void http4xxMapsToMapRunHttpErrorException() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(404)));

        assertThatThrownBy(() -> client.fetchDay("Unknown Event", 1))
                .isInstanceOf(MapRunHttpErrorException.class)
                .satisfies(ex -> {
                    MapRunHttpErrorException e = (MapRunHttpErrorException) ex;
                    assertThat(e.getHttpStatus()).isEqualTo(404);
                    assertThat(e.getDay()).isEqualTo(1);
                });
    }

    // -------------------------------------------------------------------------
    // 3. HTTP 5xx maps to MapRunHttpErrorException
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.4 — HTTP error status (5xx) triggers error
     * response identifying which day's fetch failed.
     */
    @Test
    void http5xxMapsToMapRunHttpErrorException() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(503)));

        assertThatThrownBy(() -> client.fetchDay("Test Event", 2))
                .isInstanceOf(MapRunHttpErrorException.class)
                .satisfies(ex -> {
                    MapRunHttpErrorException e = (MapRunHttpErrorException) ex;
                    assertThat(e.getHttpStatus()).isEqualTo(503);
                    assertThat(e.getDay()).isEqualTo(2);
                });
    }

    // -------------------------------------------------------------------------
    // 4. Timeout maps to MapRunTimeoutException
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.6 — connection not responding within the
     * configured timeout causes a timeout error identifying the affected day.
     *
     * <p>The client is configured with a {@value TEST_TIMEOUT_MS} ms read
     * timeout; WireMock delays the response by 3 × that value to reliably
     * trigger the timeout.</p>
     */
    @Test
    void timeoutMapsToMapRunTimeoutException() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(TEST_TIMEOUT_MS * 3)  // 3 s > 1 s timeout
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        assertThatThrownBy(() -> client.fetchDay("Test Event", 1))
                .isInstanceOf(MapRunTimeoutException.class)
                .satisfies(ex -> assertThat(((MapRunTimeoutException) ex).getDay()).isEqualTo(1));
    }

    // -------------------------------------------------------------------------
    // 5. Empty body maps to MapRunEmptyBodyException
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.5 — 200 OK with empty body triggers a parse
     * error response identifying the affected day.
     */
    @Test
    void emptyBodyMapsToMapRunEmptyBodyException() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        assertThatThrownBy(() -> client.fetchDay("Test Event", 1))
                .isInstanceOf(MapRunEmptyBodyException.class)
                .satisfies(ex -> assertThat(((MapRunEmptyBodyException) ex).getDay()).isEqualTo(1));
    }

    // -------------------------------------------------------------------------
    // 6. Malformed JSON maps to MapRunParseException
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.5 — body that is not valid JSON at all triggers
     * a parse error identifying the affected day.
     */
    @Test
    void malformedJsonMapsToMapRunParseException() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("this is not json at all { broken }")));

        assertThatThrownBy(() -> client.fetchDay("Test Event", 1))
                .isInstanceOf(MapRunParseException.class)
                .satisfies(ex -> assertThat(((MapRunParseException) ex).getDay()).isEqualTo(1));
    }

    // -------------------------------------------------------------------------
    // 7. JSON missing results array maps to MapRunMissingFieldsException
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.8 — 200 OK response with valid JSON that is
     * missing the required {@code results} array triggers a missing-fields
     * error identifying the affected day.
     */
    @Test
    void missingResultsArrayMapsToMapRunMissingFieldsException() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "errorFlag": false,
                                  "statusMessage": "",
                                  "warningFlag": false,
                                  "warningMessage": ""
                                }
                                """)));

        assertThatThrownBy(() -> client.fetchDay("Test Event", 1))
                .isInstanceOf(MapRunMissingFieldsException.class)
                .satisfies(ex -> assertThat(((MapRunMissingFieldsException) ex).getDay()).isEqualTo(1));
    }

    // -------------------------------------------------------------------------
    // 8. Participant entry missing Surname maps to MapRunMissingFieldsException
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.8 — a participant entry missing the required
     * {@code Surname} field triggers a missing-fields error for that day.
     */
    @Test
    void participantMissingSurnameMapsToMapRunMissingFieldsException() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "errorFlag": false,
                                  "statusMessage": "",
                                  "warningFlag": false,
                                  "warningMessage": "",
                                  "results": [
                                    {
                                      "Firstname": "John",
                                      "GrossScore": 100,
                                      "NetScore": 100,
                                      "punchControlIds": ["10", "20"]
                                    }
                                  ]
                                }
                                """)));

        assertThatThrownBy(() -> client.fetchDay("Test Event", 1))
                .isInstanceOf(MapRunMissingFieldsException.class)
                .satisfies(ex -> assertThat(((MapRunMissingFieldsException) ex).getDay()).isEqualTo(1));
    }
}
