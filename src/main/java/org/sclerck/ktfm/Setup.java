package org.sclerck.ktfm;

import org.sclerck.ktfm.model.Manager;
import org.sclerck.ktfm.model.Player;
import org.sclerck.ktfm.model.PlayerStatus;
import org.sclerck.ktfm.model.Team;
import org.sclerck.ktfm.model.TeamColor;

import static org.sclerck.ktfm.FootballManager.DIV_SIZE;
import static org.sclerck.ktfm.FootballManager.MAX_PLAYERS;
import static org.sclerck.ktfm.FootballManager.MAX_TEAMS;
import static org.sclerck.ktfm.FootballManager.cls;
import static org.sclerck.ktfm.FootballManager.inputInt;
import static org.sclerck.ktfm.FootballManager.inputLine;
import static org.sclerck.ktfm.FootballManager.levelName;
import static org.sclerck.ktfm.FootballManager.pause;
import static org.sclerck.ktfm.FootballManager.r;

/** Initial game setup: manager name, club and player data, team choice, skill level, colours and squad. */
final class Setup {

    /** Everything the game needs once setup is complete. Arrays are 1-based. */
    record Result(Player[] players, Team[] teams, Manager manager, int myTeam, int division) { }

    private Setup() { }

    static Result run() {
        showTitle();
        String managerName = askManagerName();

        Player[] players = new Player[MAX_PLAYERS + 1];
        Team[] teams = new Team[MAX_TEAMS + 1];
        loadData(players, teams);

        int[] choice = chooseTeam(teams);
        int myTeam = choice[0];
        int division = choice[1];
        Team team = teams[myTeam];

        int skillLevel = chooseSkillLevel();
        team.setColor(chooseColour());
        team.setFinances(50000, 0);
        assignStartingSquad(players, team);

        System.out.println("You are now managing " + team.getName() + "!");
        pause(1500);

        return new Result(players, teams, new Manager(managerName, team, skillLevel), myTeam, division);
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
    private static void loadData(Player[] players, Team[] teams) {
        // Player names (line 9030)
        String[] names = {
                "P.Parkes","D.Watson","P.Neal","A.Martin","K.Sansom","M.Mills",
                "R.Osman","S.Foster","B.Robson","G.Hoddle","G.Rix","S.Hunt",
                "G.Owen","R.Moses","B.Talbot","S.McCall","C.Regis","P.Withe",
                "T.Morley","P.Barnes","E.Gates","K.Reeves","K.Keegan","G.Shaw"
        };
        // Team names - all 4 divisions (line 9230)
        String[] teamNames = {
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
        // Create the domain objects for all clubs and players.
        for (int i = 1; i <= MAX_TEAMS; i++) {
            teams[i] = new Team(teamNames[i - 1], (i - 1) / DIV_SIZE + 1);
        }

        // Random initial player skills / energies
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            int skill = r(10);
            players[i] = new Player(i, names[i - 1], PlayerStatus.NOT_IN_SQUAD,
                    skill, r(20), skill * 5000);
        }
    }

    // ─── Choose team (subroutine 8900); returns {team number, division} ───
    private static int[] chooseTeam(Team[] teams) {
        while (true) {
            cls();
            int d = 0;
            while (d < 1 || d > 4) {
                System.out.println("Choose a division to browse (1-4): ");
                for (int div = 1; div <= 4; div++) {
                    System.out.println("  Division " + div + ":");
                    int off = (div - 1) * DIV_SIZE;
                    for (int i = off + 1; i <= off + DIV_SIZE; i++)
                        System.out.printf("    %2d. %s%n", i, teams[i].getName());
                }
                d = inputInt("Division: ");
            }
            int offset = (d - 1) * DIV_SIZE;
            int t = 0;
            while (t < offset + 1 || t > offset + DIV_SIZE) {
                t = inputInt("Type team number (or 99 for another division): ");
                if (t == 99) break;
            }
            if (t == 99) continue;
            return new int[] {t, d};
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
