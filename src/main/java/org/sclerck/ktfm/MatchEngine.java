package org.sclerck.ktfm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sclerck.ktfm.model.Competition;
import org.sclerck.ktfm.model.LeagueStanding;
import org.sclerck.ktfm.model.Match;
import org.sclerck.ktfm.model.TeamAttributes;

import static org.sclerck.ktfm.Finance.adjustFinances;
import static org.sclerck.ktfm.Finance.printMoney;
import static org.sclerck.ktfm.FootballManager.DIV_SIZE;

import static org.sclerck.ktfm.FootballManager.MAX_PLAYERS;
import static org.sclerck.ktfm.FootballManager.MAX_TEAMS;
import static org.sclerck.ktfm.FootballManager.RND;
import static org.sclerck.ktfm.FootballManager.clampMorale;
import static org.sclerck.ktfm.FootballManager.currentLeague;
import static org.sclerck.ktfm.FootballManager.divMult;
import static org.sclerck.ktfm.FootballManager.divOffset;
import static org.sclerck.ktfm.FootballManager.division;
import static org.sclerck.ktfm.FootballManager.manager;
import static org.sclerck.ktfm.FootballManager.myTeam;
import static org.sclerck.ktfm.FootballManager.player;
import static org.sclerck.ktfm.FootballManager.printSquad;
import static org.sclerck.ktfm.FootballManager.printSquadHeader;
import static org.sclerck.ktfm.FootballManager.r;
import static org.sclerck.ktfm.FootballManager.setPlayerStatus;
import static org.sclerck.ktfm.FootballManager.setTeamAttribute;
import static org.sclerck.ktfm.FootballManager.skillLevel;
import static org.sclerck.ktfm.FootballManager.standing;
import static org.sclerck.ktfm.FootballManager.statusCode;
import static org.sclerck.ktfm.FootballManager.team;
import static org.sclerck.ktfm.FootballManager.teamAttribute;
import static org.sclerck.ktfm.Utils.cls;
import static org.sclerck.ktfm.Utils.inputInt;
import static org.sclerck.ktfm.Utils.pause;
import static org.sclerck.ktfm.Utils.waitEnter;

/** Everything involved in playing a match: setup, squad selection, simulation and results. */
final class MatchEngine {

    // ─── Season progress (reset by startSeason) ──────────────────────────────
    static int leagueMatch;     // current league match number
    static int cupRound;        // 0 = not started, 1-8
    static boolean inCup;       // still in FA cup this season
    private static int matchCounter;    // cycles 1-2 before cup game
    private static int homeAway;        // h: was your team home(1) or away(2) last game
    private static int leagueGame;      // lc: 1=cup, 2=league

    // ─── Current match state ─────────────────────────────────────────────────
    private static int score1, score2;            // home, away scores
    private static int matchTeam1, matchTeam2;    // team indices
    private static int opp;                       // opponent team index
    private static TeamAttributes oppositionAttributes = new TeamAttributes(1, 1, 1, 1, 1);
    private static Match currentMatch;
    private static final String[] attrName = {"", "Energy", "Morale", "Defence", "Midfield", "Attack"};

    private MatchEngine() { }

