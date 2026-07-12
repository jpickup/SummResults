package com.maprun.results.web;

import com.maprun.results.exception.MapRunEmptyBodyException;
import com.maprun.results.exception.MapRunHttpErrorException;
import com.maprun.results.exception.MapRunMissingFieldsException;
import com.maprun.results.exception.MapRunParseException;
import com.maprun.results.exception.MapRunTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Centralized exception handler that maps domain and Spring MVC exceptions to
 * structured JSON error responses of the form {@code { "error": "..." }}.
 *
 * <p>Requirements 4.3, 4.4, 4.5.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles a missing required query parameter (e.g. day1EventId or day2EventId).
     * Returns 400 with the parameter name in the error message.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(
            MissingServletRequestParameterException ex) {

        String message = "Required query parameter '" + ex.getParameterName() + "' is missing.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    /**
     * Handles an empty or blank body returned by the MapRun API, which typically
     * indicates an invalid or unrecognised event ID.
     * Returns 400 with the exception's own message (which already identifies the day).
     */
    @ExceptionHandler(MapRunEmptyBodyException.class)
    public ResponseEntity<Map<String, String>> handleEmptyBody(
            MapRunEmptyBodyException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles an upstream HTTP error status (4xx / 5xx) from the MapRun API.
     * Returns 502 with a descriptive message including the upstream status and day.
     */
    @ExceptionHandler(MapRunHttpErrorException.class)
    public ResponseEntity<Map<String, String>> handleHttpError(
            MapRunHttpErrorException ex) {

        String message = "MapRun API returned HTTP " + ex.getHttpStatus()
                + " for day " + ex.getDay() + ".";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", message));
    }

    /**
     * Handles a connection or read timeout when contacting the MapRun API.
     * Returns 502 with a descriptive message identifying the affected day.
     */
    @ExceptionHandler(MapRunTimeoutException.class)
    public ResponseEntity<Map<String, String>> handleTimeout(
            MapRunTimeoutException ex) {

        String message = "MapRun API request timed out for day " + ex.getDay() + ".";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", message));
    }

    /**
     * Handles a JSON parse failure on a non-empty body from the MapRun API.
     * Returns 502 with a descriptive message identifying the affected day.
     */
    @ExceptionHandler(MapRunParseException.class)
    public ResponseEntity<Map<String, String>> handleParseError(
            MapRunParseException ex) {

        String message = "Failed to parse MapRun API response for day " + ex.getDay() + ".";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", message));
    }

    /**
     * Handles a well-formed JSON response from the MapRun API that is missing
     * one or more required fields.
     * Returns 502 with a descriptive message identifying the affected day.
     */
    @ExceptionHandler(MapRunMissingFieldsException.class)
    public ResponseEntity<Map<String, String>> handleMissingFields(
            MapRunMissingFieldsException ex) {

        String message = "MapRun API response for day " + ex.getDay()
                + " is missing required fields.";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", message));
    }
}
