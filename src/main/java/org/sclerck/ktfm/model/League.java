package org.sclerck.ktfm.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class League {
    public static final int TEAMS_PER_DIVISION = 16;

    private final int division;
    private final Map<Team, LeagueStanding> standings = new LinkedHashMap<>();
    private final Set<Match> recordedMatches = new HashSet<>();

    public League(int division) {
        if (division < 1 || division > 4) {
            throw new IllegalArgumentException("Division must be between 1 and 4");
        }
        this.division = division;
    }

    public int getDivision() { return division; }

    public List<Team> getTeams() {
        return List.copyOf(standings.keySet());
    }

    public LeagueStanding getStanding(Team team) {
        LeagueStanding standing = standings.get(team);
        if (standing == null) {
            throw new IllegalArgumentException("Team does not belong to this league");
        }
        return standing;
    }

    public void addTeam(Team team) {
        Objects.requireNonNull(team, "team");
        if (team.getDivision() != division) {
            throw new IllegalArgumentException("Team must belong to division " + division);
        }
        if (standings.containsKey(team)) {
            throw new IllegalArgumentException("Team is already in this league");
        }
        if (standings.size() >= TEAMS_PER_DIVISION) {
            throw new IllegalStateException("A division cannot contain more than 16 teams");
        }
        standings.put(team, new LeagueStanding(team));
    }

    public void recordResult(Match match) {
        Objects.requireNonNull(match, "match");
        if (match.getCompetition() != Competition.LEAGUE) {
            throw new IllegalArgumentException("Only league matches update league standings");
        }
        getStanding(match.getHomeTeam());
        getStanding(match.getAwayTeam());
        if (!recordedMatches.add(match)) {
            throw new IllegalArgumentException("Match result has already been recorded");
        }
        recordResult(match.getHomeTeam(), match.getAwayTeam(),
                match.getHomeGoals(), match.getAwayGoals());
    }

    public void recordResult(Team homeTeam, Team awayTeam, int homeGoals, int awayGoals) {
        Objects.requireNonNull(homeTeam, "homeTeam");
        Objects.requireNonNull(awayTeam, "awayTeam");
        if (homeTeam == awayTeam) throw new IllegalArgumentException("A team cannot play itself");
        if (homeGoals < 0 || awayGoals < 0) {
            throw new IllegalArgumentException("Goals cannot be negative");
        }
        LeagueStanding home = getStanding(homeTeam);
        LeagueStanding away = getStanding(awayTeam);
        home.record(homeGoals, awayGoals);
        away.record(awayGoals, homeGoals);
    }

    public List<LeagueStanding> getTable() {
        List<LeagueStanding> table = new ArrayList<>(standings.values());
        table.sort(Comparator.comparingInt(LeagueStanding::getPoints).reversed()
                .thenComparing(Comparator.comparingInt(LeagueStanding::getGoalDifference).reversed())
                .thenComparing(Comparator.comparingInt(LeagueStanding::getGoalsFor).reversed()));
        return List.copyOf(table);
    }
}
