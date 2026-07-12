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
    public ResponseEntity<List<Team>> getAll() {
        return ResponseEntity.ok(teamsService.getAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TeamRequest body) {
        ResponseEntity<?> validation = validateBody(body);
        if (validation != null) return validation;

        Team created = teamsService.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody TeamRequest body) {

        ResponseEntity<?> validation = validateBody(body);
        if (validation != null) return validation;

        Team updated = teamsService.update(id, body);
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
