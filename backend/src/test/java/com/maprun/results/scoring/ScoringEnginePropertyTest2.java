package com.maprun.results.scoring;

import com.maprun.results.config.EventsConfig;
import com.maprun.results.model.ControlVisit;
import com.maprun.results.model.DayResult;
import com.maprun.results.model.ParticipantResult;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Positive;
import net.jqwik.api.constraints.Size;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link ScoringEngine}, covering Properties 5–8.
 *
 * <p>Each property runs 100 generated samples (jqwik default with {@code @Property(tries=100)}).
 */
class ScoringEnginePropertyTest2 {

    private final ScoringEngine engine = new ScoringEngine();
    private final EventsConfig.EventEntry event = new EventsConfig.EventEntry();

    // -------------------------------------------------------------------------
    // Property 5: Results are sorted by total score descending, then last name ascending
    // Validates: Requirements 3.4, 3.5
    // -------------------------------------------------------------------------

    /**
     * Property 5: {@code resultsAreSortedCorrectly}
     *
     * <p>For any non-empty list of participants, the engine's output satisfies the
     * sort invariant at every adjacent pair: descending {@code totalScore}, then
     * ascending last name on a tie.
     *
     * <p><b>Validates: Requirements 3.4, 3.5</b>
     */
    @Property(tries = 100)
    void resultsAreSortedCorrectly(
            @ForAll("participantDay1Results") @Size(min = 1, max = 10) List<DayResult> day1Results,
            @ForAll("participantDay2Results") @Size(min = 0, max = 5)  List<DayResult> day2Results) {

        List<ParticipantResult> results = engine.calculate(event, day1Results, day2Results);

        for (int i = 0; i < results.size() - 1; i++) {
            ParticipantResult a = results.get(i);
            ParticipantResult b = results.get(i + 1);

            int scoreA = a.totalScore();
            int scoreB = b.totalScore();

            if (scoreA == scoreB) {
                // Tie: last name of a must be <= last name of b (ascending)
                String lastA = lastName(a.participantName());
                String lastB = lastName(b.participantName());
                assertThat(lastA)
                        .as("On score tie, lastName(%s) should be <= lastName(%s)", a.participantName(), b.participantName())
                        .isLessThanOrEqualTo(lastB);
            } else {
                // Primary sort: descending total score
                assertThat(scoreA)
                        .as("totalScore of result[%d] should be >= totalScore of result[%d]", i, i + 1)
                        .isGreaterThan(scoreB);
            }
        }
    }

    @Provide
    Arbitrary<List<DayResult>> participantDay1Results() {
        return participantDayResults("P");
    }

    @Provide
    Arbitrary<List<DayResult>> participantDay2Results() {
        // Use a different name prefix so day2-only participants can be distinguished
        return participantDayResults("Q");
    }

    private Arbitrary<List<DayResult>> participantDayResults(String namePrefix) {
        Arbitrary<String> firstName = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(8);
        Arbitrary<String> lastName = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(8);
        Arbitrary<String> name = Combinators.combine(firstName, lastName)
                .as((f, l) -> namePrefix + f + " " + l);

        Arbitrary<Integer> gross = Arbitraries.integers().between(0, 500);
        Arbitrary<Integer> penalty = Arbitraries.integers().between(0, 50);

        Arbitrary<DayResult> dayResult = Combinators.combine(name, gross, penalty)
                .as((n, g, p) -> new DayResult(n, List.of(), g, p));

        return dayResult.list().ofMinSize(0).ofMaxSize(10);
    }

    // -------------------------------------------------------------------------
    // Property 6: Absent Day 2 participant has zeroed Day 2 fields
    // Validates: Requirement 2.5
    // -------------------------------------------------------------------------

