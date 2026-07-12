package com.maprun.results.exception;

/**
 * Thrown when the MapRun API returns a non-empty body that cannot be
 * parsed as valid JSON for a given day's fetch.
 */
public class MapRunParseException extends MapRunClientException {

    public MapRunParseException(int day, String message) {
        super(day, message);
    }

    public MapRunParseException(int day, String message, Throwable cause) {
        super(day, message, cause);
    }
}