    static void startSeason() {
        leagueMatch  = 0;
        cupRound     = 0;
        matchCounter = 1;
        homeAway     = 2;
        inCup        = true;
        leagueGame   = 0;
    }
    // =========================================================================
    // MATCH SETUP  (subroutine 4000 / 4100)
    // =========================================================================
    static void playNextMatch() {
        matchCounter++;
        leagueGame = 2; // default: league
        if (matchCounter == 3) { matchCounter = 0; leagueGame = 1; }
        if (!inCup) leagueGame = 2;

        cls();
        if (leagueGame == 1) {
            // FA Cup
            cupRound++;
            opp = r(MAX_TEAMS);
            while (opp == myTeam) opp = r(MAX_TEAMS);
        } else {
            // League
            leagueMatch++;
            int oppLocal = leagueMatch + divOffset;
            if (oppLocal > divOffset + DIV_SIZE) oppLocal = divOffset + 1;
            opp = oppLocal;
            if (opp == myTeam) { opp++; if (opp > divOffset + DIV_SIZE) opp = divOffset + 1; }
        }

        buildOppositionAttributes();

        // home/away
        homeAway = (homeAway == 1) ? 2 : 1;

        matchTeam1 = (homeAway == 1) ? myTeam : opp;
        matchTeam2 = (homeAway == 1) ? opp     : myTeam;

        computeTeamAttributes();
        System.out.println("──────────────────────────────────────");
        if (leagueGame == 1) {
            System.out.println("  FA CUP MATCH - Round " + cupRoundName());
        } else {
            System.out.println("  LEAGUE MATCH No." + leagueMatch + "  |  Division " + division);
        }
        System.out.println("  " + team(matchTeam1).getName() + "  v  " + team(matchTeam2).getName());
        System.out.println("──────────────────────────────────────");
        printAttributeComparison();
        waitEnter();

        // Pre-match squad selection
        selectSquad();

        // Play match
        playMatch();

        // Post-match
        int prize = matchPrize(leagueGame == 1, cupRound, opp, homeAway == 1);
        adjustFinances(prize, 0);
        System.out.println("Match prize: £" + prize);
        printMoney();
        waitEnter();

        // Cup replay if draw
        if (leagueGame == 1 && score1 == score2) {
            System.out.println("It's a draw! Replay required.");
            waitEnter();
            homeAway = (homeAway == 1) ? 2 : 1;
            matchTeam1 = (homeAway == 1) ? myTeam : opp;
            matchTeam2 = (homeAway == 1) ? opp     : myTeam;
            playMatch();
        }

        // Update league / cup progress
        if (leagueGame == 1) updateCup();
        if (leagueGame == 2) updateLeague();

        // Morale update
        boolean myHome = (homeAway == 1);
        int mySc  = myHome ? score1 : score2;
        int oppSc = myHome ? score2 : score1;
        if (mySc > oppSc) setTeamAttribute(2, teamAttribute(2) + (20 - teamAttribute(2)) / 2);
        if (mySc < oppSc) setTeamAttribute(2, teamAttribute(2) / 2);
        clampMorale();

        // Print other results if league
        if (leagueGame == 2) { printOtherResults(); printLeagueTable(); }
    }

    private static String cupRoundName() {
        if (cupRound < 7) return String.valueOf(cupRound);
        if (cupRound == 7) return "Semi-Final";
        return "Final";
    }

    // ─── Build opposition attributes (line 4150/4152) ─────────────────────
    private static void buildOppositionAttributes() {
        int[] attributes = new int[6];
        for (int i = 1; i <= 5; i++) {
            int val;
            if (leagueGame == 1) {
                val = r(16) + skillLevel + ((myTeam - 1) / DIV_SIZE) - ((opp - 1) / DIV_SIZE + 1);
            } else {
                double zAvg = (leagueMatch > 0) ? (double) standing(opp - divOffset).getPoints() / leagueMatch : 0;
                val = r(14) + skillLevel + (int) zAvg;
            }
            val = Math.clamp(val, 1, 20);
            attributes[i] = val;
        }
        oppositionAttributes = new TeamAttributes(attributes[1], attributes[2],
                attributes[3], attributes[4], attributes[5]);
    }

