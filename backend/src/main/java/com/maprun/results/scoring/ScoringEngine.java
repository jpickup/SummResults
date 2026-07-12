package com.maprun.results.scoring;

import com.maprun.results.model.ControlVisit;
import com.maprun.results.model.DayResult;
import com.maprun.results.model.ParticipantResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure stateless scoring engine that combines Day 1 and Day 2 results into a
 * sorted list of {@link ParticipantResult} objects.
 *
 * <p>Deduplication logic: any control visited on Day 2 whose ID already appears
 * in the participant's Day 1 control list is excluded from the Day 2 net score
 * (the sum of those duplicate control point values is the {@code day2Deduction}).
 *
 * <p>Sort order: descending by {@code totalScore}, then ascending by last name
 * (last whitespace-separated word of {@code participantName}) on a tie.
 */
@Component
public class ScoringEngine {

    /**
     * Calculates combined results for all participants across both days.
     *
     * @param day1Results list of per-participant raw results for Day 1 (may be empty, not null)
     * @param day2Results list of per-participant raw results for Day 2 (may be empty, not null)
     * @return sorted list of {@link ParticipantResult}, one entry per unique participant
     */
    public List<ParticipantResult> calculate(
            List<DayResult> day1Results,
            List<DayResult> day2Results) {

        // Index both day lists by participant name for O(1) lookup
        Map<String, DayResult> day1Map = day1Results.stream()
                .collect(Collectors.toMap(DayResult::participantName, r -> r));
        Map<String, DayResult> day2Map = day2Results.stream()
                .collect(Collectors.toMap(DayResult::participantName, r -> r));

        // Union of all participant names across both days (preserving encounter order)
        Set<String> allNames = new LinkedHashSet<>();
        day1Results.forEach(r -> allNames.add(r.participantName()));
        day2Results.forEach(r -> allNames.add(r.participantName()));

        List<ParticipantResult> results = new ArrayList<>(allNames.size());

        for (String name : allNames) {
            Optional<DayResult> d1 = Optional.ofNullable(day1Map.get(name));
            Optional<DayResult> d2 = Optional.ofNullable(day2Map.get(name));

            // --- Day 1 fields ---
            List<ControlVisit> day1Controls = d1.map(DayResult::controls).orElse(List.of());
            int day1GrossScore = d1.map(DayResult::grossScore).orElse(0);
            int day1Penalty    = d1.map(DayResult::penalty).orElse(0);
            int day1NetScore   = day1GrossScore - day1Penalty;

            // Build the set of Day 1 control IDs used for deduplication
            Set<String> day1ControlIds = day1Controls.stream()
                    .map(ControlVisit::controlId)
                    .collect(Collectors.toSet());

            // --- Day 2 fields ---
            List<ControlVisit> day2Controls = d2.map(DayResult::controls).orElse(List.of());
            int day2GrossScore = d2.map(DayResult::grossScore).orElse(0);
            int day2Penalty    = d2.map(DayResult::penalty).orElse(0);

            // Deduction = sum of points for Day 2 controls whose IDs appear in Day 1
            int day2Deduction = day2Controls.stream()
                    .filter(cv -> day1ControlIds.contains(cv.controlId()))
                    .mapToInt(ControlVisit::points)
                    .sum();

            int day2NetScore = day2GrossScore - day2Deduction - day2Penalty;

            int totalScore = day1NetScore + day2NetScore;

            results.add(new ParticipantResult(
                    name,
                    day1Controls,
                    day1GrossScore,
                    day1Penalty,
                    day1NetScore,
                    day2Controls,
                    day2GrossScore,
                    day2Penalty,
                    day2Deduction,
                    day2NetScore,
                    totalScore
            ));
        }

        // Sort: descending totalScore, then ascending last name on tie
        results.sort(
                Comparator.comparingInt(ParticipantResult::totalScore).reversed()
                        .thenComparing(r -> lastName(r.participantName()))
        );

        return results;
    }

    /**
     * Extracts the last whitespace-separated word from a participant name,
     * used as the secondary sort key.
     */
    private static String lastName(String participantName) {
        if (participantName == null || participantName.isBlank()) {
            return "";
        }
        String trimmed = participantName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        return lastSpace < 0 ? trimmed : trimmed.substring(lastSpace + 1);
    }
}
