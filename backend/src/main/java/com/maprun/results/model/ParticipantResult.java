package com.maprun.results.model;

import java.util.List;

/**
 * Processed result for a single participant across both days.
 * This is the type serialised into the /api/results JSON response array.
 */
public record ParticipantResult(
        String participantName,
        List<ControlVisit> day1Controls,
        int day1GrossScore,
        int day1Penalty,
        int day1NetScore,
        List<ControlVisit> day2Controls,
        int day2GrossScore,
        int day2Penalty,
        int day2Deduction,
        int day2NetScore,
        int totalScore
) {}
