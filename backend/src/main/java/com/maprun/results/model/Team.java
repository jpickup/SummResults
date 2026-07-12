package com.maprun.results.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A team of one or two competitors.
 *
 * <p>{@code member2} is optional — leave it {@code null} for a solo entry.
 * {@code id} is a stable UUID-based string assigned by the backend on creation
 * and used as the key for update/delete operations.
 *
 * @param id       stable unique identifier, e.g. a UUID string
 * @param teamName user-defined display name for the team
 * @param member1  name of the first competitor, must match the MapRun participant name exactly
 * @param member2  name of the second competitor (optional), or {@code null} for a solo entry
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Team(
        String id,
        String teamName,
        String member1,
        String member2
) {}
