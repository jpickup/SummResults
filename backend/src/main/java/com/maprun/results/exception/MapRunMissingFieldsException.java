package com.maprun.results.exception;

/**
 * Thrown when the MapRun API returns well-formed JSON for a given day
 * but one or more required fields (e.g. {@code ResultsList}, {@code Name},
 * {@code GrossScore}, {@code Penalty}, or {@code ScoreControls}) are
 * absent from the response.
 */
public class MapRunMissingFieldsException extends MapRunClientException {

    public MapRunMissingFieldsException(int day, String message) {
        super(day, message);
    }
}
