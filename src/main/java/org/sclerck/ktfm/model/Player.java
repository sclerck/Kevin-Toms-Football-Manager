package org.sclerck.ktfm.model;

import java.util.Objects;

public final class Player {
    private final int squadNumber;
    private String name;
    private final PlayerPosition position;
    private PlayerStatus status;
    private int skill;
    private int energy;
    private int marketValue;

    public Player(int squadNumber, String name, PlayerStatus status, int skill, int energy, int marketValue) {
        this.squadNumber = squadNumber;
        this.position = PlayerPosition.fromSquadNumber(squadNumber);
        this.name = requireName(name, "Player name");
        this.status = Objects.requireNonNull(status, "status");
        setSkill(skill);
        setEnergy(energy);
        setMarketValue(marketValue);
    }

    public int getSquadNumber() { return squadNumber; }
    public String getName() { return name; }
    public PlayerPosition getPosition() { return position; }
    public PlayerStatus getStatus() { return status; }
    public int getSkill() { return skill; }
    public int getEnergy() { return energy; }
    public int getMarketValue() { return marketValue; }

    public void setStatus(PlayerStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public void setName(String name) {
        this.name = requireName(name, "Player name");
    }

    public void setSkill(int skill) {
        this.skill = requireRange(skill, 1, 10, "Skill");
    }

    public void setEnergy(int energy) {
        this.energy = requireRange(energy, 1, 20, "Energy");
    }

    public void setMarketValue(int marketValue) {
        if (marketValue < 0) throw new IllegalArgumentException("Market value cannot be negative");
        this.marketValue = marketValue;
    }

    static String requireName(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " cannot be blank");
        return value;
    }

    private static int requireRange(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max);
        }
        return value;
    }
}
