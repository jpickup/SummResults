package com.maprun.results.model;

import java.util.List;

/**
 * Aggregated result for a team across both days.
 *
 * <p>Scoring rule: each day's net score is the <em>best</em> (maximum) net score
 * among the team's members for that day. Total = day1NetScore + day2NetScore.
 *
 * <p>For solo entries ({@code members} has one element) the single member's
 * scores are used directly.
 *
 * @param teamName     user-defined team display name
 * @param members      ordered list of member names (1 or 2 entries)
 * @param day1NetScore best Day 1 net score among team members
 * @param day2NetScore best Day 2 net score among team members
 * @param totalScore   day1NetScore + day2NetScore
 */
public record TeamResult(
        String teamName,
        List<String> members,
        int day1NetScore,
        int day2NetScore,
        int totalScore
) {}
