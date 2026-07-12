package com.maprun.results.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Raw DTO mapping the top-level JSON object returned by the MapRun public API.
 */
public record MapRunResultsResponse(
        @JsonProperty("errorFlag") boolean errorFlag,
        @JsonProperty("statusMessage") String statusMessage,
        @JsonProperty("warningFlag") boolean warningFlag,
        @JsonProperty("warningMessage") String warningMessage,
        @JsonProperty("results") List<MapRunParticipantRaw> resultsList
) {}
