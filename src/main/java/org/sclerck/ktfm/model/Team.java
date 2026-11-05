package org.sclerck.ktfm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Team {
    public static final int MAX_SQUAD_SIZE = 16;

    private String name;
    private int division;
    private final List<Player> players = new ArrayList<>();
    private TeamAttributes attributes = new TeamAttributes(1, 10, 1, 1, 1);
    private TeamColor color = TeamColor.BLACK;
    private int balance;
    private int loan;

    public Team(String name, int division) {
        setName(name);
        if (division < 1 || division > 4) {
            throw new IllegalArgumentException("Division must be between 1 and 4");
        }
        this.division = division;
    }

    public String getName() { return name; }
    public int getDivision() { return division; }
    public List<Player> getPlayers() { return List.copyOf(players); }
    public List<Player> getStartingPlayers() {
        return players.stream().filter(player -> player.getStatus() == PlayerStatus.PLAYING).toList();
    }
    public TeamAttributes getAttributes() { return attributes; }
    public TeamColor getColor() { return color; }
    public int getBalance() { return balance; }
    public int getLoan() { return loan; }

    public void setName(String name) {
        this.name = Player.requireName(name, "Team name");
    }

    public void setDivision(int division) {
        if (division < 1 || division > 4) {
            throw new IllegalArgumentException("Division must be between 1 and 4");
        }
        this.division = division;
    }

    public void setAttributes(TeamAttributes attributes) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
    }

    public void setColor(TeamColor color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    public void setFinances(int balance, int loan) {
        if (balance < 0 || loan < 0) {
            throw new IllegalArgumentException("Balance and loan cannot be negative");
        }
        this.balance = balance;
        this.loan = loan;
    }

    public void addPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        if (player.getStatus() == PlayerStatus.NOT_IN_SQUAD) {
            throw new IllegalArgumentException("A player outside the squad cannot be added to a team");
        }
        if (players.size() >= MAX_SQUAD_SIZE) {
            throw new IllegalStateException("A team cannot have more than 16 players in its squad");
        }
        if (players.stream().anyMatch(existing -> existing.getSquadNumber() == player.getSquadNumber())) {
            throw new IllegalArgumentException("Squad numbers must be unique within a team");
        }
        players.add(player);
    }

    public boolean removePlayer(Player player) {
        if (!players.remove(player)) return false;
        player.setStatus(PlayerStatus.NOT_IN_SQUAD);
        return true;
    }
}
