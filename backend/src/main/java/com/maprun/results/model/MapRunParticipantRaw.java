package com.maprun.results.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Raw DTO mapping a single participant entry from the MapRun API ResultsList.
 */
public record MapRunParticipantRaw(
        @JsonProperty("Name")          String name,
        @JsonProperty("GrossScore")    int grossScore,
        @JsonProperty("Penalty")       int penalty,
        @JsonProperty("ScoreControls") List<MapRunControlRaw> scoreControls
) {}
