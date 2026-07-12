package com.maprun.results.scoring;

import com.maprun.results.model.ControlVisit;
import com.maprun.results.model.DayResult;
import com.maprun.results.model.ParticipantResult;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link ScoringEngine} using jqwik.
 *
 * <p>Properties covered:
 * <ul>
 *   <li>3.2 — day2DeductionEqualsIntersectionSum   (Validates: Requirements 2.1, 2.2)</li>
 *   <li>3.3 — day1NetScoreFormula                  (Validates: Requirement 2.4)</li>
 *   <li>3.4 — day2NetScoreFormula                  (Validates: Requirement 2.3)</li>
 *   <li>3.5 — totalScoreIsSumOfNetScores            (Validates: Requirements 3.1, 3.2)</li>
 * </ul>
 */
class ScoringEnginePropertyTest {

    private final ScoringEngine engine = new ScoringEngine();

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates a non-blank participant name that is safe to use as a map key.
     */
    @Provide
    Arbitrary<String> participantNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.isBlank());
    }

    /**
     * Generates a single {@link ControlVisit} with an arbitrary non-blank control ID
     * and a non-negative point value (0–100).
     */
    @Provide
    Arbitrary<ControlVisit> controlVisits() {
        Arbitrary<String> controlId = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(6)
                .filter(s -> !s.isBlank());
        Arbitrary<Integer> points = Arbitraries.integers().between(0, 100);
        return Combinators.combine(controlId, points).as(ControlVisit::new);
    }

    /**
     * Generates a list of up to 10 {@link ControlVisit} objects with
     * <em>unique</em> control IDs (to mirror real MapRun data where a
     * participant visits each control at most once per day).
     */
    @Provide
    Arbitrary<List<ControlVisit>> uniqueControlList() {
        return controlVisits()
                .list()
                .ofMaxSize(10)
                .uniqueElements(ControlVisit::controlId);
    }

    // -------------------------------------------------------------------------
    // Property 1 — day2DeductionEqualsIntersectionSum
    // Validates: Requirements 2.1, 2.2
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirements 2.1, 2.2**
     *
     * <p>For any participant with arbitrary Day 1 and Day 2 control lists, the
     * {@code day2Deduction} computed by {@link ScoringEngine#calculate} SHALL
     * equal the sum of points for Day 2 controls whose IDs also appear in the
     * Day 1 control list.
     */
    @Property(tries = 100)
    void day2DeductionEqualsIntersectionSum(
            @ForAll("participantNames") String name,
            @ForAll("uniqueControlList") List<ControlVisit> day1Controls,
            @ForAll("uniqueControlList") List<ControlVisit> day2Controls,
            @ForAll @IntRange(min = 0, max = 1000) int day1Gross,
            @ForAll @IntRange(min = 0, max = 200) int day1Penalty,
            @ForAll @IntRange(min = 0, max = 1000) int day2Gross,
            @ForAll @IntRange(min = 0, max = 200) int day2Penalty
    ) {
        DayResult day1 = new DayResult(name, day1Controls, day1Gross, day1Penalty);
        DayResult day2 = new DayResult(name, day2Controls, day2Gross, day2Penalty);

        List<ParticipantResult> results = engine.calculate(List.of(day1), List.of(day2));

        ParticipantResult result = results.stream()
                .filter(r -> r.participantName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Participant not found in results"));

        // Expected deduction = sum of points for Day 2 controls whose IDs are in Day 1
        Set<String> day1Ids = day1Controls.stream()
                .map(ControlVisit::controlId)
                .collect(Collectors.toSet());

        int expectedDeduction = day2Controls.stream()
                .filter(cv -> day1Ids.contains(cv.controlId()))
                .mapToInt(ControlVisit::points)
                .sum();

        assertThat(result.day2Deduction()).isEqualTo(expectedDeduction);
    }

    // -------------------------------------------------------------------------
    // Property 2 — day1NetScoreFormula
    // Validates: Requirement 2.4
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirement 2.4**
     *
     * <p>For any participant, {@code day1NetScore} SHALL equal
     * {@code day1GrossScore − day1Penalty}. No deduction is ever applied to Day 1.
     */
    @Property(tries = 100)
    void day1NetScoreFormula(
            @ForAll("participantNames") String name,
            @ForAll @IntRange(min = 0, max = 1000) int grossScore,
            @ForAll @IntRange(min = 0, max = 200) int penalty
    ) {
        DayResult day1 = new DayResult(name, List.of(), grossScore, penalty);

        // No Day 2 entry for this participant
        List<ParticipantResult> results = engine.calculate(List.of(day1), List.of());

        ParticipantResult result = results.stream()
                .filter(r -> r.participantName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Participant not found in results"));

        assertThat(result.day1NetScore()).isEqualTo(grossScore - penalty);
    }

    // -------------------------------------------------------------------------
    // Property 3 — day2NetScoreFormula
    // Validates: Requirement 2.3
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirement 2.3**
     *
     * <p>For any participant, {@code day2NetScore} SHALL equal
     * {@code day2GrossScore − day2Deduction − day2Penalty},
     * where {@code day2Deduction} is the intersection sum of duplicate controls.
     */
    @Property(tries = 100)
    void day2NetScoreFormula(
            @ForAll("participantNames") String name,
            @ForAll("uniqueControlList") List<ControlVisit> day1Controls,
            @ForAll("uniqueControlList") List<ControlVisit> day2Controls,
            @ForAll @IntRange(min = 0, max = 1000) int day1Gross,
            @ForAll @IntRange(min = 0, max = 200) int day1Penalty,
            @ForAll @IntRange(min = 0, max = 1000) int day2Gross,
            @ForAll @IntRange(min = 0, max = 200) int day2Penalty
    ) {
        DayResult day1 = new DayResult(name, day1Controls, day1Gross, day1Penalty);
        DayResult day2 = new DayResult(name, day2Controls, day2Gross, day2Penalty);

        List<ParticipantResult> results = engine.calculate(List.of(day1), List.of(day2));

        ParticipantResult result = results.stream()
                .filter(r -> r.participantName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Participant not found in results"));

        // Compute expected deduction independently
        Set<String> day1Ids = day1Controls.stream()
                .map(ControlVisit::controlId)
                .collect(Collectors.toSet());

        int expectedDeduction = day2Controls.stream()
                .filter(cv -> day1Ids.contains(cv.controlId()))
                .mapToInt(ControlVisit::points)
                .sum();

        int expectedDay2NetScore = day2Gross - expectedDeduction - day2Penalty;

        assertThat(result.day2NetScore()).isEqualTo(expectedDay2NetScore);
    }

    // -------------------------------------------------------------------------
    // Property 4 — totalScoreIsSumOfNetScores
    // Validates: Requirements 3.1, 3.2
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirements 3.1, 3.2**
     *
     * <p>For any participant result, {@code totalScore} SHALL equal
     * {@code day1NetScore + day2NetScore}.
     */
    @Property(tries = 100)
    void totalScoreIsSumOfNetScores(
            @ForAll("participantNames") String name,
            @ForAll("uniqueControlList") List<ControlVisit> day1Controls,
            @ForAll("uniqueControlList") List<ControlVisit> day2Controls,
            @ForAll @IntRange(min = 0, max = 1000) int day1Gross,
            @ForAll @IntRange(min = 0, max = 200) int day1Penalty,
            @ForAll @IntRange(min = 0, max = 1000) int day2Gross,
            @ForAll @IntRange(min = 0, max = 200) int day2Penalty
    ) {
        DayResult day1 = new DayResult(name, day1Controls, day1Gross, day1Penalty);
        DayResult day2 = new DayResult(name, day2Controls, day2Gross, day2Penalty);

        List<ParticipantResult> results = engine.calculate(List.of(day1), List.of(day2));

        ParticipantResult result = results.stream()
                .filter(r -> r.participantName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Participant not found in results"));

        assertThat(result.totalScore())
                .isEqualTo(result.day1NetScore() + result.day2NetScore());
    }
}
