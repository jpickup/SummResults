package com.maprun.results.web;

import com.maprun.results.model.TeamResult;
import com.maprun.results.service.ResultsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the processed results endpoint.
 *
 * <p>Handles {@code GET /api/results?eventId={id}}.
 * The {@code eventId} must match a named event configured under {@code app.events}.
 * Missing parameters are caught by Spring MVC and delegated to
 * {@link GlobalExceptionHandler} which returns a 400 response.
 * Blank (present-but-empty) parameters are validated here before
 * delegating to {@link ResultsService}.</p>
 */
@RestController
public class ResultsController {

    private static final Logger logger = LoggerFactory.getLogger(ResultsController.class);

    private final ResultsService resultsService;

    public ResultsController(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    /**
     * Returns a JSON array of {@link TeamResult} objects for the given named event,
     * sorted descending by total score. Participants not assigned to any team appear
     * as solo entries.
     *
     * @param eventId the stable named-event ID (e.g. {@code "SUMM-2026"}), required and non-blank
     * @return 200 with the sorted results list, 400 if {@code eventId} is blank, 404 if unknown
     */
    @GetMapping("/api/results")
    public ResponseEntity<?> getResults(@RequestParam String eventId) {
        logger.info("GET /api/results?eventId={}", eventId);

        if (eventId.isBlank()) {
            logger.info("GET /api/results → 400, eventId is blank");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query parameter 'eventId' must not be blank."));
        }

        List<TeamResult> results = resultsService.getResults(eventId);
        logger.info("GET /api/results?eventId={} → 200, {} team result(s)", eventId, results.size());
        return ResponseEntity.ok(results);
    }
}
