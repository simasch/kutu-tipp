package ch.martinelli.fun.kututipp.service;

import ch.martinelli.fun.kututipp.dto.LeaderboardEntry;
import ch.martinelli.fun.kututipp.dto.LeaderboardFilter;
import ch.martinelli.fun.kututipp.dto.RankTrend;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static ch.martinelli.fun.kututipp.db.Tables.*;
import static org.jooq.impl.DSL.*;

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

    private final DSLContext dsl;

    public LeaderboardService(DSLContext dsl) {
        this.dsl = dsl;
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

        // Define field aliases for aggregations
        var totalPointsField = coalesce(sum(PREDICTION.POINTS_EARNED), 0).as("total_points");
        var totalPredictionsField = count(PREDICTION.ID).as("total_predictions");
        var exactPredictionsField = count(when(PREDICTION.POINTS_EARNED.eq(3), 1)).as("exact_predictions");
        var avgPointsField = coalesce(avg(PREDICTION.POINTS_EARNED), 0.0).as("avg_points");

        // Query with left join to include users without predictions
        var results = dsl.select(
                        APP_USER.ID,
                        APP_USER.USERNAME,
                        APP_USER.CREATED_AT,
                        totalPointsField,
                        totalPredictionsField,
                        exactPredictionsField,
                        avgPointsField
                )
                .from(APP_USER)
                .leftJoin(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .leftJoin(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .leftJoin(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .groupBy(APP_USER.ID, APP_USER.USERNAME, APP_USER.CREATED_AT)
                .fetch();

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

        var totalPointsField = coalesce(sum(PREDICTION.POINTS_EARNED), 0).as("total_points");
        var totalPredictionsField = count(PREDICTION.ID).as("total_predictions");
        var exactPredictionsField = count(when(PREDICTION.POINTS_EARNED.eq(3), 1)).as("exact_predictions");
        var avgPointsField = coalesce(avg(PREDICTION.POINTS_EARNED), 0.0).as("avg_points");

        var results = dsl.select(
                        APP_USER.ID,
                        APP_USER.USERNAME,
                        APP_USER.CREATED_AT,
                        totalPointsField,
                        totalPredictionsField,
                        exactPredictionsField,
                        avgPointsField
                )
                .from(APP_USER)
                .leftJoin(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .leftJoin(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .leftJoin(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .where(COMPETITION.ID.eq(competitionId))
                .groupBy(APP_USER.ID, APP_USER.USERNAME, APP_USER.CREATED_AT)
                .fetch();

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

        var totalPointsField = coalesce(sum(PREDICTION.POINTS_EARNED), 0).as("total_points");
        var totalPredictionsField = count(PREDICTION.ID).as("total_predictions");
        var exactPredictionsField = count(when(PREDICTION.POINTS_EARNED.eq(3), 1)).as("exact_predictions");
        var avgPointsField = coalesce(avg(PREDICTION.POINTS_EARNED), 0.0).as("avg_points");

        var results = dsl.select(
                        APP_USER.ID,
                        APP_USER.USERNAME,
                        APP_USER.CREATED_AT,
                        totalPointsField,
                        totalPredictionsField,
                        exactPredictionsField,
                        avgPointsField
                )
                .from(APP_USER)
                .leftJoin(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .leftJoin(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .leftJoin(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .leftJoin(APPARATUS).on(COMPETITION_ENTRY.APPARATUS_ID.eq(APPARATUS.ID))
                .where(APPARATUS.ID.eq(apparatusId))
                .groupBy(APP_USER.ID, APP_USER.USERNAME, APP_USER.CREATED_AT)
                .fetch();

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

        var totalPointsField = coalesce(sum(PREDICTION.POINTS_EARNED), 0).as("total_points");
        var totalPredictionsField = count(PREDICTION.ID).as("total_predictions");
        var exactPredictionsField = count(when(PREDICTION.POINTS_EARNED.eq(3), 1)).as("exact_predictions");
        var avgPointsField = coalesce(avg(PREDICTION.POINTS_EARNED), 0.0).as("avg_points");

        var queryBuilder = dsl.select(
                        APP_USER.ID,
                        APP_USER.USERNAME,
                        APP_USER.CREATED_AT,
                        totalPointsField,
                        totalPredictionsField,
                        exactPredictionsField,
                        avgPointsField
                )
                .from(APP_USER)
                .leftJoin(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .leftJoin(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .leftJoin(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .leftJoin(GYMNAST).on(COMPETITION_ENTRY.GYMNAST_ID.eq(GYMNAST.ID))
                .leftJoin(APPARATUS).on(COMPETITION_ENTRY.APPARATUS_ID.eq(APPARATUS.ID))
                .where(PREDICTION.ID.isNull()
                        .or(field("prediction.created_at < (competition.date - interval '30 minutes')").isTrue()));

        // Apply filters
        var filteredQuery = applyFilters(queryBuilder, filter);

        var results = ((SelectConditionStep<?>) filteredQuery).groupBy(APP_USER.ID, APP_USER.USERNAME, APP_USER.CREATED_AT)
                .fetch();

        return calculateRankings(results, currentUsername);
    }

    /**
     * Get user's current rank in overall leaderboard.
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
     * Future enhancement - currently returns STABLE for all users.
     *
     * @param userId      The user ID
     * @param currentRank Current rank position
     * @return Rank trend indicator
     */
    public RankTrend calculateRankTrend(Long userId, int currentRank) {
        // TODO: Implement rank trend calculation by comparing with previous competition
        // For now, return STABLE as a placeholder
        return RankTrend.STABLE;
    }

    /**
     * Apply filter conditions to the query.
     *
     * @param query  The base query
     * @param filter Filter criteria
     * @return Query with filters applied
     */
    private SelectConditionStep<?> applyFilters(
            SelectConditionStep<?> query,
            LeaderboardFilter filter) {

        if (filter.competitionId() != null) {
            query = query.and(COMPETITION.ID.eq(filter.competitionId()));
        }

        if (filter.apparatusId() != null) {
            query = query.and(APPARATUS.ID.eq(filter.apparatusId()));
        }

        if (filter.gender() != null) {
            query = query.and(GYMNAST.GENDER.eq(filter.gender()));
        }

        if (filter.startDate() != null) {
            query = query.and(COMPETITION.DATE.greaterOrEqual(filter.startDate()));
        }

        if (filter.endDate() != null) {
            query = query.and(COMPETITION.DATE.lessOrEqual(filter.endDate()));
        }

        return query;
    }

    /**
     * Calculate rankings from query results.
     * Implements BR-001 ranking rules with tiebreakers.
     *
     * @param results         Query results
     * @param currentUsername Username of currently logged-in user (for highlighting)
     * @return Sorted list of leaderboard entries with ranks assigned
     */
    private List<LeaderboardEntry> calculateRankings(Result<? extends org.jooq.Record> results, String currentUsername) {
        // Convert records to entries with initial data
        var entries = new ArrayList<LeaderboardEntry>();

        for (org.jooq.Record record : results) {
            var userId = record.get(APP_USER.ID);
            var username = record.get(APP_USER.USERNAME);
            var createdAt = record.get(APP_USER.CREATED_AT);
            var totalPoints = record.get("total_points", Integer.class);
            var totalPredictions = record.get("total_predictions", Integer.class);
            var exactPredictions = record.get("exact_predictions", Integer.class);
            var avgPoints = record.get("avg_points", Double.class);

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
