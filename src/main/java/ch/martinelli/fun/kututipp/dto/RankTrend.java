package ch.martinelli.fun.kututipp.dto;

/**
 * Represents the trend of a user's ranking compared to a previous period.
 * Used in leaderboard displays to show if a user's rank has improved, declined, or remained stable.
 */
public enum RankTrend {
    /**
     * User's rank has improved (moved up in the rankings).
     */
    UP,

    /**
     * User's rank has declined (moved down in the rankings).
     */
    DOWN,

    /**
     * User's rank has remained unchanged.
     */
    STABLE,

    /**
     * User is new to the leaderboard (first time appearing).
     */
    NEW
}
