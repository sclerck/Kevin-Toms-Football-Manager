package org.sclerck.ktfm;

import org.sclerck.ktfm.model.League;
import java.util.List;
import org.sclerck.ktfm.model.Team;

import static org.sclerck.ktfm.FootballManager.MAX_PLAYERS;
import static org.sclerck.ktfm.FootballManager.clampMorale;
import static org.sclerck.ktfm.FootballManager.currentLeague;
import static org.sclerck.ktfm.FootballManager.divMult;
import static org.sclerck.ktfm.FootballManager.division;
import static org.sclerck.ktfm.FootballManager.myTeam;
import static org.sclerck.ktfm.FootballManager.player;
import static org.sclerck.ktfm.FootballManager.r;
import static org.sclerck.ktfm.FootballManager.setTeamAttribute;
import static org.sclerck.ktfm.FootballManager.standing;
import static org.sclerck.ktfm.FootballManager.teams;
import static org.sclerck.ktfm.Utils.cls;
import static org.sclerck.ktfm.Utils.waitEnter;

/** Season lifecycle: starting a season, the end-of-season review, and promotion or relegation. */
final class SeasonEngine {

    static int seasons;     // completed seasons

    private SeasonEngine() { }

    // =========================================================================
    // END OF SEASON  (subroutine 8100)
    // =========================================================================
    static void endOfSeasonCheck() {
        if (MatchEngine.leagueMatch < 15 || MatchEngine.inCup) return;  // simplified trigger

        cls();
        MatchEngine.printLeagueTable();

        // Bonus for performance
        int pos = currentLeague.getTable().indexOf(standing(myTeam)) + 1;
        Finance.awardSeasonBonus(pos);

        // Promotion / relegation
        if (pos <= 3 && division > 1) {
            System.out.println("🎉 " + myTeam.getName() + " are PROMOTED!");
            FootballManager.division--;
            moveManagedTeamToDivision();
            FootballManager.divMult = 5 - division;
        } else if (pos > currentLeague.getTeams().size() - 3 && division < 4) {
            System.out.println("😞 " + myTeam.getName() + " are RELEGATED!");
            FootballManager.division++;
            moveManagedTeamToDivision();
            FootballManager.divMult = 5 - division;
        } else if (pos == 1) {
            System.out.println("🏆 " + myTeam.getName() + " are League Champions!");
        }

        seasons++;
        startSeason();
        waitEnter();
    }

    // ─── Reset season variables (subroutine 8800) ─────────────────────────
    static void startSeason() {
        FootballManager.divMult = 5 - division;
        FootballManager.currentLeague = new League(division);
        teams.get(division).forEach(currentLeague::addTeam);
        MatchEngine.startSeason();

        for (int i = 1; i <= MAX_PLAYERS; i++) {
            int value = (int) (5000.0 * divMult * r(5));
            player(i).setMarketValue(value);
            player(i).setSkill(Math.max(1, value / (5000 * divMult)));
            player(i).setEnergy(r(20));
        }

        setTeamAttribute(2, 10); // morale
        clampMorale();
        Finance.startSeason();
    }

    /** Moves the managed team to its new division, swapping places with the last team there. */
    private static void moveManagedTeamToDivision() {
        int previousDivision = myTeam.getDivision();
        List<Team> oldLeague = teams.get(previousDivision);
        List<Team> newLeague = teams.get(division);
        Team displacedTeam = newLeague.removeLast();
        oldLeague.remove(myTeam);
        newLeague.add(myTeam);
        oldLeague.add(displacedTeam);
        displacedTeam.setDivision(previousDivision);
        myTeam.setDivision(division);
    }
}