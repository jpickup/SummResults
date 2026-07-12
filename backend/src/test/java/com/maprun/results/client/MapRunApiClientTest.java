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
    // 1. Successful 200 response is parsed into correct DayResult list
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.3 — 200 response parsed into Participant list
     * with Controls, GrossScore, and Penalty for that day.
     */
    @Test
    void successfulResponseIsParsedCorrectly() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "EventName": "Test Event",
                                  "ResultsList": [
                                    {
                                      "Name": "Smith John",
                                      "GrossScore": 420,
                                      "Penalty": 0,
                                      "ScoreControls": [
                                        { "Control": "101", "Points": 30 },
                                        { "Control": "102", "Points": 20 }
                                      ]
                                    }
                                  ]
                                }
                                """)));

        List<DayResult> results = client.fetchDay("Test Event", 1);

        assertThat(results).hasSize(1);
        DayResult result = results.get(0);
        assertThat(result.participantName()).isEqualTo("Smith John");
        assertThat(result.grossScore()).isEqualTo(420);
        assertThat(result.penalty()).isEqualTo(0);
        assertThat(result.controls()).hasSize(2);
        assertThat(result.controls().get(0).controlId()).isEqualTo("101");
        assertThat(result.controls().get(0).points()).isEqualTo(30);
        assertThat(result.controls().get(1).controlId()).isEqualTo("102");
        assertThat(result.controls().get(1).points()).isEqualTo(20);
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
     * Validates: Requirement 1.5 — 200 OK with a body that is valid JSON but
     * does not match the expected schema triggers a parse error for the day.
     *
     * <p>Jackson will deserialise the unknown structure into a
     * {@link com.maprun.results.model.MapRunResultsResponse} with a null
     * {@code ResultsList}, which is then caught as a
     * {@link MapRunMissingFieldsException}; however if the body cannot be
     * deserialised at all (e.g. a plain string or truly malformed JSON) then
     * a {@link MapRunParseException} is thrown. Both are acceptable for
     * Requirement 1.5. This test verifies the "structurally invalid for the
     * schema" path where the body is not even valid JSON.</p>
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
    // 7. JSON missing ResultsList maps to MapRunMissingFieldsException
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirement 1.8 — 200 OK response with valid JSON that is
     * missing the required {@code ResultsList} field triggers a missing-fields
     * error identifying the affected day.
     */
    @Test
    void missingRequiredFieldsMapsToMapRunMissingFieldsException() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "EventName": "Test Event"
                                }
                                """)));

        assertThatThrownBy(() -> client.fetchDay("Test Event", 1))
                .isInstanceOf(MapRunMissingFieldsException.class)
                .satisfies(ex -> assertThat(((MapRunMissingFieldsException) ex).getDay()).isEqualTo(1));
    }
}
