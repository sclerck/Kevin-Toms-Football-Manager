package org.sclerck.ktfm;

import org.sclerck.ktfm.model.League;
import org.sclerck.ktfm.model.Team;

import static org.sclerck.ktfm.FootballManager.DIV_SIZE;
import static org.sclerck.ktfm.FootballManager.MAX_PLAYERS;
import static org.sclerck.ktfm.FootballManager.clampMorale;
import static org.sclerck.ktfm.FootballManager.currentLeague;
import static org.sclerck.ktfm.FootballManager.divMult;
import static org.sclerck.ktfm.FootballManager.divOffset;
import static org.sclerck.ktfm.FootballManager.division;
import static org.sclerck.ktfm.FootballManager.myTeam;
import static org.sclerck.ktfm.FootballManager.player;
import static org.sclerck.ktfm.FootballManager.r;
import static org.sclerck.ktfm.FootballManager.setTeamAttribute;
import static org.sclerck.ktfm.FootballManager.standing;
import static org.sclerck.ktfm.FootballManager.team;
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
        int pos = currentLeague.getTable().indexOf(standing(myTeam - divOffset)) + 1;
        Finance.awardSeasonBonus(pos);

        // Promotion / relegation
        if (pos <= 3 && division > 1) {
            System.out.println("🎉 " + team(myTeam).getName() + " are PROMOTED!");
            FootballManager.division--;
            moveManagedTeamToDivision();
            FootballManager.divOffset = (division - 1) * DIV_SIZE;
            FootballManager.divMult = 5 - division;
        } else if (pos >= 14 && division < 4) {
            System.out.println("😞 " + team(myTeam).getName() + " are RELEGATED!");
            FootballManager.division++;
            moveManagedTeamToDivision();
            FootballManager.divOffset = (division - 1) * DIV_SIZE;
            FootballManager.divMult = 5 - division;
        } else if (pos == 1) {
            System.out.println("🏆 " + team(myTeam).getName() + " are League Champions!");
        }

        seasons++;
        startSeason();
        waitEnter();
    }

    // ─── Reset season variables (subroutine 8800) ─────────────────────────
    static void startSeason() {
        FootballManager.divOffset = (division - 1) * DIV_SIZE;
        FootballManager.divMult = 5 - division;
        FootballManager.currentLeague = new League(division);
        for (int localTeam = 1; localTeam <= DIV_SIZE; localTeam++) {
            currentLeague.addTeam(team(divOffset + localTeam));
        }
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

    private static void moveManagedTeamToDivision() {
        Team managedTeam = team(myTeam);
        int previousDivision = managedTeam.getDivision();
        int newIndex = (division - 1) * DIV_SIZE + DIV_SIZE;
        if (myTeam != newIndex) {
            Team displacedTeam = team(newIndex);
            FootballManager.teams[newIndex] = managedTeam;
            FootballManager.teams[myTeam] = displacedTeam;
            displacedTeam.setDivision(previousDivision);
            FootballManager.myTeam = newIndex;
        }
        managedTeam.setDivision(division);
    }
}
