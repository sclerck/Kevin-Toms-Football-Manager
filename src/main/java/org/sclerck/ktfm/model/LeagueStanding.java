package org.sclerck.ktfm.model;

public final class LeagueStanding {
    private final Team team;
    private int played;
    private int wins;
    private int draws;
    private int losses;
    private int goalsFor;
    private int goalsAgainst;
    private int points;

    LeagueStanding(Team team) {
        this.team = team;
    }

    public Team getTeam() { return team; }
    public int getPlayed() { return played; }
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public int getGoalDifference() { return goalsFor - goalsAgainst; }
    public int getPoints() { return points; }

    void record(int goalsScored, int goalsConceded) {
        played++;
        goalsFor += goalsScored;
        goalsAgainst += goalsConceded;
        if (goalsScored > goalsConceded) {
            wins++;
            points += 3;
        } else if (goalsScored == goalsConceded) {
            draws++;
            points++;
        } else {
            losses++;
        }
    }
}
