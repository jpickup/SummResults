package com.maprun.results.exception;

/**
 * Thrown when the connection to, or read from, the MapRun API exceeds
 * the configured timeout threshold for a given day's fetch.
 */
public class MapRunTimeoutException extends MapRunClientException {

    public MapRunTimeoutException(int day, String message) {
        super(day, message);
    }

    public MapRunTimeoutException(int day, String message, Throwable cause) {
        super(day, message, cause);
    }
}
