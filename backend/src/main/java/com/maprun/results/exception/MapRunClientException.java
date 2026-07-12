package com.maprun.results.exception;

/**
 * Abstract base for all exceptions thrown by the MapRun API client.
 * Each subclass carries a {@code day} field (1 or 2) that identifies
 * which day's fetch triggered the error.
 */
public abstract class MapRunClientException extends RuntimeException {

    private final int day;

    protected MapRunClientException(int day, String message) {
        super(message);
        this.day = day;
    }

    protected MapRunClientException(int day, String message, Throwable cause) {
        super(message, cause);
        this.day = day;
    }

    /** Returns the day (1 or 2) that caused this exception. */
    public int getDay() {
        return day;
    }
}
