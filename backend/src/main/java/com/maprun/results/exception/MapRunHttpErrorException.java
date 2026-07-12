package com.maprun.results.exception;

/**
 * Thrown when the MapRun API returns an HTTP error status (4xx / 5xx)
 * for a given day's fetch.
 */
public class MapRunHttpErrorException extends MapRunClientException {

    private final int httpStatus;

    public MapRunHttpErrorException(int day, int httpStatus, String message) {
        super(day, message);
        this.httpStatus = httpStatus;
    }

    /** Returns the upstream HTTP status code that triggered this exception. */
    public int getHttpStatus() {
        return httpStatus;
    }
}
