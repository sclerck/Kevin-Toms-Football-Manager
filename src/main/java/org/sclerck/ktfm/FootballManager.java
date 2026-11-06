package org.sclerck.ktfm;


import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import org.sclerck.ktfm.model.Competition;
import org.sclerck.ktfm.model.League;
import org.sclerck.ktfm.model.LeagueStanding;
import org.sclerck.ktfm.model.Manager;
import org.sclerck.ktfm.model.Match;
import org.sclerck.ktfm.model.Player;
import org.sclerck.ktfm.model.PlayerStatus;
import org.sclerck.ktfm.model.Team;
import org.sclerck.ktfm.model.TeamAttributes;
import org.sclerck.ktfm.model.TeamColor;

/**
 * Football Manager - Java port of Kevin Toms' classic 1982 ZX Spectrum game.
 * Original: Copyright K. & J. Toms 1982
 * Java port preserves all original game logic and data.
 */
public class FootballManager {

    // ─── Constants ───────────────────────────────────────────────────────────
    static final int MAX_PLAYERS = 24;
    static final int MAX_TEAMS   = 64;
    static final int DIV_SIZE    = 16;
    private static final int MAX_LOAN    = 250000;

    // ─── Random ──────────────────────────────────────────────────────────────
    private static final Random RND = new Random();
    /** Equivalent to FN r(x) = INT(RND*x+1) */
    static int r(int x) { return RND.nextInt(x) + 1; }

    // ─── Player data ─────────────────────────────────────────────────────────
    private static Player[] players = new Player[MAX_PLAYERS + 1]; // 1-based

    // ─── Team data ───────────────────────────────────────────────────────────
    private static Team[] teams = new Team[MAX_TEAMS + 1]; // 1-based

    // ─── League season ────────────────────────────────────────────────────
    private static League currentLeague;

    // ─── Generated opposition attributes for the current match ────────────
    private static TeamAttributes oppositionAttributes = new TeamAttributes(1, 1, 1, 1, 1);

    // ─── Match state ─────────────────────────────────────────────────────────
    private static int score1, score2;            // home, away scores
    private static int matchTeam1, matchTeam2;    // team indices

    // ─── Season / game state ─────────────────────────────────────────────────
    private static Manager manager;
    private static int myTeam;          // team index (1-based in teams[])
    private static int division;        // 1-4
    private static int divOffset;       // (division-1)*16
    private static int divMult;         // 5-division (money multiplier)
    private static int leagueMatch;     // current league match number
    private static int cupRound;        // 0 = not started, 1-8
    private static int matchCounter;    // cycles 1-2 before cup game
    private static int homeAway;        // h: was your team home(1) or away(2) last game
    private static int leagueGame;      // lc: 1=cup, 2=league
    private static int opp;             // opponent team index
    private static int groundRent;
    private static int skillLevel;      // 1-7
    private static int seasons;
    private static boolean inCup;       // still in FA cup this season
    static final String[] levelName   = {"", "Beginner", "Novice", "Average", "Good", "Expert", "Super Expert", "Genius"};
    private static final String[] attrName    = {"", "Energy", "Morale", "Defence", "Midfield", "Attack"};

    // ─── Completed match ──────────────────────────────────────────────────
    private static Match currentMatch;

    // ─── Scanner ─────────────────────────────────────────────────────────────
    private static final Scanner SC = new Scanner(System.in);

    // =========================================================================
    // ENTRY POINT
    // =========================================================================
    public static void play() {
        Setup.Result setup = Setup.run();
        players = setup.players();
        teams = setup.teams();
        manager = setup.manager();
        myTeam = setup.myTeam();
        division = setup.division();
        skillLevel = manager.getSkillLevel();

        initSeason();

        mainLoop();
    }

    // =========================================================================
    // MAIN LOOP
    // =========================================================================
    private static void mainLoop() {
        while (true) {
            managementMenu();
            setupMatch();
            payWages();
            endOfSeasonCheck();
            transferMarket();
        }
    }

