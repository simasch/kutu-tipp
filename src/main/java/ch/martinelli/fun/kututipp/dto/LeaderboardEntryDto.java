package ch.martinelli.fun.kututipp.dto;

/**
 * Represents a single entry in the leaderboard.
 * Contains all information needed to display a user's ranking and statistics.
 *
 * @param userId            The user's unique identifier
 * @param username          The user's display name
 * @param rank              The user's current rank position (1 = first place)
 * @param totalPoints       Total points earned across all predictions
 * @param totalPredictions  Total number of predictions made
 * @param exactPredictions  Number of predictions with exact matches (3 points)
 * @param avgPoints         Average points per prediction
 * @param trend             Rank trend indicator (up/down/stable/new)
 * @param isCurrentUser     True if this entry represents the currently logged-in user
 */
public record LeaderboardEntryDto(
        Long userId,
        String username,
        int rank,
        int totalPoints,
        int totalPredictions,
        int exactPredictions,
        double avgPoints,
        RankTrend trend,
        boolean isCurrentUser
) {
}
