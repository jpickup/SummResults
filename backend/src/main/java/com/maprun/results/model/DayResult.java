package com.maprun.results.model;

import java.util.List;

/**
 * Domain type holding the parsed results for a single participant on a single day.
 * Produced by MapRunApiClient and consumed by ScoringEngine.
 */
public record DayResult(
        String participantName,
        List<ControlVisit> controls,
        int grossScore,
        int penalty
) {}
