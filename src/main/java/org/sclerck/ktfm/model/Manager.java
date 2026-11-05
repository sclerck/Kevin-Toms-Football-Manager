package org.sclerck.ktfm.model;

import java.util.Objects;

public final class Manager {
    private final String name;
    private final Team team;
    private final int skillLevel;

    public Manager(String name, Team team, int skillLevel) {
        this.name = Player.requireName(name, "Manager name");
        this.team = Objects.requireNonNull(team, "team");
        if (skillLevel < 1 || skillLevel > 7) {
            throw new IllegalArgumentException("Skill level must be between 1 and 7");
        }
        this.skillLevel = skillLevel;
    }

    public String getName() { return name; }
    public Team getTeam() { return team; }
    public int getSkillLevel() { return skillLevel; }
}