    /**
     * Property 6: {@code absentDay2ZerosDay2Fields}
     *
     * <p>A participant who appears only in Day 1 data (absent from Day 2) must have
     * {@code day2GrossScore == 0}, {@code day2Deduction == 0}, and {@code day2NetScore == 0}.
     *
     * <p><b>Validates: Requirement 2.5</b>
     */
    @Property(tries = 100)
    void absentDay2ZerosDay2Fields(
            @ForAll @NotBlank String firstName,
            @ForAll @NotBlank String lastName,
            @ForAll("controlList") List<ControlVisit> day1Controls,
            @ForAll @Positive int grossScore,
            @ForAll @Positive int penalty) {

        String participantName = firstName.trim() + " " + lastName.trim();
        DayResult day1Entry = new DayResult(participantName, day1Controls, grossScore, penalty);

        // Day 2 is empty — participant is absent from Day 2
        List<ParticipantResult> results = engine.calculate(event, List.of(day1Entry), List.of());

        ParticipantResult result = results.stream()
                .filter(r -> r.participantName().equals(participantName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Participant not found in results: " + participantName));

        assertThat(result.day2GrossScore())
                .as("day2GrossScore should be 0 for participant absent from Day 2")
                .isEqualTo(0);
        assertThat(result.day2Deduction())
                .as("day2Deduction should be 0 for participant absent from Day 2")
                .isEqualTo(0);
        assertThat(result.day2NetScore())
                .as("day2NetScore should be 0 for participant absent from Day 2")
                .isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Property 7: Absent Day 1 participant has no deduction on Day 2
    // Validates: Requirement 2.6
    // -------------------------------------------------------------------------

    /**
     * Property 7: {@code absentDay1ZeroesDay1AndNoDeduction}
     *
     * <p>A participant who appears only in Day 2 data (absent from Day 1) must have
     * {@code day1GrossScore == 0}, {@code day1NetScore == 0}, and {@code day2Deduction == 0}.
     *
     * <p><b>Validates: Requirement 2.6</b>
     */
    @Property(tries = 100)
    void absentDay1ZeroesDay1AndNoDeduction(
            @ForAll @NotBlank String firstName,
            @ForAll @NotBlank String lastName,
            @ForAll("controlList") List<ControlVisit> day2Controls,
            @ForAll @Positive int grossScore,
            @ForAll @Positive int penalty) {

        String participantName = firstName.trim() + " " + lastName.trim();
        DayResult day2Entry = new DayResult(participantName, day2Controls, grossScore, penalty);

        // Day 1 is empty — participant is absent from Day 1
        List<ParticipantResult> results = engine.calculate(event, List.of(), List.of(day2Entry));

        ParticipantResult result = results.stream()
                .filter(r -> r.participantName().equals(participantName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Participant not found in results: " + participantName));

        assertThat(result.day1GrossScore())
                .as("day1GrossScore should be 0 for participant absent from Day 1")
                .isEqualTo(0);
        assertThat(result.day1NetScore())
                .as("day1NetScore should be 0 for participant absent from Day 1")
                .isEqualTo(0);
        assertThat(result.day2Deduction())
                .as("day2Deduction should be 0 for participant absent from Day 1 (no Day 1 controls to deduplicate against)")
                .isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Property 8: Controls unique to Day 2 (not in Day 1) incur no deduction
    // Validates: Requirements 2.1, 2.2
    // -------------------------------------------------------------------------

    /**
     * Property 8: {@code uniqueDay2ControlsIncurNoDeduction}
     *
     * <p>When all Day 2 control IDs are guaranteed to differ from all Day 1 control IDs
     * (Day 1 IDs prefixed with "D1-", Day 2 IDs prefixed with "D2-"), the deduction
     * must be zero because there are no duplicates.
     *
     * <p><b>Validates: Requirements 2.1, 2.2</b>
     */
    @Property(tries = 100)
    void uniqueDay2ControlsIncurNoDeduction(
            @ForAll @NotBlank String firstName,
            @ForAll @NotBlank String lastName,
            @ForAll("uniqueDay1Controls") List<ControlVisit> day1Controls,
            @ForAll("uniqueDay2Controls") List<ControlVisit> day2Controls,
            @ForAll @Positive int day1Gross,
            @ForAll @Positive int day2Gross,
            @ForAll @Positive int day1Penalty,
            @ForAll @Positive int day2Penalty) {

        String participantName = firstName.trim() + " " + lastName.trim();
        DayResult day1Entry = new DayResult(participantName, day1Controls, day1Gross, day1Penalty);
        DayResult day2Entry = new DayResult(participantName, day2Controls, day2Gross, day2Penalty);

        List<ParticipantResult> results = engine.calculate(event, List.of(day1Entry), List.of(day2Entry));

        ParticipantResult result = results.stream()
                .filter(r -> r.participantName().equals(participantName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Participant not found in results: " + participantName));

        assertThat(result.day2Deduction())
                .as("day2Deduction should be 0 when all Day 2 control IDs are unique (not present in Day 1)")
                .isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Shared providers
    // -------------------------------------------------------------------------

    /**
     * Generates a list of ControlVisit entries with IDs prefixed "D1-" (guaranteed distinct from D2 controls).
     */
    @Provide
    Arbitrary<List<ControlVisit>> uniqueDay1Controls() {
        return controlId().map(id -> "D1-" + id)
                .flatMap(id -> points().map(p -> new ControlVisit(id, p)))
                .list().ofMinSize(0).ofMaxSize(5);
    }

    /**
     * Generates a list of ControlVisit entries with IDs prefixed "D2-" (guaranteed distinct from D1 controls).
     */
    @Provide
    Arbitrary<List<ControlVisit>> uniqueDay2Controls() {
        return controlId().map(id -> "D2-" + id)
                .flatMap(id -> points().map(p -> new ControlVisit(id, p)))
                .list().ofMinSize(0).ofMaxSize(5);
    }

    /**
     * Generates a general-purpose list of ControlVisit entries (arbitrary IDs).
     */
    @Provide
    Arbitrary<List<ControlVisit>> controlList() {
        return controlId()
                .flatMap(id -> points().map(p -> new ControlVisit(id, p)))
                .list().ofMinSize(0).ofMaxSize(5);
    }

    private Arbitrary<String> controlId() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(4);
    }

    private Arbitrary<Integer> points() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * Extracts the last whitespace-separated word — mirrors the private method in {@link ScoringEngine}.
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
