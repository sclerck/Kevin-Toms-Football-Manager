package org.sclerck.ktfm;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.sclerck.ktfm.model.League;
import org.sclerck.ktfm.model.LeagueStanding;
import org.sclerck.ktfm.model.Manager;
import org.sclerck.ktfm.model.Player;
import org.sclerck.ktfm.model.PlayerStatus;
import org.sclerck.ktfm.model.Team;
import org.sclerck.ktfm.model.TeamAttributes;
import org.sclerck.ktfm.model.TeamColor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.sclerck.ktfm.Finance.printMoney;

/**
 * Football Manager - Java port of Kevin Toms' classic 1982 ZX Spectrum game.
 * Original: Copyright K. & J. Toms 1982
 * Java port preserves all original game logic and data.
 *
 * Starts the interactive console game once the Spring context is ready.
 */
@Component
@ConditionalOnProperty(name = "ktfm.console.enabled", havingValue = "true", matchIfMissing = true)
public class FootballManager implements CommandLineRunner {

    // ─── Constants ───────────────────────────────────────────────────────────
    static final int MAX_PLAYERS = 24;

    // ─── Random ──────────────────────────────────────────────────────────────
    static final Random RND = new Random();
    /** Equivalent to FN r(x) = INT(RND*x+1) */
    static int r(int x) { return RND.nextInt(x) + 1; }

    // ─── Player data ─────────────────────────────────────────────────────────
    private static Player[] players = new Player[MAX_PLAYERS + 1]; // 1-based

    // ─── Team data ───────────────────────────────────────────────────────────
    static Map<Integer, List<Team>> teams = Map.of(); // division ID -> teams in that division

    // ─── League season ────────────────────────────────────────────────────
    static League currentLeague;

    // ─── Season / game state ─────────────────────────────────────────────────
    static Manager manager;
    static Team myTeam;         // the team being managed
    static int division;        // 1-4
    static int divMult;         // 5-division (money multiplier)
    static int skillLevel;      // 1-7
    static final String[] levelName   = {"", "Beginner", "Novice", "Average", "Good", "Expert", "Super Expert", "Genius"};

    @Override
    public void run(String... args) {
        play();
    }

    // =========================================================================
    // ENTRY POINT
    // =========================================================================
    private void play() {
        GameEngine.Result setup = GameEngine.run();
        players = setup.players();
        teams = setup.teams();
        manager = setup.manager();
        myTeam = setup.myTeam();
        division = setup.division();
        skillLevel = manager.getSkillLevel();

        SeasonEngine.startSeason();

        mainLoop();
    }

    // =========================================================================
    // MAIN LOOP
    // =========================================================================
    private void mainLoop() {
        while (true) {
            managementMenu();
            MatchEngine.playNextMatch();
            Finance.payWages();
            SeasonEngine.endOfSeasonCheck();
            TransferMarket.transferMarket();
        }
    }

    // =========================================================================
    // MANAGEMENT MENU  (subroutine 2000)
    // =========================================================================
    private void managementMenu() {
        while (true) {
            Utils.cls();
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
            String cmd = Utils.inputLine("Enter command: ", 0).toUpperCase();

            switch (cmd) {
                case "A"      -> TransferMarket.sellOrListPlayers();
                case "S"      -> printStats();
                case "L"      -> Finance.obtainLoan(divMult);
                case "P"      -> Finance.payOffLoan();
                case "CHANGE" -> { changeTeamNames(); changePlayerNames(); }
                case "LEVEL"  -> changeLevel();
                case "KEEP"   -> { saveGame(); return; }
                case "99"     -> { return; }
            }
        }
    }

    // ─── Print stats (subroutine 3000) ────────────────────────────────────
    private void printStats() {
        Utils.cls();
        System.out.println("Team    : " + myTeam.getName());
        System.out.println("Manager : " + manager.getName());
        System.out.println("Colours : " + (myTeam.getColor() == TeamColor.BLACK ? "Black" : "White"));
        System.out.println("Level   : " + manager.getSkillLevel() + " - " + levelName[manager.getSkillLevel()]);
        System.out.println("Seasons : " + SeasonEngine.seasons);
        System.out.println("Division: " + division);
        System.out.println("Morale  : " + teamAttribute(2));
        printMoney();
        System.out.println("League match no: " + MatchEngine.leagueMatch);
        Utils.waitEnter();
    }

    // ─── Change level (subroutine 3200) ───────────────────────────────────
    private void changeLevel() {
        Utils.cls();
        System.out.println("Current level: " + skillLevel + " - " + levelName[skillLevel]);
        System.out.println("Choose a level:");
        for (int i = 1; i <= 7; i++) System.out.println("  " + i + " - " + levelName[i]);
        int n = Utils.inputInt("Enter level (1-7): ");
        if (n < 1 || n > 7) { System.out.println("1 to 7 please!"); Utils.pause(1000); return; }
        skillLevel = n;
        manager = new Manager(manager.getName(), myTeam, skillLevel);
        System.out.println("New skill level: " + skillLevel + " - " + levelName[skillLevel]);
        Utils.waitEnter();
    }

