package com.maprun.results.teams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maprun.results.model.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple JSON-file-backed store for {@link Team} definitions.
 *
 * <p>All reads load the file from disk; all writes are atomic (write to a
 * temp file then move). If the file does not yet exist an empty list is
 * returned and the file is created on the first write.
 *
 * <p>The file path is controlled by the {@code app.teams-file} property
 * (default: {@code teams.json} in the process working directory).
 */
@Repository
public class TeamsRepository {

    private static final Logger logger = LoggerFactory.getLogger(TeamsRepository.class);
    private static final TypeReference<List<Team>> LIST_TYPE = new TypeReference<>() {};

    private final Path teamsFile;
    private final ObjectMapper objectMapper;

    public TeamsRepository(
            @Value("${app.teams-file:teams.json}") String teamsFilePath,
            ObjectMapper objectMapper) {
        this.teamsFile = Path.of(teamsFilePath);
        this.objectMapper = objectMapper;
    }

    /**
     * Returns all persisted teams, or an empty list if the file does not exist.
     */
    public synchronized List<Team> findAll() {
        if (!Files.exists(teamsFile)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(teamsFile.toFile(), LIST_TYPE);
        } catch (IOException e) {
            logger.error("Failed to read teams file {}: {}", teamsFile, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Persists the given list, replacing the entire file contents.
     */
    public synchronized void saveAll(List<Team> teams) {
        try {
            // Write to a sibling temp file then atomically move to avoid partial writes.
            Path tmp = Path.of(teamsFile + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), teams);
            Files.move(tmp, teamsFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to write teams file {}: {}", teamsFile, e.getMessage());
            throw new RuntimeException("Could not persist teams", e);
        }
    }
}
