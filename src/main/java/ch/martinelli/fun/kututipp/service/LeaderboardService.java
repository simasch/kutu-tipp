package ch.martinelli.fun.kututipp.service;

import ch.martinelli.fun.kututipp.dto.LeaderboardEntry;
import ch.martinelli.fun.kututipp.dto.LeaderboardFilter;
import ch.martinelli.fun.kututipp.dto.RankTrend;
import ch.martinelli.fun.kututipp.repository.LeaderboardRepository;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static ch.martinelli.fun.kututipp.db.Tables.APP_USER;

/**
 * Service for retrieving and calculating leaderboard rankings.
 * Implements UC-015: View Leaderboard.
 * <p>
 * Business Rules (BR-001):
 * - Primary Ranking: Total points (sum of all prediction points)
 * - Tie Breaker 1: Number of exact predictions (3 points)
 * - Tie Breaker 2: Total number of predictions made (more predictions = higher rank)
 * - Tie Breaker 3: Earlier registration date
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
    public List<LeaderboardEntry> getOverallLeaderboard() {
        return getOverallLeaderboard(null);
    }

    /**
     * Get overall leaderboard across all competitions with current user context.
     *
     * @param currentUsername Username of currently logged-in user (for highlighting)
     * @return List of leaderboard entries sorted by rank
     */
    public List<LeaderboardEntry> getOverallLeaderboard(String currentUsername) {
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
    public List<LeaderboardEntry> getCompetitionLeaderboard(Long competitionId, String currentUsername) {
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
    public List<LeaderboardEntry> getApparatusLeaderboard(Long apparatusId, String currentUsername) {
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
    public List<LeaderboardEntry> getFilteredLeaderboard(LeaderboardFilter filter, String currentUsername) {
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
                .map(LeaderboardEntry::rank)
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
     * Calculate rankings from query results.
     * Implements BR-001 ranking rules with tiebreakers.
     *
     * @param results         Query results
     * @param currentUsername Username of currently logged-in user (for highlighting)
     * @return Sorted list of leaderboard entries with ranks assigned
     */
    private List<LeaderboardEntry> calculateRankings(Result<? extends Record> results, String currentUsername) {
        // Convert records to entries with initial data
        var entries = new ArrayList<LeaderboardEntry>();

        for (var result : results) {
            var userId = result.get(APP_USER.ID);
            var username = result.get(APP_USER.USERNAME);
            var totalPoints = result.get("total_points", Integer.class);
            var totalPredictions = result.get("total_predictions", Integer.class);
            var exactPredictions = result.get("exact_predictions", Integer.class);
            var avgPoints = result.get("avg_points", Double.class);

            // Create entry without rank (will be assigned after sorting)
            var entry = new LeaderboardEntry(
                    userId,
                    username,
                    0, // Rank will be assigned below
                    totalPoints,
                    totalPredictions,
                    exactPredictions,
                    Math.round(avgPoints * 100.0) / 100.0, // Round to 2 decimal places
                    RankTrend.STABLE, // TODO: Implement trend calculation
                    username.equals(currentUsername)
            );

            entries.add(entry);
        }

        // Sort by ranking rules (BR-001)
        entries.sort(Comparator
                .comparing(LeaderboardEntry::totalPoints).reversed()
                .thenComparing(LeaderboardEntry::exactPredictions).reversed()
                .thenComparing(LeaderboardEntry::totalPredictions).reversed()
        );

        // Assign ranks handling ties
        var rankedEntries = new ArrayList<LeaderboardEntry>();
        var rank = 1;

        for (var i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);

            // If not first entry and scores differ, update rank
            if (i > 0 && !hasSameScore(entry, entries.get(i - 1))) {
                rank = i + 1;
            }

            // Create new entry with rank assigned
            var rankedEntry = new LeaderboardEntry(
                    entry.userId(),
                    entry.username(),
                    rank,
                    entry.totalPoints(),
                    entry.totalPredictions(),
                    entry.exactPredictions(),
                    entry.avgPoints(),
                    entry.trend(),
                    entry.isCurrentUser()
            );

            rankedEntries.add(rankedEntry);
        }

        log.debug("Calculated rankings for {} users", rankedEntries.size());
        return rankedEntries;
    }

    /**
     * Check if two entries have the same score according to tiebreaker rules.
     *
     * @param e1 First entry
     * @param e2 Second entry
     * @return true if scores are equal (considering all tiebreakers)
     */
    private boolean hasSameScore(LeaderboardEntry e1, LeaderboardEntry e2) {
        return e1.totalPoints() == e2.totalPoints()
                && e1.exactPredictions() == e2.exactPredictions()
                && e1.totalPredictions() == e2.totalPredictions();
    }
}
