package com.maprun.results.teams;

import com.maprun.results.model.Team;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for team maintenance.
 *
 * <pre>
 * GET    /api/teams/?eventId={eventId} — list all teams for an event
 * POST   /api/teams/{eventId}          — create a team
 * PUT    /api/teams/{eventId}/{id}     — replace a team
 * DELETE /api/teams/{eventId}/{id}     — delete a team
 * </pre>
 *
 * <p>Request body for POST and PUT:
 * <pre>
 * {
 *   "teamName":      "Smith &amp; Jones",
 *   "member1":       "Smith John",
 *   "member1Age":    52,
 *   "member1Gender": "M",
 *   "member2":       "Jones Alice",   // omit or null for solo
 *   "member2Age":    48,
 *   "member2Gender": "F"
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
    public ResponseEntity<List<Team>> getAll(@RequestParam(name="eventId") String eventId) {
        return ResponseEntity.ok(teamsService.getAll(eventId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestParam(name="eventId") String eventId, @RequestBody TeamRequest body) {
        ResponseEntity<?> validation = validateBody(body);
        if (validation != null) return validation;

        Team created = teamsService.create(eventId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<?> update(
            @RequestParam(name="eventId") String eventId,
            @PathVariable String teamId,
            @RequestBody TeamRequest body) {

        ResponseEntity<?> validation = validateBody(body);
        if (validation != null) return validation;

        Team updated = teamsService.update(eventId, teamId, body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> delete(@RequestParam(name="eventId") String eventId, @PathVariable String teamId) {
        teamsService.delete(eventId, teamId);
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
        if (body.member1Age() == null || body.member1Age() < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'member1Age' must be a non-negative integer."));
        }
        if (!isValidGender(body.member1Gender())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'member1Gender' must be 'M' or 'F'."));
        }
        boolean hasMember2 = body.member2() != null && !body.member2().isBlank();
        if (hasMember2) {
            if (body.member2Age() == null || body.member2Age() < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "'member2Age' must be a non-negative integer when member2 is provided."));
            }
            if (!isValidGender(body.member2Gender())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "'member2Gender' must be 'M' or 'F' when member2 is provided."));
            }
        }
        return null;
    }

    private static boolean isValidGender(String g) {
        return "M".equalsIgnoreCase(g) || "F".equalsIgnoreCase(g);
    }

    /**
     * JSON request body for create and update operations.
     *
     * @param teamName      user-defined team name (required)
     * @param member1       first competitor MapRun name (required)
     * @param member1Age    real age in whole years (required)
     * @param member1Gender {@code "M"} or {@code "F"} (required)
     * @param member2       second competitor name (optional)
     * @param member2Age    real age of member 2 (required when member2 present)
     * @param member2Gender gender of member 2 (required when member2 present)
     */
    public record TeamRequest(
            String teamName,
            String member1,
            Integer member1Age,
            String member1Gender,
            String member2,
            Integer member2Age,
            String member2Gender
    ) {}
}
