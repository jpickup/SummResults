package com.maprun.results.web;

import com.maprun.results.exception.MapRunEmptyBodyException;
import com.maprun.results.exception.MapRunHttpErrorException;
import com.maprun.results.model.ParticipantResult;
import com.maprun.results.service.ResultsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice tests for {@link ResultsController}.
 *
 * <p>Validates Requirements 4.1–4.5: endpoint existence, missing-parameter
 * handling (400), empty-body upstream error (400), upstream HTTP error (502),
 * and successful 200 response shape.</p>
 */
@WebMvcTest(ResultsController.class)
@Import(GlobalExceptionHandler.class)
class ResultsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResultsService resultsService;

    // -----------------------------------------------------------------------
    // Requirement 4.3 – missing query parameters → 400
    // -----------------------------------------------------------------------

    @Test
    void returns400WhenDay1EventIdAbsent() throws Exception {
        mockMvc.perform(get("/api/results").param("day2EventId", "456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returns400WhenDay2EventIdAbsent() throws Exception {
        mockMvc.perform(get("/api/results").param("day1EventId", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // -----------------------------------------------------------------------
    // Requirement 4.4 – empty body from upstream → 400
    // -----------------------------------------------------------------------

    @Test
    void returns400WhenUpstreamReturnsEmptyBody() throws Exception {
        when(resultsService.getResults(any(), any()))
                .thenThrow(new MapRunEmptyBodyException(1, "empty"));

        mockMvc.perform(get("/api/results")
                        .param("day1EventId", "123")
                        .param("day2EventId", "456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // -----------------------------------------------------------------------
    // Requirement 4.5 – upstream HTTP error → 502
    // -----------------------------------------------------------------------

    @Test
    void returns502ForUpstreamHttpError() throws Exception {
        when(resultsService.getResults(any(), any()))
                .thenThrow(new MapRunHttpErrorException(1, 503, "error"));

        mockMvc.perform(get("/api/results")
                        .param("day1EventId", "123")
                        .param("day2EventId", "456"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").exists());
    }

    // -----------------------------------------------------------------------
    // Requirement 4.1 / 4.2 – successful request → 200 with correct JSON shape
    // -----------------------------------------------------------------------

    @Test
    void returns200WithCorrectJsonOnSuccess() throws Exception {
        ParticipantResult participant = new ParticipantResult(
                "Smith John",
                List.of(),
                100, 0, 100,
                List.of(),
                50, 0, 0, 50,
                150
        );
        when(resultsService.getResults(any(), any())).thenReturn(List.of(participant));

        mockMvc.perform(get("/api/results")
                        .param("day1EventId", "123")
                        .param("day2EventId", "456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participantName").value("Smith John"))
                .andExpect(jsonPath("$[0].totalScore").value(150));
    }
}
