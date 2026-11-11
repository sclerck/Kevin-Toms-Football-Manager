package org.sclerck.ktfm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.sclerck.ktfm.model.Manager;
import org.sclerck.ktfm.model.Player;
import org.sclerck.ktfm.model.PlayerStatus;
import org.sclerck.ktfm.model.Team;
import org.sclerck.ktfm.model.TeamColor;

import static org.sclerck.ktfm.FootballManager.MAX_PLAYERS;
import static org.sclerck.ktfm.FootballManager.levelName;
import static org.sclerck.ktfm.FootballManager.r;
import static org.sclerck.ktfm.Utils.cls;
import static org.sclerck.ktfm.Utils.inputInt;
import static org.sclerck.ktfm.Utils.inputLine;
import static org.sclerck.ktfm.Utils.pause;

/** Initial game setup: manager name, club and player data, team choice, skill level, colours and squad. */
final class GameEngine {

    /** Everything the game needs once setup is complete. Arrays are 1-based. */
    record Result(Player[] players, Map<Integer, List<Team>> teams, Manager manager, Team myTeam, int division) { }

    /** A team entry in efl.json. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record EflTeam(@JsonProperty("team_id") int id,
                   @JsonProperty("team_name") String name,
                   @JsonProperty("team_abbreviation") String abbreviation,
                   @JsonProperty("team_stadium") String stadium) { }

    private GameEngine() { }

    static Result run() {
        showTitle();
        String managerName = askManagerName();

        Player[] players = new Player[MAX_PLAYERS + 1];
        loadPlayers(players);
        Map<Integer, List<Team>> teams = loadTeams();

        Team team = chooseTeam(teams);
        int division = team.getDivision();

        int skillLevel = chooseSkillLevel();
        team.setColor(chooseColour());
        Finance.openAccount(team);
        assignStartingSquad(players, team);

        System.out.println("You are now managing " + team.getName() + "!");
        pause(1500);

        return new Result(players, teams, new Manager(managerName, team, skillLevel), team, division);
    }

    private static void showTitle() {
        cls();
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║       FOOTBALL MANAGER           ║");
        System.out.println("║   © K. & J. Toms 1982            ║");
        System.out.println("║   Java port - faithful remake    ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println();
    }

    private static String askManagerName() {
        String name;
        do {
            name = inputLine("Type your name: ", 20);
        } while (name.isBlank());
        return name;
    }

    // ─── Data loading (subroutine 9000) ───────────────────────────────────
    private static void loadPlayers(Player[] players) {
        // Player names (line 9030)
        String[] names = {
                "P.Parkes","D.Watson","P.Neal","A.Martin","K.Sansom","M.Mills",
                "R.Osman","S.Foster","B.Robson","G.Hoddle","G.Rix","S.Hunt",
                "G.Owen","R.Moses","B.Talbot","S.McCall","C.Regis","P.Withe",
                "T.Morley","P.Barnes","E.Gates","K.Reeves","K.Keegan","G.Shaw"
        };
        // Random initial player skills / energies
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            int skill = r(10);
            players[i] = new Player(i, names[i - 1], PlayerStatus.NOT_IN_SQUAD,
                    skill, r(20), skill * 5000);
        }
    }

    /**
     * Reads efl.json into a map of division ID to the teams in that division.
     * The file repeats the "division" and "teams" keys once per division inside a single
     * object, so it is read token by token rather than bound to a single tree.
     */
    private static Map<Integer, List<Team>> loadTeams() {
        ObjectMapper mapper = new ObjectMapper();
        Map<Integer, List<Team>> teams = new TreeMap<>();
        try (InputStream in = GameEngine.class.getResourceAsStream("/efl.json")) {
            if (in == null) throw new IllegalStateException("efl.json not found on the classpath");
            JsonParser parser = mapper.createParser(in);
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IllegalStateException("efl.json must contain a JSON object");
            }
            int division = 0;
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String field = parser.currentName();
                parser.nextToken();
                if (field.equals("division")) {
                    division = parser.getIntValue();
                } else if (field.equals("teams")) {
                    if (division == 0) throw new IllegalStateException("efl.json: teams listed before a division");
                    List<Team> divisionTeams = teams.computeIfAbsent(division, k -> new ArrayList<>());
                    while (parser.nextToken() == JsonToken.START_OBJECT) {
                        EflTeam t = mapper.readValue(parser, EflTeam.class);
                        divisionTeams.add(new Team(t.id(), t.name(), t.abbreviation(), t.stadium(), division));
                    }
                } else {
                    parser.skipChildren();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read efl.json", e);
        }
        return teams;
    }

    // ─── Choose team (subroutine 8900) ────────────────────────────────────
    private static Team chooseTeam(Map<Integer, List<Team>> teams) {
        while (true) {
            cls();
            int d = 0;
            while (!teams.containsKey(d)) {
                System.out.println("Choose a division to browse " + teams.keySet() + ": ");
                teams.forEach((division, divisionTeams) -> {
                    System.out.println("  Division " + division + ":");
                    for (Team team : divisionTeams)
                        System.out.printf("    %2d. %s%n", team.getId(), team.getName());
                });
                d = inputInt("Division: ");
            }
            List<Team> divisionTeams = teams.get(d);
            while (true) {
                int id = inputInt("Type team number (or 99 for another division): ");
                if (id == 99) break;
                for (Team team : divisionTeams) {
                    if (team.getId() == id) return team;
                }
            }
        }
    }
    private static int chooseSkillLevel() {
        cls();
        System.out.println("Choose your skill level:");
        for (int i = 1; i <= 7; i++) System.out.println("  " + i + " - " + levelName[i]);
        int level = 0;
        while (level < 1 || level > 7)
            level = inputInt("Level (1-7): ");
        return level;
    }

    private static TeamColor chooseColour() {
        int choice = -1;
        while (choice != 0 && choice != 7) {
            choice = inputInt("Team colours - 0=Black or 7=White: ");
        }
        return choice == 0 ? TeamColor.BLACK : TeamColor.WHITE;
    }

    // Assign random starting squad (12 players from the 24)
    private static void assignStartingSquad(Player[] players, Team team) {
        int picked = 0;
        while (picked < 12) {
            Player player = players[r(MAX_PLAYERS)];
            if (player.getStatus() == PlayerStatus.NOT_IN_SQUAD) {
                player.setStatus(picked == 11 ? PlayerStatus.SQUAD : PlayerStatus.PLAYING); // last one on bench
                team.addPlayer(player);
                picked++;
            }
        }
    }
}
