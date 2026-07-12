package com.maprun.results.model;

import java.util.List;

/**
 * Aggregated result for a team across both days, including handicap-adjusted scores.
 *
 * <h2>Standard scoring</h2>
 * <p>Each day's net score is the best (maximum) net score among the team's members.
 * {@code totalScore} = {@code day1NetScore} + {@code day2NetScore}.
 *
 * <h2>Handicap scoring</h2>
 * <p>A team qualifies if any member is aged 45 or over (using real age).
 * The effective age of a member is their real age (male) or real age + 10 (female).
 * {@code handicapPct} = max(effective ages) - 45, or 0 if the team does not qualify.
 * {@code handicapScore} = round(totalScore × (1 + handicapPct / 100)).
 *
 * @param teamName      user-defined team display name
 * @param members       ordered list of member names (1 or 2 entries)
 * @param day1NetScore  best Day 1 net score among team members
 * @param day2NetScore  best Day 2 net score among team members
 * @param totalScore    day1NetScore + day2NetScore
 * @param handicapPct   percentage bonus (0 if not qualifying)
 * @param handicapScore totalScore increased by handicapPct percent, rounded to nearest integer
 */
public record TeamResult(
        String teamName,
        List<String> members,
        int day1NetScore,
        int day2NetScore,
        int totalScore,
        int handicapPct,
        int handicapScore
) {}
