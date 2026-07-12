package com.maprun.results.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A team of one or two competitors, each with age and gender for handicap calculation.
 *
 * <p>Gender is represented as {@code "M"} (male) or {@code "F"} (female).
 * Fields for member 2 are {@code null} for solo entries and are omitted from JSON.
 *
 * @param id             stable unique identifier (UUID string)
 * @param teamName       user-defined display name
 * @param member1        name matching the MapRun participant name exactly
 * @param member1Age     real age of member 1 in whole years
 * @param member1Gender  {@code "M"} or {@code "F"}
 * @param member2        second competitor name, or {@code null} for a solo entry
 * @param member2Age     real age of member 2, or {@code null} for solo entries
 * @param member2Gender  {@code "M"} or {@code "F"}, or {@code null} for solo entries
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Team(
        String id,
        String teamName,
        String member1,
        Integer member1Age,
        String member1Gender,
        String member2,
        Integer member2Age,
        String member2Gender
) {}
