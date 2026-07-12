package com.maprun.results.model;

/**
 * Domain type representing a single control visit with its scored point value.
 * Used in both DayResult (input) and ParticipantResult (output).
 */
public record ControlVisit(String controlId, int points) {}
