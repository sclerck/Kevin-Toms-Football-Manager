package org.sclerck.ktfm;

import org.sclerck.ktfm.model.Match;

import static org.sclerck.ktfm.Finance.adjustFinances;
import static org.sclerck.ktfm.Finance.balance;
import static org.sclerck.ktfm.Finance.printMoney;
import static org.sclerck.ktfm.FootballManager.MAX_PLAYERS;

import static org.sclerck.ktfm.FootballManager.MAX_TEAMS;
import static org.sclerck.ktfm.FootballManager.divMult;
import static org.sclerck.ktfm.FootballManager.myTeam;
import static org.sclerck.ktfm.FootballManager.player;
import static org.sclerck.ktfm.FootballManager.printSquad;
import static org.sclerck.ktfm.FootballManager.printSquadHeader;
import static org.sclerck.ktfm.FootballManager.r;
import static org.sclerck.ktfm.FootballManager.setPlayerStatus;
import static org.sclerck.ktfm.FootballManager.statusCode;
import static org.sclerck.ktfm.FootballManager.team;
import static org.sclerck.ktfm.Utils.cls;
import static org.sclerck.ktfm.Utils.inputInt;
import static org.sclerck.ktfm.Utils.inputLine;
import static org.sclerck.ktfm.Utils.pause;
import static org.sclerck.ktfm.Utils.waitEnter;

/** Buying, selling and listing players. */
final class TransferMarket {

    private TransferMarket() { }

    // ─── Sell or list players (subroutine 2400) ───────────────────────────
    static void sellOrListPlayers(int myTeam) {
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

    private static String randomTeamName() {
        return team(r(MAX_TEAMS)).getName();
    }

    // ─── Buy a player (subroutine 8700) ───────────────────────────────────
    static void transferMarket() {
        if (MatchEngine.leagueMatch == 0) return;

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
}
