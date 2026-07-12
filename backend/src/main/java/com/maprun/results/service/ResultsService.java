package com.maprun.results.service;

import com.maprun.results.client.MapRunApiClient;
import com.maprun.results.config.EventsConfig;
import com.maprun.results.model.DayResult;
import com.maprun.results.model.ParticipantResult;
import com.maprun.results.model.Team;
import com.maprun.results.model.TeamResult;
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
 * Orchestrates fetching raw day results, scoring them, and folding the
 * per-participant scores into per-team results.
 *
 * <h2>Team scoring rule</h2>
 * <p>Each day's team net score is the <em>best</em> (maximum) net score
 * among the team's members for that day. The team total is the sum of the
 * two day scores.
 *
 * <h2>Unregistered participants</h2>
 * <p>Any participant who is not listed as a member of any configured team
 * appears as a solo entry whose team name equals their participant name.
 */
@Service
public class ResultsService {

    private final MapRunApiClient mapRunApiClient;
    private final ScoringEngine scoringEngine;
    private final EventsConfig eventsConfig;
    private final TeamsRepository teamsRepository;

    public ResultsService(MapRunApiClient mapRunApiClient,
                          ScoringEngine scoringEngine,
                          EventsConfig eventsConfig,
                          TeamsRepository teamsRepository) {
        this.mapRunApiClient = mapRunApiClient;
        this.scoringEngine = scoringEngine;
        this.eventsConfig = eventsConfig;
        this.teamsRepository = teamsRepository;
    }

    /**
     * Fetches and scores results for the named event, then folds them into
     * team results sorted descending by total score.
     *
     * @param eventId the stable named-event ID, e.g. {@code "SUMM-2026"}
     * @return sorted list of {@link TeamResult}, one entry per team (or solo participant)
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

        // Score each participant individually (existing logic).
        List<ParticipantResult> individual = scoringEngine.calculate(day1Results, day2Results);

        // Index by participant name for O(1) lookup.
        Map<String, ParticipantResult> byName = individual.stream()
                .collect(Collectors.toMap(ParticipantResult::participantName, r -> r));

        List<Team> teams = teamsRepository.findAll();

        // Track which participants have been claimed by a team.
        Set<String> claimed = new HashSet<>();
        List<TeamResult> teamResults = new ArrayList<>();

        for (Team team : teams) {
            List<String> members = membersOf(team);

            // Only include the team if at least one member has results.
            List<ParticipantResult> memberResults = members.stream()
                    .filter(byName::containsKey)
                    .map(byName::get)
                    .toList();

            if (memberResults.isEmpty()) {
                continue;
            }

            int day1Net = memberResults.stream()
                    .mapToInt(ParticipantResult::day1NetScore)
                    .max()
                    .orElse(0);

            int day2Net = memberResults.stream()
                    .mapToInt(ParticipantResult::day2NetScore)
                    .max()
                    .orElse(0);

            teamResults.add(new TeamResult(team.teamName(), members, day1Net, day2Net, day1Net + day2Net));
            claimed.addAll(members);
        }

        // Any participant not claimed by a team appears as a solo entry.
        for (ParticipantResult p : individual) {
            if (!claimed.contains(p.participantName())) {
                teamResults.add(new TeamResult(
                        p.participantName(),
                        List.of(p.participantName()),
                        p.day1NetScore(),
                        p.day2NetScore(),
                        p.totalScore()));
            }
        }

        // Sort: descending totalScore, then ascending teamName on tie.
        teamResults.sort(
                Comparator.comparingInt(TeamResult::totalScore).reversed()
                        .thenComparing(TeamResult::teamName));

        return teamResults;
    }

    private static List<String> membersOf(Team team) {
        if (team.member2() == null || team.member2().isBlank()) {
            return List.of(team.member1());
        }
        return List.of(team.member1(), team.member2());
    }
}
