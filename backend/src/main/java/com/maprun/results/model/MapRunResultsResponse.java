package com.maprun.results.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Raw DTO mapping the top-level JSON object returned by the MapRun public API.
 */
public record MapRunResultsResponse(
        @JsonProperty("EventName") String eventName,
        @JsonProperty("ResultsList") List<MapRunParticipantRaw> resultsList
) {}
