package com.maprun.results.model;

/**
 * Domain type representing a single control visit with its scored point value.
 * Used in both DayResult (input) and ParticipantResult (output).
 */
public record ControlVisit(String controlId, int points, boolean isDuplicate) {
    public ControlVisit(String number, int points) {
        this(number, points, false);
    }

    public String controlDescription() {
        return isDuplicate ? controlId + " (extra)" : controlId;
    }
}
