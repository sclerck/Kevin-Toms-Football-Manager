package org.sclerck.ktfm.model;

public enum PlayerPosition {
    DEFENDER,
    MIDFIELDER,
    ATTACKER;

    public static PlayerPosition fromSquadNumber(int number) {
        if (number < 1 || number > 24) {
            throw new IllegalArgumentException("Squad number must be between 1 and 24");
        }
        if (number <= 8) return DEFENDER;
        if (number <= 16) return MIDFIELDER;
        return ATTACKER;
    }
}
