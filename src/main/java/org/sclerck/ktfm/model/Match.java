package org.sclerck.ktfm.model;

import java.util.Objects;
import java.util.Optional;

public final class Match {
    public static final int PLAYERS_PER_TEAM = 11;

    private final Team homeTeam;
    private final Team awayTeam;
    private final Competition competition;
    private final int round;
    private final int homeGoals;
    private final int awayGoals;

    public Match(Team homeTeam, Team awayTeam, Competition competition, int round,
                 int homeGoals, int awayGoals) {
        this(homeTeam, awayTeam, competition, round, homeGoals, awayGoals, true);
    }

    /** Creates a scored result for simulations that do not materialize both lineups. */
    public static Match simulated(Team homeTeam, Team awayTeam, Competition competition, int round,
                                  int homeGoals, int awayGoals) {
        return new Match(homeTeam, awayTeam, competition, round, homeGoals, awayGoals, false);
    }

    private Match(Team homeTeam, Team awayTeam, Competition competition, int round,
                  int homeGoals, int awayGoals, boolean validateLineups) {
        this.homeTeam = Objects.requireNonNull(homeTeam, "homeTeam");
        this.awayTeam = Objects.requireNonNull(awayTeam, "awayTeam");
        if (homeTeam == awayTeam) throw new IllegalArgumentException("A team cannot play itself");
        if (validateLineups) {
            requireStartingEleven(homeTeam);
            requireStartingEleven(awayTeam);
        }
        this.competition = Objects.requireNonNull(competition, "competition");
        if (round < 1) throw new IllegalArgumentException("Round must be positive");
        if (homeGoals < 0 || awayGoals < 0) {
            throw new IllegalArgumentException("Goals cannot be negative");
        }
        this.round = round;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    private static void requireStartingEleven(Team team) {
        if (team.getStartingPlayers().size() != PLAYERS_PER_TEAM) {
            throw new IllegalArgumentException("Each team must field exactly 11 players");
        }
    }

    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public Competition getCompetition() { return competition; }
    public int getRound() { return round; }
    public int getHomeGoals() { return homeGoals; }
    public int getAwayGoals() { return awayGoals; }

    public boolean isDraw() {
        return homeGoals == awayGoals;
    }

    public Optional<Team> getWinner() {
        if (isDraw()) return Optional.empty();
        return Optional.of(homeGoals > awayGoals ? homeTeam : awayTeam);
    }
}
