package com.maprun.results.scoring;

import com.maprun.results.model.ControlVisit;
import com.maprun.results.model.DayResult;
import com.maprun.results.model.ParticipantResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringEngineTest {

    private ScoringEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ScoringEngine();
    }

    /**
     * Participant on both days with some controls appearing on both days.
     * Verifies: deduction == sum of shared control points,
     *           day2NetScore == gross - deduction - penalty,
     *           totalScore == day1NetScore + day2NetScore.
     */
    @Test
    void bothDaysWithOverlappingControls() {
        // Day 1: controls 101 (30pts), 102 (20pts), 103 (10pts)  gross=60, penalty=5
        DayResult day1 = new DayResult(
                "Alice Smith",
                List.of(
                        new ControlVisit("101", 30),
                        new ControlVisit("102", 20),
                        new ControlVisit("103", 10)
                ),
                60, 5
        );

        // Day 2: controls 102 (20pts) [overlap], 104 (15pts) [new], 105 (25pts) [new]  gross=60, penalty=10
        DayResult day2 = new DayResult(
                "Alice Smith",
                List.of(
                        new ControlVisit("102", 20),
                        new ControlVisit("104", 15),
                        new ControlVisit("105", 25)
                ),
                60, 10
        );

        List<ParticipantResult> results = engine.calculate(List.of(day1), List.of(day2));

        assertThat(results).hasSize(1);
        ParticipantResult r = results.get(0);

        // Day 1
        assertThat(r.day1GrossScore()).isEqualTo(60);
        assertThat(r.day1Penalty()).isEqualTo(5);
        assertThat(r.day1NetScore()).isEqualTo(55); // 60 - 5

        // Day 2 deduction = points of overlapping control 102 = 20
        assertThat(r.day2Deduction()).isEqualTo(20);
        assertThat(r.day2GrossScore()).isEqualTo(60);
        assertThat(r.day2Penalty()).isEqualTo(10);
        assertThat(r.day2NetScore()).isEqualTo(30); // 60 - 20 - 10

        // Total
        assertThat(r.totalScore()).isEqualTo(85); // 55 + 30
    }

    /**
     * Participant appears in day1Results only.
     * Verifies all day2 fields are zero.
     */
    @Test
    void day1OnlyParticipant() {
        DayResult day1 = new DayResult(
                "Bob Jones",
                List.of(
                        new ControlVisit("201", 40),
                        new ControlVisit("202", 30)
                ),
                70, 0
        );

        List<ParticipantResult> results = engine.calculate(List.of(day1), List.of());

        assertThat(results).hasSize(1);
        ParticipantResult r = results.get(0);

        assertThat(r.day1GrossScore()).isEqualTo(70);
        assertThat(r.day1Penalty()).isEqualTo(0);
        assertThat(r.day1NetScore()).isEqualTo(70);

        assertThat(r.day2GrossScore()).isEqualTo(0);
        assertThat(r.day2Penalty()).isEqualTo(0);
        assertThat(r.day2Deduction()).isEqualTo(0);
        assertThat(r.day2NetScore()).isEqualTo(0);

        assertThat(r.totalScore()).isEqualTo(70);
    }

    /**
     * Participant appears in day2Results only.
     * Verifies all day1 fields are zero and day2Deduction is zero
     * (no Day 1 controls to overlap against).
     */
    @Test
    void day2OnlyParticipant() {
        DayResult day2 = new DayResult(
                "Carol White",
                List.of(
                        new ControlVisit("301", 50),
                        new ControlVisit("302", 25)
                ),
                75, 5
        );

        List<ParticipantResult> results = engine.calculate(List.of(), List.of(day2));

        assertThat(results).hasSize(1);
        ParticipantResult r = results.get(0);

        // Day 1 fields all zero
        assertThat(r.day1GrossScore()).isEqualTo(0);
        assertThat(r.day1Penalty()).isEqualTo(0);
        assertThat(r.day1NetScore()).isEqualTo(0);

        // Day 2 deduction is zero — no Day 1 controls to overlap
        assertThat(r.day2Deduction()).isEqualTo(0);
        assertThat(r.day2GrossScore()).isEqualTo(75);
        assertThat(r.day2Penalty()).isEqualTo(5);
        assertThat(r.day2NetScore()).isEqualTo(70); // 75 - 0 - 5

        assertThat(r.totalScore()).isEqualTo(70);
    }

    /**
     * All Day 2 controls have IDs present in Day 1.
     * Verifies deduction equals the full Day 2 gross score's worth of duplicate points,
     * and day2NetScore can be negative when the penalty pushes it below zero.
     */
    @Test
    void allControlsOverlap() {
        // Day 1: controls 401 (50pts), 402 (30pts)  gross=80, penalty=0
        DayResult day1 = new DayResult(
                "Dave Brown",
                List.of(
                        new ControlVisit("401", 50),
                        new ControlVisit("402", 30)
                ),
                80, 0
        );

        // Day 2: same controls 401 (50pts), 402 (30pts)  gross=80, penalty=10
        DayResult day2 = new DayResult(
                "Dave Brown",
                List.of(
                        new ControlVisit("401", 50),
                        new ControlVisit("402", 30)
                ),
                80, 10
        );

        List<ParticipantResult> results = engine.calculate(List.of(day1), List.of(day2));

        assertThat(results).hasSize(1);
        ParticipantResult r = results.get(0);

        // Deduction = all of Day 2's control points = 80
        assertThat(r.day2Deduction()).isEqualTo(80);

        // day2NetScore = 80 - 80 - 10 = -10 (can be negative)
        assertThat(r.day2NetScore()).isEqualTo(-10);

        // totalScore = (80 - 0) + (-10) = 70
        assertThat(r.totalScore()).isEqualTo(70);
    }

    /**
     * Day 2 controls all have IDs absent from Day 1.
     * Verifies day2Deduction == 0.
     */
    @Test
    void noControlsOverlap() {
        // Day 1: controls 501, 502
        DayResult day1 = new DayResult(
                "Eve Green",
                List.of(
                        new ControlVisit("501", 20),
                        new ControlVisit("502", 30)
                ),
                50, 0
        );

        // Day 2: completely different controls 601, 602
        DayResult day2 = new DayResult(
                "Eve Green",
                List.of(
                        new ControlVisit("601", 25),
                        new ControlVisit("602", 35)
                ),
                60, 0
        );

        List<ParticipantResult> results = engine.calculate(List.of(day1), List.of(day2));

        assertThat(results).hasSize(1);
        ParticipantResult r = results.get(0);

        assertThat(r.day2Deduction()).isEqualTo(0);
        assertThat(r.day2NetScore()).isEqualTo(60); // 60 - 0 - 0
        assertThat(r.totalScore()).isEqualTo(110);  // 50 + 60
    }

    /**
     * Two participants with the same totalScore.
     * Verifies they are sorted ascending by last name.
     */
    @Test
    void tieBrakingSortByLastName() {
        // Both participants will have totalScore = 100
        // "Frank Zorro" last name "Zorro", "Grace Able" last name "Able"
        // After sort: Able before Zorro
        DayResult zorro = new DayResult(
                "Frank Zorro",
                List.of(new ControlVisit("701", 100)),
                100, 0
        );
        DayResult able = new DayResult(
                "Grace Able",
                List.of(new ControlVisit("801", 100)),
                100, 0
        );

        // Pass in Zorro first so we can verify sort reorders them
        List<ParticipantResult> results = engine.calculate(
                List.of(zorro, able),
                List.of()
        );

        assertThat(results).hasSize(2);
        assertThat(results.get(0).participantName()).isEqualTo("Grace Able");
        assertThat(results.get(1).participantName()).isEqualTo("Frank Zorro");
        assertThat(results.get(0).totalScore()).isEqualTo(results.get(1).totalScore());
    }
}