    // ─── Save game (subroutine 800) ───────────────────────────────────────
    private void saveGame() {
        System.out.println("(Save not implemented in this port - your game state is held in memory.)");
        String ans = Utils.inputLine("Do you want to play more? (y/n): ", 0).toUpperCase();
        if (!ans.equals("Y") && !ans.equals("YES")) {
            System.out.println("Thanks for playing Football Manager!");
            System.exit(0);
        }
    }

    // =========================================================================
    // SQUAD DISPLAY HELPERS
    // =========================================================================
    protected static void printSquadHeader() {
        System.out.printf("  %-12s %3s %6s %7s %8s %7s%n",
                "NAME", "NO.", "SKILL", "ENERGY", "VALUE", "STATUS");
        System.out.println("  " + "─".repeat(52));
    }

    protected static void printSquad(boolean showStatus) {
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

    // ─── Clamp morale 1..20 (subroutine 1040) ─────────────────────────────
    protected static void clampMorale() {
        setTeamAttribute(2, Math.clamp(teamAttribute(2), 1, 20));
    }

    // =========================================================================
    // CHANGE NAMES  (subroutines 9400 / 9500)
    // =========================================================================
    private static void changeTeamNames() {
        String ans = Utils.inputLine("Change team names? (y/n): ", 0).toUpperCase();
        if (!ans.equals("Y") && !ans.equals("YES")) return;
        while (true) {
            int d = Utils.inputInt("Division of team to rename (99 to stop): ");
            if (d == 99) break;
            List<Team> divisionTeams = teams.get(d);
            if (divisionTeams == null) continue;
            int id = Utils.inputInt("Team number within division: ");
            Team target = divisionTeams.stream().filter(team -> team.getId() == id).findFirst().orElse(null);
            if (target == null) continue;
            System.out.println("Current name: " + target.getName());
            String name = Utils.inputLine("New name (max 10 chars): ", 10);
            if (!name.isEmpty()) target.setName(name);
        }
    }
    private static void changePlayerNames() {
        String ans = Utils.inputLine("Change player names? (y/n): ", 0).toUpperCase();
        if (!ans.equals("Y") && !ans.equals("YES")) return;
        while (true) {
            int n = Utils.inputInt("Player number to rename (1-24, 99 to stop): ");
            if (n == 99) break;
            if (n < 1 || n > MAX_PLAYERS) continue;
            System.out.println("Current name: " + player(n).getName());
            String name = Utils.inputLine("New name (max 8 chars): ", 8);
            if (!name.isEmpty()) player(n).setName(name);
        }
    }

    static Player player(int number) {
        return players[number];
    }

    static List<Team> allTeams() {
        List<Team> all = new ArrayList<>();
        teams.values().forEach(all::addAll);
        return all;
    }

    static Team randomTeam() {
        List<Team> all = allTeams();
        return all.get(r(all.size()) - 1);
    }

    static int statusCode(int playerNumber) {
        return switch (player(playerNumber).getStatus()) {
            case NOT_IN_SQUAD -> 0;
            case SQUAD -> 1;
            case PLAYING -> 2;
            case INJURED -> 3;
        };
    }

    static void setPlayerStatus(int playerNumber, int status) {
        PlayerStatus playerStatus = switch (status) {
            case 0 -> PlayerStatus.NOT_IN_SQUAD;
            case 1 -> PlayerStatus.SQUAD;
            case 2 -> PlayerStatus.PLAYING;
            case 3 -> PlayerStatus.INJURED;
            default -> throw new IllegalArgumentException("Unknown player status: " + status);
        };
        player(playerNumber).setStatus(playerStatus);
        if (playerStatus == PlayerStatus.NOT_IN_SQUAD && myTeam != null) {
            myTeam.removePlayer(player(playerNumber));
        }
    }

    static int teamAttribute(int attribute) {
        TeamAttributes attributes = myTeam.getAttributes();
        return switch (attribute) {
            case 1 -> attributes.energy();
            case 2 -> attributes.morale();
            case 3 -> attributes.defence();
            case 4 -> attributes.midfield();
            case 5 -> attributes.attack();
            default -> throw new IllegalArgumentException("Unknown team attribute: " + attribute);
        };
    }

    static void setTeamAttribute(int attribute, int value) {
        if (attribute < 1 || attribute > 5) {
            throw new IllegalArgumentException("Unknown team attribute: " + attribute);
        }
        value = Math.clamp(value, 1, 20);
        TeamAttributes attributes = myTeam.getAttributes();
        int energy = attribute == 1 ? value : attributes.energy();
        int morale = attribute == 2 ? value : attributes.morale();
        int defence = attribute == 3 ? value : attributes.defence();
        int midfield = attribute == 4 ? value : attributes.midfield();
        int attack = attribute == 5 ? value : attributes.attack();
        myTeam.setAttributes(new TeamAttributes(energy, morale, defence, midfield, attack));
    }

    static LeagueStanding standing(Team team) {
        return currentLeague.getStanding(team);
    }
}

