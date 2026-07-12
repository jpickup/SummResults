package com.maprun.results.teams;

import com.maprun.results.model.Team;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for team maintenance.
 *
 * <pre>
 * GET    /api/teams          — list all teams
 * POST   /api/teams          — create a team
 * PUT    /api/teams/{id}     — replace a team
 * DELETE /api/teams/{id}     — delete a team
 * </pre>
 *
 * <p>Request body for POST and PUT:
 * <pre>
 * {
 *   "teamName": "Smith & Jones",
 *   "member1":  "Smith John",
 *   "member2":  "Jones Alice"   // omit or null for a solo entry
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/teams")
public class TeamsController {

    private final TeamsService teamsService;

    public TeamsController(TeamsService teamsService) {
        this.teamsService = teamsService;
    }

    @GetMapping
    public ResponseEntity<List<Team>> getAll() {
        return ResponseEntity.ok(teamsService.getAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TeamRequest body) {
        ResponseEntity<?> validation = validateBody(body);
        if (validation != null) return validation;

        Team created = teamsService.create(body.teamName(), body.member1(), body.member2());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody TeamRequest body) {

        ResponseEntity<?> validation = validateBody(body);
        if (validation != null) return validation;

        Team updated = teamsService.update(id, body.teamName(), body.member1(), body.member2());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        teamsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------

    private ResponseEntity<?> validateBody(TeamRequest body) {
        if (body == null || body.teamName() == null || body.teamName().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'teamName' must not be blank."));
        }
        if (body.member1() == null || body.member1().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'member1' must not be blank."));
        }
        return null;
    }

    /**
     * JSON request body for create and update operations.
     *
     * @param teamName user-defined team name
     * @param member1  first competitor (required)
     * @param member2  second competitor (optional, may be null or absent)
     */
    public record TeamRequest(String teamName, String member1, String member2) {}
}
