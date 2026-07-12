package com.maprun.results.model;

/**
 * A user-friendly named event that maps to two underlying MapRun event IDs.
 *
 * <p>Populated from application properties via {@link com.maprun.results.config.EventsConfig}
 * and exposed through {@code GET /api/events}.</p>
 *
 * @param id           a stable identifier for this event, e.g. {@code "SUMM-2026"}
 * @param name         a human-readable label, e.g. {@code "The SUMM, 2026"}
 * @param day1EventId  the MapRun event ID for Day 1
 * @param day2EventId  the MapRun event ID for Day 2
 */
public record NamedEvent(
        String id,
        String name,
        String day1EventId,
        String day2EventId
) {}
