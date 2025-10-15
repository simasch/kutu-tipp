package ch.martinelli.fun.kututipp.service;

import ch.martinelli.fun.kututipp.dto.LeaderboardEntryDto;
import ch.martinelli.fun.kututipp.dto.LeaderboardFilter;
import ch.martinelli.fun.kututipp.dto.RankTrend;
import ch.martinelli.fun.kututipp.repository.LeaderboardRepository;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static ch.martinelli.fun.kututipp.db.Tables.APP_USER;

/**
 * Service for retrieving leaderboard rankings.
 * Implements UC-015: View Leaderboard.
 * <p>
 * Business Rules (BR-001):
 * - Primary Ranking: Total points (sum of all prediction points)
 * - Tie Breaker 1: Number of exact predictions (3 points)
 * - Tie Breaker 2: Total number of predictions made (more predictions = higher rank)
 * <p>
 * Note: Rankings are calculated in the database using SQL window functions (RANK())
 * for better performance and simpler code.
 */
@Service
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardService(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    /**
     * Get overall leaderboard across all competitions.
     * BR-002: Aggregates points from all competitions.
     *
     * @return List of leaderboard entries sorted by rank
     */
    public List<LeaderboardEntryDto> getOverallLeaderboard() {
        return getOverallLeaderboard(null);
    }

    /**
     * Get overall leaderboard across all competitions with current user context.
     *
     * @param currentUsername Username of currently logged-in user (for highlighting)
     * @return List of leaderboard entries sorted by rank
     */
    public List<LeaderboardEntryDto> getOverallLeaderboard(String currentUsername) {
        log.debug("Fetching overall leaderboard");

        var results = leaderboardRepository.getOverallLeaderboard();
        return calculateRankings(results, currentUsername);
    }

    /**
     * Get leaderboard for a specific competition.
     * BR-002: Aggregates points from single competition only.
     *
     * @param competitionId   The competition ID
     * @param currentUsername Username of currently logged-in user (for highlighting)
     * @return Competition-specific rankings
     */
    public List<LeaderboardEntryDto> getCompetitionLeaderboard(Long competitionId, String currentUsername) {
        log.debug("Fetching leaderboard for competition: {}", competitionId);

        var results = leaderboardRepository.getCompetitionLeaderboard(competitionId);
        return calculateRankings(results, currentUsername);
    }

    /**
     * Get leaderboard for a specific apparatus.
     * BR-002: Aggregates points from specific apparatus across all competitions.
     *
     * @param apparatusId     The apparatus ID
     * @param currentUsername Username of currently logged-in user (for highlighting)
     * @return Apparatus-specific rankings
     */
    public List<LeaderboardEntryDto> getApparatusLeaderboard(Long apparatusId, String currentUsername) {
        log.debug("Fetching leaderboard for apparatus: {}", apparatusId);

        var results = leaderboardRepository.getApparatusLeaderboard(apparatusId);
        return calculateRankings(results, currentUsername);
    }

    /**
     * Get leaderboard with filters applied.
     * Supports filtering by competition, apparatus, gender, and date range.
     *
     * @param filter          Filter criteria
     * @param currentUsername Username of currently logged-in user (for highlighting)
     * @return Filtered leaderboard entries
     */
    public List<LeaderboardEntryDto> getFilteredLeaderboard(LeaderboardFilter filter, String currentUsername) {
        log.debug("Fetching filtered leaderboard: {}", filter);

        var results = leaderboardRepository.getFilteredLeaderboard(filter);
        return calculateRankings(results, currentUsername);
    }

    /**
     * Get user's current rank in overall leaderboard.
     * <p>
     * TODO: This method is currently unused and kept for future feature implementation.
     * Consider implementing a user statistics dashboard that displays personal rank.
     *
     * @param userId The user ID
     * @return User's rank (position), or 0 if not found
     */
    public int getUserRank(Long userId) {
        var leaderboard = getOverallLeaderboard();
        return leaderboard.stream()
                .filter(entry -> entry.userId().equals(userId))
                .findFirst()
                .map(LeaderboardEntryDto::rank)
                .orElse(0);
    }

    /**
     * Calculate rank trend by comparing with previous competition.
     * <p>
     * TODO: Future enhancement - implement rank trend calculation.
     * This will require:
     * - Storing historical rank snapshots after each competition
     * - Creating a rank_history table with (user_id, competition_id, rank, timestamp)
     * - Comparing current rank with previous competition's rank
     * - Returns UP if rank improved (lower number), DOWN if worsened, NEW for first-time rankers
     * <p>
     * Currently returns STABLE for all users as a placeholder.
     *
     * @param userId      The user ID
     * @param currentRank Current rank position
     * @return Rank trend indicator
     */
    public RankTrend calculateRankTrend(Long userId, int currentRank) {
        // Placeholder implementation - always returns STABLE
        return RankTrend.STABLE;
    }

    /**
     * Convert query results to LeaderboardEntryDto list.
     * Rankings are now calculated in SQL using window functions (BR-001).
     *
     * @param results         Query results (already sorted and ranked by database)
     * @param currentUsername Username of currently logged-in user (for highlighting)
     * @return List of leaderboard entries with ranks from database
     */
    private List<LeaderboardEntryDto> calculateRankings(Result<? extends org.jooq.Record> results, String currentUsername) {
        var entries = new ArrayList<LeaderboardEntryDto>();

        for (var result : results) {
            var userId = result.get(APP_USER.ID);
            var username = result.get(APP_USER.USERNAME);
            var rank = result.get("rank", Integer.class);
            var totalPoints = result.get("total_points", Integer.class);
            var totalPredictions = result.get("total_predictions", Integer.class);
            var exactPredictions = result.get("exact_predictions", Integer.class);
            var avgPoints = result.get("avg_points", Double.class);

            var entry = new LeaderboardEntryDto(
                    userId,
                    username,
                    rank,
                    totalPoints,
                    totalPredictions,
                    exactPredictions,
                    Math.round(avgPoints * 100.0) / 100.0, // Round to 2 decimal places
                    RankTrend.STABLE, // TODO: Implement trend calculation
                    username.equals(currentUsername)
            );

            entries.add(entry);
        }

        log.debug("Mapped {} ranked users from database", entries.size());
        return entries;
    }
}
