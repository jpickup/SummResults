package com.maprun.results.web;

import com.maprun.results.client.MapRunApiClient;
import com.maprun.results.exception.MapRunEmptyBodyException;
import com.maprun.results.exception.MapRunHttpErrorException;
import com.maprun.results.model.TeamResult;
import com.maprun.results.service.ResultsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice tests for {@link ResultsController}.
 *
 * <p>Validates endpoint existence, missing/blank parameter handling (400),
 * empty-body upstream error (400), upstream HTTP error (502), and
 * successful 200 response shape.</p>
 */
@WebMvcTest(ResultsController.class)
@Import(GlobalExceptionHandler.class)
class ResultsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResultsService resultsService;

    // Prevent the @WebMvcTest slice from trying to instantiate MapRunApiClient,
    // which requires RestClient.Builder — not available in the MVC test slice.
    @MockBean
    @SuppressWarnings("unused")
    private MapRunApiClient mapRunApiClient;

    // -----------------------------------------------------------------------
    // Missing query parameter → 400
    // -----------------------------------------------------------------------

    @Test
    void returns400WhenEventIdAbsent() throws Exception {
        mockMvc.perform(get("/api/results"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returns400WhenEventIdBlank() throws Exception {
        mockMvc.perform(get("/api/results").param("eventId", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // -----------------------------------------------------------------------
    // Empty body from upstream → 400
    // -----------------------------------------------------------------------

    @Test
    void returns400WhenUpstreamReturnsEmptyBody() throws Exception {
        when(resultsService.getResults(anyString()))
                .thenThrow(new MapRunEmptyBodyException(1, "empty"));

        mockMvc.perform(get("/api/results").param("eventId", "SUMM-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // -----------------------------------------------------------------------
    // Upstream HTTP error → 502
    // -----------------------------------------------------------------------

    @Test
    void returns502ForUpstreamHttpError() throws Exception {
        when(resultsService.getResults(anyString()))
                .thenThrow(new MapRunHttpErrorException(1, 503, "error"));

        mockMvc.perform(get("/api/results").param("eventId", "SUMM-2026"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").exists());
    }

    // -----------------------------------------------------------------------
    // Successful request → 200 with correct JSON shape
    // -----------------------------------------------------------------------

    @Test
    void returns200WithCorrectJsonOnSuccess() throws Exception {
        TeamResult team = new TeamResult(
                "Smith & Jones",
                List.of("Smith John", "Jones Alice"),
                "50,51",
                100,
                "40,41",
                80,
                180,
                10,
                198
        );
        when(resultsService.getResults(anyString())).thenReturn(List.of(team));

        mockMvc.perform(get("/api/results").param("eventId", "SUMM-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamName").value("Smith & Jones"))
                .andExpect(jsonPath("$[0].totalScore").value(180));
    }
}