    // =========================================================================
    // MANAGEMENT MENU  (subroutine 2000)
    // =========================================================================
    private static void managementMenu() {
        while (true) {
            cls();
            System.out.println("══════════════════════════════════");
            System.out.println("  FOOTBALL MANAGER - Main Menu");
            System.out.println("══════════════════════════════════");
            System.out.println("  [A] Sell or list your players");
            System.out.println("  [S] Print score / stats");
            System.out.println("  [L] Obtain a loan");
            System.out.println("  [P] Pay off loan");
            System.out.println("  [CHANGE] Change team/player names");
            System.out.println("  [LEVEL]  Change skill level");
            System.out.println("  [KEEP]   Save & continue");
            System.out.println("  [99]     Continue to next match");
            System.out.println("══════════════════════════════════");
            String cmd = inputLine("Enter command: ", 0).toUpperCase();

            switch (cmd) {
                case "A"      -> sellOrListPlayers();
                case "S"      -> printStats();
                case "L"      -> obtainLoan();
                case "P"      -> payOffLoan();
                case "CHANGE" -> { changeTeamNames(); changePlayerNames(); }
                case "LEVEL"  -> changeLevel();
                case "KEEP"   -> { saveGame(); return; }
                case "99"     -> { return; }
            }
        }
    }

    // ─── Sell or list players (subroutine 2400) ───────────────────────────
    private static void sellOrListPlayers() {
        while (true) {
            cls();
            printSquadHeader();
            printSquad(false);
            System.out.println();
            System.out.println("[99] to go back");
            int n = inputInt("Select player number to sell (or 99): ");
            if (n == 99) return;
            if (n < 1 || n > MAX_PLAYERS) continue;
            if (statusCode(n) == 0) { System.out.println("No such player."); pause(1000); continue; }
            if (team(myTeam).getPlayers().size() <= Match.PLAYERS_PER_TEAM) {
                System.out.println("You must keep at least 11 players in your squad.");
                pause(1000); continue;
            }
            if (statusCode(n) == 3) {
                System.out.println(player(n).getName() + " is injured - nobody wants him!");
                pause(1500); continue;
            }
            int offer = (int) (((r(5) + 7.0) * player(n).getMarketValue()) / 10);
            System.out.println(randomTeamName() + " have offered £" + offer + " for " + player(n).getName());
            System.out.println("He is worth £" + player(n).getMarketValue());
            String ans = inputLine("Do you accept? (y/n): ", 0).toUpperCase();
            if (ans.equals("Y") || ans.equals("YES")) {
                setPlayerStatus(n, 0);
                adjustFinances(offer, 0);
                System.out.println(player(n).getName() + " has been sold.");
                printMoney(); pause(1500);
            } else {
                if (r(3) == 1) {
                    setPlayerStatus(n, 3);
                    System.out.println("He's been injured in training after rejecting the move!");
                    pause(1500);
                }
            }
        }
    }

    // ─── Print stats (subroutine 3000) ────────────────────────────────────
    private static void printStats() {
        cls();
        System.out.println("Team    : " + team(myTeam).getName());
        System.out.println("Manager : " + manager.getName());
        System.out.println("Colours : " + (team(myTeam).getColor() == TeamColor.BLACK ? "Black" : "White"));
        System.out.println("Level   : " + manager.getSkillLevel() + " - " + levelName[manager.getSkillLevel()]);
        System.out.println("Seasons : " + seasons);
        System.out.println("Division: " + division);
        System.out.println("Morale  : " + teamAttribute(2));
        printMoney();
        System.out.println("League match no: " + leagueMatch);
        waitEnter();
    }

    // ─── Loan (subroutine 2800) ────────────────────────────────────────────
    private static void obtainLoan() {
        cls();
        printMoney();
        int amount = inputInt("How much do you want to borrow? £");
        cls();
        if (amount <= 0) {
            System.out.println("Enter a positive amount.");
            pause(1500); return;
        }
        int maxLoan = MAX_LOAN * divMult;
        if (amount + debt() > maxLoan) {
            System.out.println("Maximum loan is £" + maxLoan);
            pause(1500); return;
        }
        adjustFinances(amount, amount);
        printMoney();
        waitEnter();
    }

    // ─── Pay off loan (subroutine 2900) ───────────────────────────────────
    private static void payOffLoan() {
        cls();
        printMoney();
        int amount = inputInt("How much do you want to repay? £");
        cls();
        if (amount <= 0) { System.out.println("Enter a positive amount."); pause(1500); return; }
        if (amount > debt()) { System.out.println("You don't owe that much!"); pause(1500); return; }
        if (amount > balance()) { System.out.println("You haven't enough money!"); pause(1500); return; }
        adjustFinances(-amount, -amount);
        printMoney();
        waitEnter();
    }

