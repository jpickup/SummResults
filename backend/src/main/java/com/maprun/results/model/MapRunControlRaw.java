package com.maprun.results.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw DTO mapping a single control visit entry within a participant's ScoreControls list.
 */
public record MapRunControlRaw(
        @JsonProperty("Control") String controlId,
        @JsonProperty("Points")  int points
) {}
