package com.maprun.results.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Raw DTO mapping a single participant entry from the MapRun API results array.
 *
 * <p>The real API does not provide per-control point values; only the aggregate
 * {@code GrossScore} and {@code NetScore} are available. Individual control IDs
 * are listed in {@code punchControlIds} as plain strings.</p>
 *
 * <p>{@code penalty} is derived by the client as {@code GrossScore - NetScore}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MapRunParticipantRaw(
        @JsonProperty("Surname")         String surname,
        @JsonProperty("Firstname")        String firstname,
        @JsonProperty("GrossScore")       int grossScore,
        @JsonProperty("NetScore")         int netScore,
        @JsonProperty("punchControlIds")  List<String> punchControlIds
) {}
