package com.maprun.results.teams;

import com.maprun.results.model.Team;
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

    private final TeamsRepository teamsRepository;

    public TeamsService(TeamsRepository teamsRepository) {
        this.teamsRepository = teamsRepository;
    }

    /** Returns all teams. */
    public List<Team> getAll() {
        return teamsRepository.findAll();
    }

    /**
     * Creates a new team from the given request, assigning a fresh UUID as its ID.
     *
     * @return the persisted team with its assigned ID
     */
    public Team create(TeamsController.TeamRequest req) {
        Team team = toTeam(UUID.randomUUID().toString(), req);
        List<Team> teams = teamsRepository.findAll();
        teams.add(team);
        teamsRepository.saveAll(teams);
        return team;
    }

    /**
     * Replaces an existing team's fields. The ID must match an existing entry.
     *
     * @throws ResponseStatusException 404 if no team with that ID exists
     */
    public Team update(String id, TeamsController.TeamRequest req) {
        List<Team> teams = teamsRepository.findAll();
        int idx = indexById(teams, id);
        Team updated = toTeam(id, req);
        teams.set(idx, updated);
        teamsRepository.saveAll(teams);
        return updated;
    }

    /**
     * Deletes a team by ID.
     *
     * @throws ResponseStatusException 404 if no team with that ID exists
     */
    public void delete(String id) {
        List<Team> teams = teamsRepository.findAll();
        int idx = indexById(teams, id);
        teams.remove(idx);
        teamsRepository.saveAll(teams);
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
