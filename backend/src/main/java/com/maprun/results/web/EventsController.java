package com.maprun.results.web;

import com.maprun.results.config.EventsConfig;
import com.maprun.results.model.NamedEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller that exposes the configured list of named events.
 *
 * <p>{@code GET /api/events} returns a JSON array of objects containing
 * only the public fields {@code id} and {@code name}. The underlying
 * MapRun event IDs are internal configuration details and are not
 * included in the response.</p>
 */
@RestController
public class EventsController {

    private final EventsConfig eventsConfig;

    public EventsController(EventsConfig eventsConfig) {
        this.eventsConfig = eventsConfig;
    }

    /**
     * Returns the list of named events as {@link NamedEvent} records
     * (id and name only — day IDs are omitted from the public API).
     *
     * @return 200 with a JSON array; empty array when no events are configured
     */
    @GetMapping("/api/events")
    public ResponseEntity<List<EventSummary>> getEvents() {
        List<EventSummary> summaries = eventsConfig.getEvents().stream()
                .map(e -> new EventSummary(e.getId(), e.getName()))
                .toList();
        return ResponseEntity.ok(summaries);
    }

    /**
     * Public projection returned by {@code GET /api/events}.
     * Intentionally omits the raw MapRun day event IDs.
     *
     * @param id   stable event identifier, e.g. {@code "SUMM-2026"}
     * @param name human-readable label, e.g. {@code "The SUMM, 2026"}
     */
    public record EventSummary(String id, String name) {}
}
