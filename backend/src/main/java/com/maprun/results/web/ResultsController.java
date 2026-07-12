package com.maprun.results.web;

import com.maprun.results.model.ParticipantResult;
import com.maprun.results.service.ResultsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the processed results endpoint.
 *
 * <p>Handles {@code GET /api/results?day1EventId={id}&day2EventId={id}}.
 * Missing parameters are caught by Spring MVC and delegated to
 * {@link GlobalExceptionHandler} which returns a 400 response.
 * Blank (present-but-empty) parameters are validated here before
 * delegating to {@link ResultsService}.</p>
 *
 * <p>Requirements 4.1, 4.2, 4.3.</p>
 */
@RestController
public class ResultsController {

    private final ResultsService resultsService;

    public ResultsController(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    /**
     * Returns a JSON array of processed {@link ParticipantResult} objects for the
     * given two-day event.
     *
     * @param day1EventId the MapRun event ID for Day 1 (required, non-blank)
     * @param day2EventId the MapRun event ID for Day 2 (required, non-blank)
     * @return 200 with the sorted results list, or 400 if either ID is blank
     */
    @GetMapping("/api/results")
    public ResponseEntity<?> getResults(
            @RequestParam String day1EventId,
            @RequestParam String day2EventId) {

        if (day1EventId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query parameter 'day1EventId' must not be blank."));
        }
        if (day2EventId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query parameter 'day2EventId' must not be blank."));
        }

        List<ParticipantResult> results = resultsService.getResults(day1EventId, day2EventId);
        return ResponseEntity.ok(results);
    }
}
