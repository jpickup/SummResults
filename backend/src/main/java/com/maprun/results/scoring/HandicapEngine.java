package com.maprun.results.scoring;

import com.maprun.results.model.Team;
import org.springframework.stereotype.Component;

/**
 * Pure stateless handicap calculator.
 *
 * <h2>Rules</h2>
 * <ol>
 *   <li>A team qualifies for a handicap if at least one member's <em>real</em> age is 45 or over.</li>
 *   <li>The <em>effective age</em> of a member is their real age if male ({@code "M"}),
 *       or their real age + 10 if female ({@code "F"}).</li>
 *   <li>The handicap percentage = max(effective ages across all members) − 45.
 *       If no member qualifies the percentage is 0.</li>
 *   <li>The handicap-adjusted score = round(standardTotal × (1 + handicapPct / 100.0)).</li>
 * </ol>
 *
 * <p>Members with {@code null} age or gender contribute 0 effective age and therefore
 * never trigger a handicap on their own.
 */
@Component
public class HandicapEngine {

    private static final int QUALIFYING_AGE = 45;

    /**
     * Computes the handicap percentage for the given team.
     *
     * @param team the team whose members are evaluated
     * @return handicap percentage (≥ 0); 0 means no handicap applies
     */
    public int handicapPct(Team team) {
        int maxEffective = 0;
        boolean anyQualifies = false;

        Integer age1 = team.member1Age();
        String gender1 = team.member1Gender();
        if (age1 != null && gender1 != null) {
            if (age1 >= QUALIFYING_AGE) anyQualifies = true;
            int eff1 = effectiveAge(age1, gender1);
            if (eff1 > maxEffective) maxEffective = eff1;
        }

        Integer age2 = team.member2Age();
        String gender2 = team.member2Gender();
        if (age2 != null && gender2 != null) {
            if (age2 >= QUALIFYING_AGE) anyQualifies = true;
            int eff2 = effectiveAge(age2, gender2);
            if (eff2 > maxEffective) maxEffective = eff2;
        }

        if (!anyQualifies) {
            return 0;
        }
        return Math.max(0, maxEffective - QUALIFYING_AGE);
    }

    /**
     * Applies the handicap percentage to a standard score.
     *
     * @param standardScore the unadjusted total score
     * @param pct           handicap percentage as computed by {@link #handicapPct(Team)}
     * @return the handicap-adjusted score, rounded to the nearest integer
     */
    public int handicapScore(int standardScore, int pct) {
        return (int) Math.round(standardScore * (1.0 + pct / 100.0));
    }

    // -------------------------------------------------------------------------

    private static int effectiveAge(int realAge, String gender) {
        return "F".equalsIgnoreCase(gender) ? realAge + 10 : realAge;
    }
}