    // ─── Change level (subroutine 3200) ───────────────────────────────────
    private static void changeLevel() {
        cls();
        System.out.println("Current level: " + skillLevel + " - " + levelName[skillLevel]);
        System.out.println("Choose a level:");
        for (int i = 1; i <= 7; i++) System.out.println("  " + i + " - " + levelName[i]);
        int n = inputInt("Enter level (1-7): ");
        if (n < 1 || n > 7) { System.out.println("1 to 7 please!"); pause(1000); return; }
        skillLevel = n;
        manager = new Manager(manager.getName(), team(myTeam), skillLevel);
        System.out.println("New skill level: " + skillLevel + " - " + levelName[skillLevel]);
        waitEnter();
    }

    // ─── Save game (subroutine 800) ───────────────────────────────────────
    private static void saveGame() {
        System.out.println("(Save not implemented in this port - your game state is held in memory.)");
        String ans = inputLine("Do you want to play more? (y/n): ", 0).toUpperCase();
        if (!ans.equals("Y") && !ans.equals("YES")) {
            System.out.println("Thanks for playing Football Manager!");
            System.exit(0);
        }
    }

    // =========================================================================
    // MATCH SETUP  (subroutine 4000 / 4100)
    // =========================================================================
    private static void setupMatch() {
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
        adjustFinances(matchPrize(), 0);
        System.out.println("Match prize: £" + matchPrize());
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

    private static int matchPrize() {
        if (leagueGame == 1) {
            if (cupRound == 7) return 50000;
            if (cupRound == 8) return 100000;
            int div = (opp - 1) / DIV_SIZE + 1;
            return (5 - div) * 10000;
        } else {
            if (homeAway == 1) return 5000 * divMult;
            int oppDiv = (opp - 1) / DIV_SIZE + 1;
            return (17 - oppDiv) * divMult * 500;
        }
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
    private static void printLeagueTable() {
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

    // =========================================================================
    // WAGES AND FINANCES  (subroutine 8000)
    // =========================================================================
    private static void payWages() {
        cls();
        int wages = 0;
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (statusCode(i) > 0) wages += player(i).getSkill() * 100 * divMult;
        }
        int interest = debt() / 100;

        System.out.println("── WEEKLY FINANCES ─────────────────");
        System.out.println("  Wage bill    : £" + wages);
        System.out.println("  Ground rent  : £" + groundRent);
        System.out.println("  Loan interest: £" + interest);

        int balance = balance() - wages - groundRent - interest;
        int debt = debt();

        System.out.println("  Weekly balance: £" + (balance - debt));

        if (balance < 0) {
            debt -= balance;
            balance = 0;
            System.out.println("  ⚠ Shortfall added to loan!");
        }
        team(myTeam).setFinances(balance, debt);
        if (debt() > MAX_LOAN * divMult) {
            System.out.println("  ❌ DEBT EXCEEDED LIMIT - GAME OVER");
            waitEnter();
            System.exit(0);
        }
        waitEnter();
    }

    // =========================================================================
    // END OF SEASON  (subroutine 8100)
    // =========================================================================
    private static void endOfSeasonCheck() {
        if (leagueMatch < 15 || inCup) return;  // simplified trigger

        cls();
        printLeagueTable();

        // Bonus for performance
        int pos = currentLeague.getTable().indexOf(standing(myTeam - divOffset)) + 1;
        int bonus = (17 - pos) * 5000 * divMult + cupRound * 2;
        adjustFinances((17 - pos) * 5000 * divMult, 0);
        System.out.println("Season bonus: £" + ((17 - pos) * 5000 * divMult));

        // Promotion / relegation
        if (pos <= 3 && division > 1) {
            System.out.println("🎉 " + team(myTeam).getName() + " are PROMOTED!");
            division--;
            moveManagedTeamToDivision();
            divOffset = (division - 1) * DIV_SIZE;
            divMult = 5 - division;
        } else if (pos >= 14 && division < 4) {
            System.out.println("😞 " + team(myTeam).getName() + " are RELEGATED!");
            division++;
            moveManagedTeamToDivision();
            divOffset = (division - 1) * DIV_SIZE;
            divMult = 5 - division;
        } else if (pos == 1) {
            System.out.println("🏆 " + team(myTeam).getName() + " are League Champions!");
        }

        seasons++;
        initSeason();
        waitEnter();
    }

    // ─── Reset season variables (subroutine 8800) ─────────────────────────
    private static void initSeason() {
        divOffset = (division - 1) * DIV_SIZE;
        divMult   = 5 - division;
        currentLeague = new League(division);
        for (int localTeam = 1; localTeam <= DIV_SIZE; localTeam++) {
            currentLeague.addTeam(team(divOffset + localTeam));
        }
        leagueMatch  = 0;
        cupRound     = 0;
        matchCounter = 1;
        homeAway     = 2;
        inCup        = true;
        leagueGame   = 0;
        groundRent   = 500 * divMult;

        for (int i = 1; i <= MAX_PLAYERS; i++) {
            int value = (int) (5000.0 * divMult * r(5));
            player(i).setMarketValue(value);
            player(i).setSkill(Math.max(1, value / (5000 * divMult)));
            player(i).setEnergy(r(20));
        }

        setTeamAttribute(2, 10); // morale
        clampMorale();
        if (balance() == 0) team(myTeam).setFinances(50000 * divMult, debt());
    }

    // =========================================================================
    // TRANSFER MARKET  (subroutine 8700)
    // =========================================================================
    private static void transferMarket() {
        if (leagueMatch == 0) return;

        // Find an available player (one not in squad)
        int candidate = -1;
        for (int tries = 0; tries < 50; tries++) {
            int n = r(MAX_PLAYERS);
            if (statusCode(n) == 0) { candidate = n; break; }
        }
        if (candidate == -1) return;

        int squadSize = team(myTeam).getPlayers().size();
        if (squadSize >= 16) {
            System.out.println("Squad full (16 max) - cannot sign new player.");
            waitEnter(); return;
        }

        cls();
        System.out.println("── TRANSFER MARKET ─────────────────");
        printSquadHeader();
        System.out.printf("  %-10s %3d  Skill:%-3d  Energy:%-3d  Value:£%d%n",
                player(candidate).getName(), candidate,
                player(candidate).getSkill(), player(candidate).getEnergy(), player(candidate).getMarketValue());
        printMoney();
        System.out.println("[99] to decline");

        int bid = inputInt("Your bid for " + player(candidate).getName() + ": £");
        if (bid == 99) return;

        if (balance() < bid) {
            System.out.println("You haven't enough money!");
            pause(1500); return;
        }
        double ratio = (double)(r(10) * bid) / player(candidate).getMarketValue();
        if (ratio >= 5) {
            setPlayerStatus(candidate, 1);
            player(candidate).setMarketValue(player(candidate).getSkill() * 5000 * divMult);
            team(myTeam).addPlayer(player(candidate));
            adjustFinances(-bid, 0);
            System.out.println(player(candidate).getName() + " has joined your team!");
        } else {
            player(candidate).setMarketValue((int)(player(candidate).getMarketValue() * 1.2));
            System.out.println("Bid rejected. His value has risen to £" + player(candidate).getMarketValue());
        }
        waitEnter();
    }

    // =========================================================================
    // SQUAD DISPLAY HELPERS
    // =========================================================================
    private static void printSquadHeader() {
        System.out.printf("  %-12s %3s %6s %7s %8s %7s%n",
                "NAME", "NO.", "SKILL", "ENERGY", "VALUE", "STATUS");
        System.out.println("  " + "─".repeat(52));
    }

    private static void printSquad(boolean showStatus) {
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (statusCode(i) == 0) continue;
            String positionLabel = switch (player(i).getPosition()) {
                case DEFENDER -> "D ";
                case MIDFIELDER -> "M ";
                case ATTACKER -> "A ";
            };
            String status = switch(statusCode(i)) {
                case 1 -> "Squad";
                case 2 -> "PLAYING";
                case 3 -> "Injured";
                default -> "?";
            };
            System.out.printf("  %s%-12s %3d %6d %7d %8s %7s%n",
                    positionLabel, player(i).getName(), i,
                    player(i).getSkill(), player(i).getEnergy(), "£" + player(i).getMarketValue(),
                    showStatus ? status : "");
        }
    }

    // ─── Print money (subroutine 1000) ────────────────────────────────────
    private static void printMoney() {
        System.out.println("  Money: £" + balance() + "   Loan: £" + debt());
    }

    // ─── Clamp morale 1..20 (subroutine 1040) ─────────────────────────────
    private static void clampMorale() {
        setTeamAttribute(2, Math.clamp(teamAttribute(2), 1, 20));
    }

    // =========================================================================
    // CHANGE NAMES  (subroutines 9400 / 9500)
    // =========================================================================
    private static void changeTeamNames() {
        String ans = inputLine("Change team names? (y/n): ", 0).toUpperCase();
        if (!ans.equals("Y") && !ans.equals("YES")) return;
        while (true) {
            int n = inputInt("Team number to rename (1-64, 99 to stop): ");
            if (n == 99) break;
            if (n < 1 || n > MAX_TEAMS) continue;
            System.out.println("Current name: " + team(n).getName());
            String name = inputLine("New name (max 10 chars): ", 10);
            if (!name.isEmpty()) team(n).setName(name);
        }
    }

    private static void changePlayerNames() {
        String ans = inputLine("Change player names? (y/n): ", 0).toUpperCase();
        if (!ans.equals("Y") && !ans.equals("YES")) return;
        while (true) {
            int n = inputInt("Player number to rename (1-24, 99 to stop): ");
            if (n == 99) break;
            if (n < 1 || n > MAX_PLAYERS) continue;
            System.out.println("Current name: " + player(n).getName());
            String name = inputLine("New name (max 8 chars): ", 8);
            if (!name.isEmpty()) player(n).setName(name);
        }
    }

    // =========================================================================
    // UTILITY
    // =========================================================================
    static void cls() {
        // Clear screen best-effort for terminals
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private static void waitEnter() {
        System.out.print("  [Press ENTER to continue]");
        SC.nextLine();
    }

    static String inputLine(String prompt, int maxLen) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine().trim();
            if (maxLen > 0 && s.length() > maxLen) {
                System.out.println("Too long (max " + maxLen + " chars).");
                continue;
            }
            return s;
        }
    }

