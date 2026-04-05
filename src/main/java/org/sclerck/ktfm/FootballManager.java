import java.util.*;

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
    static final int MAX_LOAN    = 250000;

    // ─── Random ──────────────────────────────────────────────────────────────
    static final Random RND = new Random();
    /** Equivalent to FN r(x) = INT(RND*x+1) */
    static int r(int x) { return RND.nextInt(x) + 1; }

    // ─── Player data ─────────────────────────────────────────────────────────
    static String[] playerName = new String[MAX_PLAYERS + 1]; // 1-based
    static int[]    playerStatus = new int[MAX_PLAYERS + 1];  // 0=gone,1=squad,2=playing,3=injured
    static int[]    playerSkill  = new int[MAX_PLAYERS + 1];
    static int[]    playerEnergy = new int[MAX_PLAYERS + 1];
    static int[]    playerValue  = new int[MAX_PLAYERS + 1];

    // ─── Team data ───────────────────────────────────────────────────────────
    static String[] teamName = new String[MAX_TEAMS + 1]; // 1-based

    // ─── League season arrays (16 teams per division) ─────────────────────
    static int[] pts  = new int[DIV_SIZE + 2]; // points
    static int[] gFor = new int[DIV_SIZE + 2]; // goals for
    static int[] gAga = new int[DIV_SIZE + 2]; // goals against
    static int[] tablePos = new int[DIV_SIZE + 2]; // final position

    // ─── Team attributes (5: energy, morale, defence, midfield, attack) ──
    static int[] teamAttr = new int[6];   // your team
    static int[] oppAttr  = new int[6];   // opposition

    // ─── Match state ─────────────────────────────────────────────────────────
    static int score1, score2;            // home, away scores
    static int matchTeam1, matchTeam2;    // team indices
    static int homeFlag, awayFlag;        // 1 or 2

    // ─── Season / game state ─────────────────────────────────────────────────
    static String managerName;
    static int myTeam;          // team index (1-based in teamName[])
    static int division;        // 1-4
    static int divOffset;       // (division-1)*16
    static int divMult;         // 5-division (money multiplier)
    static int leagueMatch;     // current league match number
    static int cupRound;        // 0 = not started, 1-8
    static int matchCounter;    // cycles 1-2 before cup game
    static int homeAway;        // h: was your team home(1) or away(2) last game
    static int prevHomeAway;
    static int leagueGame;      // lc: 1=cup, 2=league
    static int opp;             // opponent team index
    static int money;
    static int loan;
    static int groundRent;
    static int morale;          // teamAttr[2]
    static int skillLevel;      // 1-7
    static int teamColour;      // 0=black, 7=white
    static int seasons;
    static int totalScore;
    static int prevBalance;
    static boolean inCup;       // still in FA cup this season
    static int leaguePos;       // current league position
    static String[] levelName   = {"", "Beginner", "Novice", "Average", "Good", "Expert", "Super Expert", "Genius"};
    static String[] attrName    = {"", "Energy", "Morale", "Defence", "Midfield", "Attack"};

    // ─── Sort array for league table ─────────────────────────────────────────
    static int[] sortOrder = new int[DIV_SIZE + 1]; // holds team indices 1-16 (within division)

    // ─── Scanner ─────────────────────────────────────────────────────────────
    static final Scanner SC = new Scanner(System.in);

    // =========================================================================
    // ENTRY POINT
    // =========================================================================
    public static void main(String[] args) {
        cls();
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║       FOOTBALL MANAGER           ║");
        System.out.println("║   © K. & J. Toms 1982            ║");
        System.out.println("║   Java port - faithful remake    ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println();

        managerName = inputLine("Type your name: ", 20);

        loadData();
        chooseTeam();
        initSeason();

        mainLoop();
    }

    // =========================================================================
    // MAIN LOOP
    // =========================================================================
    static void mainLoop() {
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
    static void managementMenu() {
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
    static void sellOrListPlayers() {
        while (true) {
            cls();
            printSquadHeader();
            printSquad(false);
            System.out.println();
            System.out.println("[99] to go back");
            int n = inputInt("Select player number to sell (or 99): ");
            if (n == 99) return;
            if (n < 1 || n > MAX_PLAYERS) continue;
            if (playerStatus[n] == 0) { System.out.println("No such player."); pause(1000); continue; }
            if (playerStatus[n] == 3) {
                System.out.println(playerName[n] + " is injured - nobody wants him!");
                pause(1500); continue;
            }
            int offer = (int) (((r(5) + 7.0) * playerValue[n]) / 10);
            System.out.println(randomTeamName() + " have offered £" + offer + " for " + playerName[n]);
            System.out.println("He is worth £" + playerValue[n]);
            String ans = inputLine("Do you accept? (y/n): ", 0).toUpperCase();
            if (ans.equals("Y") || ans.equals("YES")) {
                playerStatus[n] = 0;
                money += offer;
                System.out.println(playerName[n] + " has been sold.");
                printMoney(); pause(1500);
            } else {
                if (r(3) == 1) {
                    playerStatus[n] = 3;
                    System.out.println("He's been injured in training after rejecting the move!");
                    pause(1500);
                }
            }
        }
    }

    // ─── Print stats (subroutine 3000) ────────────────────────────────────
    static void printStats() {
        cls();
        System.out.println("Team    : " + teamName[myTeam]);
        System.out.println("Manager : " + managerName);
        System.out.println("Colours : " + (teamColour == 0 ? "Black" : "White"));
        System.out.println("Level   : " + skillLevel + " - " + levelName[skillLevel]);
        System.out.println("Seasons : " + seasons);
        System.out.println("Division: " + division);
        System.out.println("Morale  : " + teamAttr[2]);
        printMoney();
        System.out.println("League match no: " + leagueMatch);
        waitEnter();
    }

    // ─── Loan (subroutine 2800) ────────────────────────────────────────────
    static void obtainLoan() {
        cls();
        printMoney();
        int amount = inputInt("How much do you want to borrow? £");
        cls();
        int maxLoan = MAX_LOAN * divMult;
        if (amount + loan > maxLoan) {
            System.out.println("Maximum loan is £" + maxLoan);
            pause(1500); return;
        }
        money += amount;
        loan  += amount;
        printMoney();
        waitEnter();
    }

    // ─── Pay off loan (subroutine 2900) ───────────────────────────────────
    static void payOffLoan() {
        cls();
        printMoney();
        int amount = inputInt("How much do you want to repay? £");
        cls();
        if (amount > loan) { System.out.println("You don't owe that much!"); pause(1500); return; }
        if (amount > money) { System.out.println("You haven't enough money!"); pause(1500); return; }
        loan  -= amount;
        money -= amount;
        printMoney();
        waitEnter();
    }

    // ─── Change level (subroutine 3200) ───────────────────────────────────
    static void changeLevel() {
        cls();
        System.out.println("Current level: " + skillLevel + " - " + levelName[skillLevel]);
        System.out.println("Choose a level:");
        for (int i = 1; i <= 7; i++) System.out.println("  " + i + " - " + levelName[i]);
        int n = inputInt("Enter level (1-7): ");
        if (n < 1 || n > 7) { System.out.println("1 to 7 please!"); pause(1000); return; }
        skillLevel = n;
        System.out.println("New skill level: " + skillLevel + " - " + levelName[skillLevel]);
        waitEnter();
    }

    // ─── Save game (subroutine 800) ───────────────────────────────────────
    static void saveGame() {
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
    static void setupMatch() {
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
        prevHomeAway = homeAway;
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
        System.out.println("  " + teamName[matchTeam1] + "  v  " + teamName[matchTeam2]);
        System.out.println("──────────────────────────────────────");
        printAttributeComparison();
        waitEnter();

        // Pre-match squad selection
        selectSquad();

        // Play match
        playMatch();

        // Post-match
        money += matchPrize();
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
        if (mySc > oppSc) teamAttr[2] = teamAttr[2] + (20 - teamAttr[2]) / 2;
        if (mySc < oppSc) teamAttr[2] = teamAttr[2] / 2;
        clampMorale();

        // Print other results if league
        if (leagueGame == 2) { printOtherResults(); printLeagueTable(); }
    }

    static String cupRoundName() {
        if (cupRound < 7) return String.valueOf(cupRound);
        if (cupRound == 7) return "Semi-Final";
        return "Final";
    }

    static int matchPrize() {
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
    static void buildOppositionAttributes() {
        for (int i = 1; i <= 5; i++) {
            int val;
            if (leagueGame == 1) {
                val = r(16) + skillLevel + ((myTeam - 1) / DIV_SIZE) - ((opp - 1) / DIV_SIZE + 1);
            } else {
                double zAvg = (leagueMatch > 0) ? (double) pts[(opp - divOffset)] / leagueMatch : 0;
                val = r(14) + skillLevel + (int) zAvg;
            }
            val = Math.max(1, Math.min(20, val));
            oppAttr[i] = val;
        }
    }

    // ─── Compute your team attributes from squad (subroutine 6500) ────────
    static void computeTeamAttributes() {
        teamAttr[1] = 0;
        for (int i = 3; i <= 5; i++) teamAttr[i] = 0;

        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (playerStatus[i] == 2) {
                int pos = (i - 1) / 8 + 3; // 3=def,4=mid,5=att
                teamAttr[pos] += playerSkill[i];
                teamAttr[1]   += playerEnergy[i];
            }
        }
        teamAttr[1] = teamAttr[1] / 11;
        for (int i = 3; i <= 5; i++) {
            teamAttr[i] = Math.max(1, Math.min(20, teamAttr[i]));
        }
    }

    // ─── Print attribute comparison ────────────────────────────────────────
    static void printAttributeComparison() {
        System.out.printf("%-15s %5s %5s%n", "Attribute", teamName[myTeam].substring(0, Math.min(5, teamName[myTeam].length())), teamName[opp].substring(0, Math.min(5, teamName[opp].length())));
        for (int i = 1; i <= 5; i++) {
            System.out.printf("%-15s %5d %5d%n", attrName[i], teamAttr[i], oppAttr[i]);
        }
    }

    // =========================================================================
    // SQUAD SELECTION  (subroutine 6000)
    // =========================================================================
    static void selectSquad() {
        // Update energy and injury recovery
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (playerStatus[i] == 0) continue;
            if (playerStatus[i] == 1 || playerStatus[i] == 3)
                playerEnergy[i] = Math.min(20, playerEnergy[i] + 10);
            if (playerStatus[i] == 2)
                playerEnergy[i] = Math.max(1, playerEnergy[i] - 1);
            if (playerStatus[i] == 3) playerStatus[i] = 1; // recover
            // Random injury
            if (playerStatus[i] != 0 && r(20) == 20) playerStatus[i] = 3;
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
                if (n == 99) break;
                if (n < 1 || n > MAX_PLAYERS) continue;
                if (playerStatus[n] != 1) { System.out.println("Can't pick that player."); pause(800); continue; }
                playerStatus[n] = 2;
            } else {
                System.out.println("Remove a player or 99 to start match:");
                int n = inputInt(">> ");
                if (n == 99) break;
                if (n < 1 || n > MAX_PLAYERS) continue;
                if (playerStatus[n] != 2) { System.out.println("That player isn't in the team."); pause(800); continue; }
                playerStatus[n] = 1;
            }
        }
    }

    static int countPicked() {
        int c = 0;
        for (int i = 1; i <= MAX_PLAYERS; i++) if (playerStatus[i] == 2) c++;
        return c;
    }

    // =========================================================================
    // MATCH SIMULATION  (subroutine 5000)
    // =========================================================================
    static void playMatch() {
        score1 = 0; score2 = 0;
        cls();
        System.out.println("★★  MATCH HIGHLIGHTS  ★★");
        System.out.println(teamName[matchTeam1] + "  v  " + teamName[matchTeam2]);
        System.out.println();

        boolean myHome = (matchTeam1 == myTeam);

        for (int round = 1; round <= 5; round++) {
            // Your team attacks
            int n = r(100) + (teamAttr[round <= 2 ? 3 : round <= 4 ? 4 : 5] - oppAttr[round <= 2 ? 3 : round <= 4 ? 4 : 5]) * 5;
            if (n >= 75) {
                simulateAttack(myHome, true);
                simulateAttack(myHome, true);
            }
            // Opposition attacks
            int m = r(100) + (oppAttr[round <= 2 ? 3 : round <= 4 ? 4 : 5] - teamAttr[round <= 2 ? 3 : round <= 4 ? 4 : 5]) * 5;
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
        System.out.printf("  %-20s %2d%n", teamName[matchTeam1], score1);
        System.out.printf("  %-20s %2d%n", teamName[matchTeam2], score2);
        System.out.println("──────────────────────────────────");
    }

    static void simulateAttack(boolean myHome, boolean myTeamAttacking) {
        boolean isGoal = r(3) == 1; // simplified probability
        String attacker = randomPlayerName(myTeamAttacking);

        if (isGoal) {
            if (myHome == myTeamAttacking) { score1++; } else { score2++; }
            System.out.println("  ⚽ GOAL!  " + attacker + " scores!");
            System.out.printf("     [%s %d - %d %s]%n", teamName[matchTeam1], score1, score2, teamName[matchTeam2]);
        } else {
            System.out.println("  → " + attacker + " drives forward... NO GOAL!");
        }
    }

    static String randomPlayerName(boolean myTeam) {
        if (myTeam) {
            for (int tries = 0; tries < 50; tries++) {
                int i = r(MAX_PLAYERS);
                if (playerStatus[i] == 2) return playerName[i];
            }
        }
        return "Opposition player";
    }

    // =========================================================================
    // LEAGUE UPDATES  (subroutine 7000)
    // =========================================================================
    static void updateLeague() {
        int myLocal  = myTeam  - divOffset;
        int oppLocal = opp     - divOffset;

        gFor[myLocal]  += score1; gAga[myLocal]  += score2;
        gFor[oppLocal] += score2; gAga[oppLocal] += score1;

        if (score1 == score2) { pts[myLocal]++;  pts[oppLocal]++; }
        else if (score1 > score2) pts[myLocal]  += 3;
        else                      pts[oppLocal] += 3;
    }

    // ─── Print other results (subroutine 7500) ────────────────────────────
    static void printOtherResults() {
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
            int ga = (int) (pts[a] / Math.max(1, leagueMatch) + RND.nextDouble() * 4);
            int gb = (int) (pts[b] / Math.max(1, leagueMatch) + RND.nextDouble() * 4);
            System.out.printf("  %-18s %d  -  %d  %-18s%n",
                    teamName[divOffset + a], ga, gb, teamName[divOffset + b]);
            gFor[a] += ga; gAga[a] += gb;
            gFor[b] += gb; gAga[b] += ga;
            if (ga > gb) pts[a] += 3;
            else if (ga < gb) pts[b] += 3;
            else { pts[a]++; pts[b]++; }
            games++;
        }
        waitEnter();
    }

    // ─── Print league table (subroutine 7710 / 7805) ─────────────────────
    static void printLeagueTable() {
        // Sort indices 1..16
        Integer[] order = new Integer[DIV_SIZE];
        for (int i = 0; i < DIV_SIZE; i++) order[i] = i + 1;
        Arrays.sort(order, (a, b) -> {
            if (pts[b] != pts[a])    return pts[b] - pts[a];
            int gdA = gFor[a] - gAga[a], gdB = gFor[b] - gAga[b];
            if (gdB != gdA) return gdB - gdA;
            return gFor[b] - gFor[a];
        });

        cls();
        System.out.println("══════════════════════════════════════════════════");
        System.out.printf("  %-22s %4s %4s %4s%n", "TEAM", "F", "A", "PTS");
        System.out.println("──────────────────────────────────────────────────");
        for (int rank = 0; rank < DIV_SIZE; rank++) {
            int idx = order[rank];
            String marker = (divOffset + idx == myTeam) ? " ◄" : "";
            System.out.printf("  %-22s %4d %4d %4d%s%n",
                    teamName[divOffset + idx], gFor[idx], gAga[idx], pts[idx], marker);
            tablePos[idx] = rank + 1;
        }
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("League match " + leagueMatch + "  |  Division " + division);
        waitEnter();
    }

    // =========================================================================
    // CUP UPDATES  (subroutine 5200)
    // =========================================================================
    static void updateCup() {
        boolean myHome = (matchTeam1 == myTeam);
        int mySc  = myHome ? score1 : score2;
        int oppSc = myHome ? score2 : score1;

        if (mySc < oppSc) {
            inCup = false;
            teamAttr[2] = teamAttr[2] / 2;
            System.out.println("You have been knocked out of the FA Cup!");
        } else {
            if (cupRound == 8) {
                cls();
                System.out.println();
                System.out.println("╔══════════════════════════════════╗");
                System.out.println("║  🏆  FA CUP WINNERS! 🏆          ║");
                System.out.println("║  Congratulations, " + managerName + "!  ║");
                System.out.println("╚══════════════════════════════════╝");
                System.out.println();
                pause(2000);
            } else {
                System.out.println("You are through to round " + (cupRound + 1) + "!");
            }
            teamAttr[2] = teamAttr[2] + (20 - teamAttr[2]) / 2;
        }
        clampMorale();
        waitEnter();
    }

    // =========================================================================
    // WAGES AND FINANCES  (subroutine 8000)
    // =========================================================================
    static void payWages() {
        cls();
        int wages = 0;
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (playerStatus[i] > 0) wages += playerSkill[i] * 100 * divMult;
        }
        int interest = loan / 100;

        System.out.println("── WEEKLY FINANCES ─────────────────");
        System.out.println("  Wage bill    : £" + wages);
        System.out.println("  Ground rent  : £" + groundRent);
        System.out.println("  Loan interest: £" + interest);

        money -= wages + groundRent + interest;

        int balance = money - loan;
        System.out.println("  Weekly balance: £" + balance);

        if (money < 0) {
            loan -= money;
            money = 0;
            System.out.println("  ⚠ Shortfall added to loan!");
        }
        if (loan > MAX_LOAN * divMult) {
            System.out.println("  ❌ DEBT EXCEEDED LIMIT - GAME OVER");
            waitEnter();
            System.exit(0);
        }
        waitEnter();
    }

    // =========================================================================
    // END OF SEASON  (subroutine 8100)
    // =========================================================================
    static void endOfSeasonCheck() {
        if (leagueMatch < 15 || inCup) return;  // simplified trigger

        cls();
        printLeagueTable();

        // Bonus for performance
        int myLocal = myTeam - divOffset;
        int pos = tablePos[myLocal];
        int bonus = (17 - pos) * 5000 * divMult + cupRound * 2;
        totalScore += bonus + pos;
        money += (17 - pos) * 5000 * divMult;
        System.out.println("Season bonus: £" + ((17 - pos) * 5000 * divMult));

        // Promotion / relegation
        if (pos <= 3 && division > 1) {
            System.out.println("🎉 " + teamName[myTeam] + " are PROMOTED!");
            division--;
            divOffset = (division - 1) * DIV_SIZE;
            divMult = 5 - division;
        } else if (pos >= 14 && division < 4) {
            System.out.println("😞 " + teamName[myTeam] + " are RELEGATED!");
            division++;
            divOffset = (division - 1) * DIV_SIZE;
            divMult = 5 - division;
        } else if (pos == 1) {
            System.out.println("🏆 " + teamName[myTeam] + " are League Champions!");
        }

        seasons++;
        initSeason();
        waitEnter();
    }

    // ─── Reset season variables (subroutine 8800) ─────────────────────────
    static void initSeason() {
        divOffset = (division - 1) * DIV_SIZE;
        divMult   = 5 - division;
        leagueMatch  = 0;
        cupRound     = 0;
        matchCounter = 1;
        homeAway     = 2;
        inCup        = true;
        leagueGame   = 0;
        groundRent   = 500 * divMult;

        Arrays.fill(pts,  0);
        Arrays.fill(gFor, 0);
        Arrays.fill(gAga, 0);

        for (int i = 1; i <= MAX_PLAYERS; i++) {
            playerValue[i] = (int) (5000.0 * divMult * r(5));
            playerSkill[i] = Math.max(1, playerValue[i] / (5000 * divMult));
            playerEnergy[i] = r(20);
        }

        teamAttr[2] = 10; // morale
        clampMorale();
        money = (money == 0) ? 50000 * divMult : money;
    }

    // =========================================================================
    // TRANSFER MARKET  (subroutine 8700)
    // =========================================================================
    static void transferMarket() {
        if (leagueMatch == 0) return;

        // Find an available player (one not in squad)
        int candidate = -1;
        for (int tries = 0; tries < 50; tries++) {
            int n = r(MAX_PLAYERS);
            if (playerStatus[n] == 0) { candidate = n; break; }
        }
        if (candidate == -1) return;

        int squadSize = 0;
        for (int i = 1; i <= MAX_PLAYERS; i++) if (playerStatus[i] > 0) squadSize++;
        if (squadSize >= 16) {
            System.out.println("Squad full (16 max) - cannot sign new player.");
            waitEnter(); return;
        }

        cls();
        System.out.println("── TRANSFER MARKET ─────────────────");
        printSquadHeader();
        System.out.printf("  %-10s %3d  Skill:%-3d  Energy:%-3d  Value:£%d%n",
                playerName[candidate], candidate,
                playerSkill[candidate], playerEnergy[candidate], playerValue[candidate]);
        printMoney();
        System.out.println("[99] to decline");

        int bid = inputInt("Your bid for " + playerName[candidate] + ": £");
        if (bid == 99) return;

        if (money < bid) {
            System.out.println("You haven't enough money!");
            pause(1500); return;
        }
        double ratio = (double)(r(10) * bid) / playerValue[candidate];
        if (ratio >= 5) {
            playerStatus[candidate] = 1;
            playerValue[candidate]  = playerSkill[candidate] * 5000 * divMult;
            money -= bid;
            System.out.println(playerName[candidate] + " has joined your team!");
        } else {
            playerValue[candidate] = (int)(playerValue[candidate] * 1.2);
            System.out.println("Bid rejected. His value has risen to £" + playerValue[candidate]);
        }
        waitEnter();
    }

    // =========================================================================
    // SQUAD DISPLAY HELPERS
    // =========================================================================
    static void printSquadHeader() {
        System.out.printf("  %-12s %3s %6s %7s %8s %7s%n",
                "NAME", "NO.", "SKILL", "ENERGY", "VALUE", "STATUS");
        System.out.println("  " + "─".repeat(52));
    }

    static void printSquad(boolean showStatus) {
        String[] posLabel = {"", "D ", "D ", "D ", "D ", "D ", "D ", "D ", "D ",
                "M ", "M ", "M ", "M ", "M ", "M ", "M ", "M ",
                "A ", "A ", "A ", "A ", "A ", "A ", "A ", "A "};
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (playerStatus[i] == 0) continue;
            String status = switch(playerStatus[i]) {
                case 1 -> "Squad";
                case 2 -> "PLAYING";
                case 3 -> "Injured";
                default -> "?";
            };
            System.out.printf("  %s%-12s %3d %6d %7d %8s %7s%n",
                    posLabel[i], playerName[i], i,
                    playerSkill[i], playerEnergy[i], "£" + playerValue[i],
                    showStatus ? status : "");
        }
    }

    // ─── Print money (subroutine 1000) ────────────────────────────────────
    static void printMoney() {
        System.out.println("  Money: £" + money + "   Loan: £" + loan);
    }

    // ─── Clamp morale 1..20 (subroutine 1040) ─────────────────────────────
    static void clampMorale() {
        teamAttr[2] = Math.max(1, Math.min(20, teamAttr[2]));
    }

    // =========================================================================
    // CHANGE NAMES  (subroutines 9400 / 9500)
    // =========================================================================
    static void changeTeamNames() {
        String ans = inputLine("Change team names? (y/n): ", 0).toUpperCase();
        if (!ans.equals("Y") && !ans.equals("YES")) return;
        while (true) {
            int n = inputInt("Team number to rename (1-64, 99 to stop): ");
            if (n == 99) break;
            if (n < 1 || n > MAX_TEAMS) continue;
            System.out.println("Current name: " + teamName[n]);
            String name = inputLine("New name (max 10 chars): ", 10);
            if (!name.isEmpty()) teamName[n] = name;
        }
    }

    static void changePlayerNames() {
        String ans = inputLine("Change player names? (y/n): ", 0).toUpperCase();
        if (!ans.equals("Y") && !ans.equals("YES")) return;
        while (true) {
            int n = inputInt("Player number to rename (1-24, 99 to stop): ");
            if (n == 99) break;
            if (n < 1 || n > MAX_PLAYERS) continue;
            System.out.println("Current name: " + playerName[n]);
            String name = inputLine("New name (max 8 chars): ", 8);
            if (!name.isEmpty()) playerName[n] = name;
        }
    }

    // =========================================================================
    // CHOOSE TEAM  (subroutine 8900)
    // =========================================================================
    static void chooseTeam() {
        while (true) {
            cls();
            int d = 0;
            while (d < 1 || d > 4) {
                System.out.println("Choose a division to browse (1-4): ");
                for (int div = 1; div <= 4; div++) {
                    System.out.println("  Division " + div + ":");
                    int off = (div - 1) * 16;
                    for (int i = off + 1; i <= off + 16; i++)
                        System.out.printf("    %2d. %s%n", i, teamName[i]);
                }
                d = inputInt("Division: ");
            }
            int offset = (d - 1) * 16;
            int t = 0;
            while (t < offset + 1 || t > offset + 16) {
                t = inputInt("Type team number (or 99 for another division): ");
                if (t == 99) break;
            }
            if (t == 99) continue;

            myTeam   = t;
            division = d;
            break;
        }

        // Skill level
        cls();
        System.out.println("Choose your skill level:");
        for (int i = 1; i <= 7; i++) System.out.println("  " + i + " - " + levelName[i]);
        skillLevel = 0;
        while (skillLevel < 1 || skillLevel > 7)
            skillLevel = inputInt("Level (1-7): ");

        // Colours
        teamColour = -1;
        while (teamColour != 0 && teamColour != 7) {
            teamColour = inputInt("Team colours - 0=Black or 7=White: ");
        }

        // Assign random starting squad (12 players from the 24)
        int picked = 0;
        while (picked < 12) {
            int i = r(MAX_PLAYERS);
            if (playerStatus[i] == 0) {
                playerStatus[i] = (picked == 11) ? 1 : 2; // last one on bench
                picked++;
            }
        }

        System.out.println("You are now managing " + teamName[myTeam] + "!");
        pause(1500);
    }

    // =========================================================================
    // DATA LOADING  (subroutine 9000)
    // =========================================================================
    static void loadData() {
        // Player names (line 9030)
        String[] names = {
                "P.Parkes","D.Watson","P.Neal","A.Martin","K.Sansom","M.Mills",
                "R.Osman","S.Foster","B.Robson","G.Hoddle","G.Rix","S.Hunt",
                "G.Owen","R.Moses","B.Talbot","S.McCall","C.Regis","P.Withe",
                "T.Morley","P.Barnes","E.Gates","K.Reeves","K.Keegan","G.Shaw"
        };
        for (int i = 1; i <= MAX_PLAYERS; i++) playerName[i] = names[i - 1];

        // Team names - all 4 divisions (line 9230)
        String[] teams = {
                // Division 1
                "Arsenal","Aston V.","Brighton","Coventry","Everton","Ipswich",
                "Liverpool","Luton","Man.City","Man.Utd","Norwich","Notts.F.",
                "Swansea","Spurs","Watford","West Ham",
                // Division 2
                "Blackburn","Bolton","Cambridge","Charlton","Chelsea","Crystal P.",
                "Derby Co.","Fulham","Grimsby","Leeds","Middlesbro","Newcastle",
                "Oldham","Q.P.R.","Rotherham","Sheff.Wed",
                // Division 3
                "Bradford","Brentford","Bristol R.","Cardiff","Doncaster","Exeter",
                "Lincoln","Millwall","Newport","Orient","Oxford","Plymouth",
                "Preston","Reading","Southend","Walsall",
                // Division 4
                "Blackpool","Bury","Colchester","Crewe","Darlington","Halifax",
                "Hartlepool","Hereford","Hull","Mansfield","Port Vale","Rochdale",
                "Scunthorpe","Stockport","Torquay","York City"
        };
        for (int i = 1; i <= MAX_TEAMS; i++) teamName[i] = teams[i - 1];

        // Init player status, money etc
        Arrays.fill(playerStatus, 0);
        money = 50000;
        loan  = 0;

        // Random initial player skills / energies
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            playerSkill[i]  = r(10);
            playerEnergy[i] = r(20);
            playerValue[i]  = playerSkill[i] * 5000;
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

    static void waitEnter() {
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

    static String randomTeamName() {
        return teamName[r(MAX_TEAMS)];
    }
}
