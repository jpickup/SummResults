package com.maprun.results.teams;

import com.maprun.results.config.EventsConfig;
import com.maprun.results.model.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * CRUD operations for {@link Team} definitions.
 *
 * <p>IDs are assigned here as random UUIDs on creation. All mutations
 * read-modify-write through {@link TeamsRepository}.
 */
@Service
public class TeamsService {
    private static final Logger logger = LoggerFactory.getLogger(TeamsService.class);

    private final TeamsRepository teamsRepository;
    private final EventsConfig eventsConfig;

    public TeamsService(TeamsRepository teamsRepository, EventsConfig eventsConfig) {
        this.teamsRepository = teamsRepository;
        this.eventsConfig = eventsConfig;
    }

    /** Returns all teams. */
    public List<Team> getAll(String eventId) {
        EventsConfig.EventEntry event = eventsConfig.getEvent(eventId);
        logger.info("Loading teams for event {}", event);
        return teamsRepository.findAll(event.getTeamsFilename());
    }

    /**
     * Creates a new team from the given request, assigning a fresh UUID as its ID.
     *
     * @return the persisted team with its assigned ID
     */
    public Team create(String eventId, TeamsController.TeamRequest req) {
        logger.info("Creating team for event {}", eventId);
        EventsConfig.EventEntry event = eventsConfig.getEvent(eventId);
        Team team = toTeam(UUID.randomUUID().toString(), req);
        List<Team> teams = teamsRepository.findAll(event.getTeamsFilename());
        teams.add(team);
        teamsRepository.saveAll(event.getTeamsFilename(), teams);
        return team;
    }

    /**
     * Replaces an existing team's fields. The ID must match an existing entry.
     *
     * @throws ResponseStatusException 404 if no team with that ID exists
     */
    public Team update(String eventId, String teamId, TeamsController.TeamRequest req) {
        logger.info("Updating team for event {}", eventId);
        EventsConfig.EventEntry event = eventsConfig.getEvent(eventId);
        List<Team> teams = teamsRepository.findAll(event.getTeamsFilename());
        int idx = indexById(teams, teamId);
        Team updated = toTeam(teamId, req);
        teams.set(idx, updated);
        teamsRepository.saveAll(event.getTeamsFilename(), teams);
        return updated;
    }

    /**
     * Deletes a team by ID.
     *
     * @throws ResponseStatusException 404 if no team with that ID exists
     */
    public void delete(String eventId, String teamId) {
        logger.info("Deleting team for event {}", eventId);
        EventsConfig.EventEntry event = eventsConfig.getEvent(eventId);
        List<Team> teams = teamsRepository.findAll(event.getTeamsFilename());
        int idx = indexById(teams, teamId);
        teams.remove(idx);
        teamsRepository.saveAll(event.getTeamsFilename(), teams);
    }

    // -----------------------------------------------------------------------

    private static Team toTeam(String id, TeamsController.TeamRequest req) {
        boolean hasMember2 = req.member2() != null && !req.member2().isBlank();
        return new Team(
                id,
                req.teamName(),
                req.member1(),
                req.member1Age(),
                req.member1Gender() == null ? null : req.member1Gender().toUpperCase(),
                hasMember2 ? req.member2()       : null,
                hasMember2 ? req.member2Age()    : null,
                hasMember2 ? (req.member2Gender() == null ? null : req.member2Gender().toUpperCase()) : null
        );
    }

    private int indexById(List<Team> teams, String id) {
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).id().equals(id)) {
                return i;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found: " + id);
    }
}