    static int inputInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.println("Please enter a number."); }
        }
    }

    private static String randomTeamName() {
        return team(r(MAX_TEAMS)).getName();
    }

    private static Player player(int number) {
        return players[number];
    }

    private static Team team(int number) {
        return teams[number];
    }

    private static int statusCode(int playerNumber) {
        return switch (player(playerNumber).getStatus()) {
            case NOT_IN_SQUAD -> 0;
            case SQUAD -> 1;
            case PLAYING -> 2;
            case INJURED -> 3;
        };
    }

    private static void setPlayerStatus(int playerNumber, int status) {
        PlayerStatus playerStatus = switch (status) {
            case 0 -> PlayerStatus.NOT_IN_SQUAD;
            case 1 -> PlayerStatus.SQUAD;
            case 2 -> PlayerStatus.PLAYING;
            case 3 -> PlayerStatus.INJURED;
            default -> throw new IllegalArgumentException("Unknown player status: " + status);
        };
        player(playerNumber).setStatus(playerStatus);
        if (playerStatus == PlayerStatus.NOT_IN_SQUAD && myTeam > 0) {
            team(myTeam).removePlayer(player(playerNumber));
        }
    }

    private static int teamAttribute(int attribute) {
        TeamAttributes attributes = team(myTeam).getAttributes();
        return switch (attribute) {
            case 1 -> attributes.energy();
            case 2 -> attributes.morale();
            case 3 -> attributes.defence();
            case 4 -> attributes.midfield();
            case 5 -> attributes.attack();
            default -> throw new IllegalArgumentException("Unknown team attribute: " + attribute);
        };
    }

    private static void setTeamAttribute(int attribute, int value) {
        if (attribute < 1 || attribute > 5) {
            throw new IllegalArgumentException("Unknown team attribute: " + attribute);
        }
        value = Math.clamp(value, 1, 20);
        TeamAttributes attributes = team(myTeam).getAttributes();
        int energy = attribute == 1 ? value : attributes.energy();
        int morale = attribute == 2 ? value : attributes.morale();
        int defence = attribute == 3 ? value : attributes.defence();
        int midfield = attribute == 4 ? value : attributes.midfield();
        int attack = attribute == 5 ? value : attributes.attack();
        team(myTeam).setAttributes(new TeamAttributes(energy, morale, defence, midfield, attack));
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

    private static LeagueStanding standing(int localTeamNumber) {
        return currentLeague.getStanding(team(divOffset + localTeamNumber));
    }

    private static int balance() {
        return team(myTeam).getBalance();
    }

    private static int debt() {
        return team(myTeam).getLoan();
    }

    private static void adjustFinances(int balanceChange, int debtChange) {
        team(myTeam).setFinances(balance() + balanceChange, debt() + debtChange);
    }

    private static void moveManagedTeamToDivision() {
        Team managedTeam = team(myTeam);
        int previousDivision = managedTeam.getDivision();
        int newIndex = (division - 1) * DIV_SIZE + DIV_SIZE;
        if (myTeam != newIndex) {
            Team displacedTeam = team(newIndex);
            teams[newIndex] = managedTeam;
            teams[myTeam] = displacedTeam;
            displacedTeam.setDivision(previousDivision);
            myTeam = newIndex;
        }
        managedTeam.setDivision(division);
    }
}
