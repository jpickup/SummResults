package com.maprun.results.service;

import com.maprun.results.client.MapRunApiClient;
import com.maprun.results.config.EventsConfig;
import com.maprun.results.model.*;
import com.maprun.results.scoring.HandicapEngine;
import com.maprun.results.scoring.ScoringEngine;
import com.maprun.results.teams.TeamsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates fetching raw day results, scoring them, folding per-participant
 * scores into per-team results, and applying handicap adjustments.
 *
 * <h2>Standard scoring</h2>
 * <p>Each day's team net score is the best (maximum) net score among the team's
 * members. Total = day1Net + day2Net.
 *
 * <h2>Handicap scoring</h2>
 * <p>See {@link HandicapEngine} for the full rules.
 *
 * <h2>Unregistered participants</h2>
 * <p>Any participant not assigned to a team appears as a solo entry.
 * Solo entries receive a handicap of 0 (no age/gender data available).
 */
@Service
public class ResultsService {

    private final MapRunApiClient mapRunApiClient;
    private final ScoringEngine scoringEngine;
    private final HandicapEngine handicapEngine;
    private final EventsConfig eventsConfig;
    private final TeamsRepository teamsRepository;

    public ResultsService(MapRunApiClient mapRunApiClient,
                          ScoringEngine scoringEngine,
                          HandicapEngine handicapEngine,
                          EventsConfig eventsConfig,
                          TeamsRepository teamsRepository) {
        this.mapRunApiClient = mapRunApiClient;
        this.scoringEngine = scoringEngine;
        this.handicapEngine = handicapEngine;
        this.eventsConfig = eventsConfig;
        this.teamsRepository = teamsRepository;
    }

    /**
     * Fetches and scores results for the named event, folds them into team results,
     * applies handicap adjustments, and returns the list sorted descending by
     * handicap score (teams with equal handicap scores are ordered by team name).
     *
     * @param eventId the stable named-event ID, e.g. {@code "SUMM-2026"}
     * @return sorted list of {@link TeamResult}
     * @throws ResponseStatusException 404 if {@code eventId} is not found in config
     */
    public List<TeamResult> getResults(String eventId) {
        EventsConfig.EventEntry event = eventsConfig.getEvents().stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found: " + eventId));

        List<DayResult> day1Results = mapRunApiClient.fetchDay(event.getDay1EventId(), 1);
        List<DayResult> day2Results = mapRunApiClient.fetchDay(event.getDay2EventId(), 2);

        List<ParticipantResult> individual = scoringEngine.calculate(day1Results, day2Results);

        Map<String, ParticipantResult> byName = individual.stream()
                .collect(Collectors.toMap(ParticipantResult::participantName, r -> r));

        List<Team> teams = teamsRepository.findAll();
        Set<String> claimed = new HashSet<>();
        List<TeamResult> teamResults = new ArrayList<>();

        for (Team team : teams) {
            List<String> members = membersOf(team);

            List<ParticipantResult> memberResults = members.stream()
                    .filter(byName::containsKey)
                    .map(byName::get)
                    .toList();

            if (memberResults.isEmpty()) {
                continue;
            }

            String day1Controls = memberResults.stream().findFirst()
                    .map(r -> toControlList(r.day1Controls()))
                    .orElse("");

            int day1Net = memberResults.stream()
                    .mapToInt(ParticipantResult::day1NetScore)
                    .max()
                    .orElse(0);

            String day2Controls = memberResults.stream().findFirst()
                    .map(r -> toControlList(r.day2Controls()))
                    .orElse("");

            int day2Net = memberResults.stream()
                    .mapToInt(ParticipantResult::day2NetScore)
                    .max()
                    .orElse(0);

            int total = day1Net + day2Net;
            int pct = handicapEngine.handicapPct(team);
            int hScore = handicapEngine.handicapScore(total, pct);

            teamResults.add(new TeamResult(team.teamName(), members,
                    day1Controls, day1Net,
                    day2Controls, day2Net,
                    total, pct, hScore));
            claimed.addAll(members);
        }

        // Unregistered participants appear as solo entries with no handicap.
        for (ParticipantResult p : individual) {
            if (!claimed.contains(p.participantName())) {
                teamResults.add(new TeamResult(
                        p.participantName(),
                        List.of(p.participantName()),
                        toControlList(p.day1Controls()),
                        p.day1NetScore(),
                        toControlList(p.day2Controls()),
                        p.day2NetScore(),
                        p.totalScore(),
                        0,
                        p.totalScore()));
            }
        }

        // Sort descending by handicapScore, then ascending by teamName on tie.
        teamResults.sort(
                Comparator.comparingInt(TeamResult::handicapScore).reversed()
                        .thenComparing(TeamResult::teamName));

        return teamResults;
    }

    private String toControlList(List<ControlVisit> controls) {
        return String.join(",", controls.stream().map(ControlVisit::controlId).toList());
    }

    private static List<String> membersOf(Team team) {
        if (team.member2() == null || team.member2().isBlank()) {
            return List.of(team.member1());
        }
        return List.of(team.member1(), team.member2());
    }
}
