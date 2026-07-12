package com.maprun.results.exception;

/**
 * Thrown when the MapRun API returns a 200 OK response whose body is
 * empty (or blank), making it impossible to parse results for the
 * given day. This typically indicates an invalid or unrecognised
 * event ID.
 */
public class MapRunEmptyBodyException extends MapRunClientException {

    public MapRunEmptyBodyException(int day, String message) {
        super(day, message);
    }
}