    // ─── Compute your team attributes from squad (subroutine 6500) ────────
    private static void computeTeamAttributes() {
        int[] attributes = new int[6];

        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (statusCode(i) == 2) {
                int pos = switch (player(i).getPosition()) {
                    case DEFENDER -> 3;
                    case MIDFIELDER -> 4;
                    case ATTACKER -> 5;
                };
                attributes[pos] += player(i).getSkill();
                attributes[1] += player(i).getEnergy();
            }
        }
        attributes[1] /= 11;
        for (int i = 3; i <= 5; i++) {
            attributes[i] = Math.clamp(attributes[i], 1, 20);
        }
        team(myTeam).setAttributes(new TeamAttributes(
                Math.clamp(attributes[1], 1, 20), teamAttribute(2),
                attributes[3], attributes[4], attributes[5]));
    }

    // ─── Print attribute comparison ────────────────────────────────────────
    private static void printAttributeComparison() {
        System.out.printf("%-15s %5s %5s%n", "Attribute", team(myTeam).getName().substring(0, Math.min(5, team(myTeam).getName().length())), team(opp).getName().substring(0, Math.min(5, team(opp).getName().length())));
        for (int i = 1; i <= 5; i++) {
            System.out.printf("%-15s %5d %5d%n", attrName[i], teamAttribute(i), oppositionAttribute(i));
        }
    }

    // =========================================================================
    // SQUAD SELECTION  (subroutine 6000)
    // =========================================================================
    private static void selectSquad() {
        // Update energy and injury recovery
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (statusCode(i) == 0) continue;
            if (statusCode(i) == 1 || statusCode(i) == 3)
                player(i).setEnergy(Math.min(20, player(i).getEnergy() + 10));
            if (statusCode(i) == 2)
                player(i).setEnergy(Math.max(1, player(i).getEnergy() - 1));
            if (statusCode(i) == 3) setPlayerStatus(i, 1); // recover
        }

        int availablePlayers = 0;
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (statusCode(i) > 0) availablePlayers++;
        }
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (statusCode(i) != 0 && availablePlayers > Match.PLAYERS_PER_TEAM && r(20) == 20) {
                setPlayerStatus(i, 3);
                availablePlayers--;
            }
        }

        while (true) {
            cls();
            System.out.println("PRE-MATCH SQUAD SELECTION");
            System.out.println("Pick 11 players to play (status 2=playing, 1=squad, 3=injured)");
            printSquadHeader();
            printSquad(true);
            int picked = countPicked();
            System.out.println("Players picked: " + picked + " / 11");
            System.out.println("[99] Done");

            if (picked < 11) {
                System.out.println("Pick a player to add (or 99 to continue):");
                int n = inputInt(">> ");
                if (n == 99) {
                    System.out.println("You must select 11 players to start the match.");
                    pause(800);
                    continue;
                }
                if (n < 1 || n > MAX_PLAYERS) continue;
                if (statusCode(n) != 1) { System.out.println("Can't pick that player."); pause(800); continue; }
                setPlayerStatus(n, 2);
            } else {
                System.out.println("Remove a player or 99 to start match:");
                int n = inputInt(">> ");
                if (n == 99) break;
                if (n < 1 || n > MAX_PLAYERS) continue;
                if (statusCode(n) != 2) { System.out.println("That player isn't in the team."); pause(800); continue; }
                setPlayerStatus(n, 1);
            }
        }
    }

    private static int countPicked() {
        return team(myTeam).getStartingPlayers().size();
    }

    // =========================================================================
    // MATCH SIMULATION  (subroutine 5000)
    // =========================================================================
    private static void playMatch() {
        score1 = 0; score2 = 0;
        cls();
        System.out.println("★★  MATCH HIGHLIGHTS  ★★");
        System.out.println(team(matchTeam1).getName() + "  v  " + team(matchTeam2).getName());
        System.out.println();

        boolean myHome = (matchTeam1 == myTeam);

        for (int round = 1; round <= 5; round++) {
            // Your team attacks
            int n = r(100) + (teamAttribute(round <= 2 ? 3 : round <= 4 ? 4 : 5)
                    - oppositionAttribute(round <= 2 ? 3 : round <= 4 ? 4 : 5)) * 5;
            if (n >= 75) {
                simulateAttack(myHome, true);
                simulateAttack(myHome, true);
            }
            // Opposition attacks
            int m = r(100) + (oppositionAttribute(round <= 2 ? 3 : round <= 4 ? 4 : 5)
                    - teamAttribute(round <= 2 ? 3 : round <= 4 ? 4 : 5)) * 5;
            if (m >= 75) {
                simulateAttack(myHome, false);
                simulateAttack(myHome, false);
            }
        }
        // Guarantee at least one attack if 0-0
        if (score1 + score2 == 0) simulateAttack(myHome, r(2) == 1);

        System.out.println();
        System.out.println("──────────────────────────────────");
        System.out.println("  ★★  FINAL SCORE  ★★");
        System.out.printf("  %-20s %2d%n", team(matchTeam1).getName(), score1);
        System.out.printf("  %-20s %2d%n", team(matchTeam2).getName(), score2);
        currentMatch = Match.simulated(team(matchTeam1), team(matchTeam2),
                leagueGame == 1 ? Competition.FA_CUP : Competition.LEAGUE,
                leagueGame == 1 ? cupRound : leagueMatch, score1, score2);
        System.out.println("──────────────────────────────────");
    }

    private static void simulateAttack(boolean myHome, boolean myTeamAttacking) {
        boolean isGoal = r(3) == 1; // simplified probability
        String attacker = randomPlayerName(myTeamAttacking);

        if (isGoal) {
            if (myHome == myTeamAttacking) { score1++; } else { score2++; }
            System.out.println("  ⚽ GOAL!  " + attacker + " scores!");
            System.out.printf("     [%s %d - %d %s]%n", team(matchTeam1).getName(), score1, score2, team(matchTeam2).getName());
        } else {
            System.out.println("  → " + attacker + " drives forward... NO GOAL!");
        }
    }

    private static String randomPlayerName(boolean myTeam) {
        if (myTeam) {
            for (int tries = 0; tries < 50; tries++) {
                int i = r(MAX_PLAYERS);
                if (statusCode(i) == 2) return player(i).getName();
            }
        }
        return "Opposition player";
    }

    // =========================================================================
    // LEAGUE UPDATES  (subroutine 7000)
    // =========================================================================
    private static void updateLeague() {
        currentLeague.recordResult(currentMatch);
    }

    // ─── Print other results (subroutine 7500) ────────────────────────────
    private static void printOtherResults() {
        cls();
        System.out.println("OTHER RESULTS - Division " + division);
        System.out.println("──────────────────────────────────");
        Set<Integer> done = new HashSet<>();
        done.add(myTeam - divOffset);
        done.add(opp    - divOffset);
        int games = 0;
        while (games < 7) {
            int a = r(DIV_SIZE), b = r(DIV_SIZE);
            if (a == b || done.contains(a) || done.contains(b)) continue;
            done.add(a); done.add(b);
            int ga = (int) (standing(a).getPoints() / Math.max(1, leagueMatch) + RND.nextDouble() * 4);
            int gb = (int) (standing(b).getPoints() / Math.max(1, leagueMatch) + RND.nextDouble() * 4);
            System.out.printf("  %-18s %d  -  %d  %-18s%n",
                    team(divOffset + a).getName(), ga, gb, team(divOffset + b).getName());
            currentLeague.recordResult(team(divOffset + a), team(divOffset + b), ga, gb);
            games++;
        }
        waitEnter();
    }

    // ─── Print league table (subroutine 7710 / 7805) ─────────────────────
    static void printLeagueTable() {
        List<LeagueStanding> table = currentLeague.getTable();
        cls();
        System.out.println("══════════════════════════════════════════════════");
        System.out.printf("  %-22s %4s %4s %4s%n", "TEAM", "F", "A", "PTS");
        System.out.println("──────────────────────────────────────────────────");
        for (int rank = 0; rank < DIV_SIZE; rank++) {
            LeagueStanding standing = table.get(rank);
            String marker = (standing.getTeam() == team(myTeam)) ? " ◄" : "";
            System.out.printf("  %-22s %4d %4d %4d%s%n",
                    standing.getTeam().getName(), standing.getGoalsFor(), standing.getGoalsAgainst(),
                    standing.getPoints(), marker);
        }
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("League match " + leagueMatch + "  |  Division " + division);
        waitEnter();
    }

    // =========================================================================
    // CUP UPDATES  (subroutine 5200)
    // =========================================================================
    private static void updateCup() {
        boolean myHome = (matchTeam1 == myTeam);
        int mySc  = myHome ? score1 : score2;
        int oppSc = myHome ? score2 : score1;

        if (mySc < oppSc) {
            inCup = false;
            setTeamAttribute(2, teamAttribute(2) / 2);
            System.out.println("You have been knocked out of the FA Cup!");
        } else {
            if (cupRound == 8) {
                cls();
                System.out.println();
                System.out.println("╔══════════════════════════════════╗");
                System.out.println("║  🏆  FA CUP WINNERS! 🏆          ║");
                System.out.println("║  Congratulations, " + manager.getName() + "!  ║");
                System.out.println("╚══════════════════════════════════╝");
                System.out.println();
                pause(2000);
            } else {
                System.out.println("You are through to round " + (cupRound + 1) + "!");
            }
            setTeamAttribute(2, teamAttribute(2) + (20 - teamAttribute(2)) / 2);
        }
        clampMorale();
        waitEnter();
    }

    private static int oppositionAttribute(int attribute) {
        return switch (attribute) {
            case 1 -> oppositionAttributes.energy();
            case 2 -> oppositionAttributes.morale();
            case 3 -> oppositionAttributes.defence();
            case 4 -> oppositionAttributes.midfield();
            case 5 -> oppositionAttributes.attack();
            default -> throw new IllegalArgumentException("Unknown team attribute: " + attribute);
        };
    }

    // ─── Prize money ──────────────────────────────────────────────────────
    private static int matchPrize(boolean cupMatch, int cupRound, int opponentTeam, boolean homeGame) {
        int opponentDivision = (opponentTeam - 1) / DIV_SIZE + 1;
        if (cupMatch) {
            if (cupRound == 7) return 50000;
            if (cupRound == 8) return 100000;
            return (5 - opponentDivision) * 10000;
        }
        if (homeGame) return 5000 * divMult;
        return (17 - opponentDivision) * divMult * 500;
    }
}
