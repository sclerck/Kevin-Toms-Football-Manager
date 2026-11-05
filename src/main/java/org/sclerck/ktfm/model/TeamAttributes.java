package org.sclerck.ktfm.model;

public record TeamAttributes(int energy, int morale, int defence, int midfield, int attack) {
    public TeamAttributes {
        requireRating(energy, "Energy");
        requireRating(morale, "Morale");
        requireRating(defence, "Defence");
        requireRating(midfield, "Midfield");
        requireRating(attack, "Attack");
    }

    private static void requireRating(int rating, String label) {
        if (rating < 1 || rating > 20) {
            throw new IllegalArgumentException(label + " must be between 1 and 20");
        }
    }
}
