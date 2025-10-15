package ch.martinelli.fun.kututipp.repository;

import ch.martinelli.fun.kututipp.dto.LeaderboardFilter;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectHavingStep;
import org.springframework.stereotype.Repository;

import static ch.martinelli.fun.kututipp.db.Routines.calculatePoints;
import static ch.martinelli.fun.kututipp.db.Tables.*;
import static org.jooq.impl.DSL.*;

/**
 * Repository for leaderboard database operations using jOOQ.
 * Handles all leaderboard-related queries including ranking calculation via SQL window functions.
 */
@Repository
public class LeaderboardRepository {

    private static final String TOTAL_POINTS = "total_points";
    private static final String TOTAL_PREDICTIONS = "total_predictions";
    private static final String EXACT_PREDICTIONS = "exact_predictions";
    private static final String AVG_POINTS = "avg_points";
    private static final String RANK = "rank";

    private final DSLContext dsl;

    public LeaderboardRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Get overall leaderboard across all competitions with rankings calculated in SQL.
     *
     * @return Query results with leaderboard data including rank
     */
    public Result<? extends org.jooq.Record> getOverallLeaderboard() {
        var aggregatedData = buildAggregatedLeaderboardQuery(null, null, null);
        return addRankingAndOrder(aggregatedData);
    }

    /**
     * Get leaderboard for a specific competition with rankings calculated in SQL.
     *
     * @param competitionId The competition ID
     * @return Competition-specific query results including rank
     */
    public Result<? extends org.jooq.Record> getCompetitionLeaderboard(Long competitionId) {
        var aggregatedData = buildAggregatedLeaderboardQuery(competitionId, null, null);
        return addRankingAndOrder(aggregatedData);
    }

    /**
     * Get leaderboard for a specific apparatus with rankings calculated in SQL.
     *
     * @param apparatusId The apparatus ID
     * @return Apparatus-specific query results including rank
     */
    public Result<? extends org.jooq.Record> getApparatusLeaderboard(Long apparatusId) {
        var aggregatedData = buildAggregatedLeaderboardQuery(null, apparatusId, null);
        return addRankingAndOrder(aggregatedData);
    }

    /**
     * Get leaderboard with filters applied and rankings calculated in SQL.
     *
     * @param filter Filter criteria
     * @return Filtered leaderboard query results including rank
     */
    public Result<? extends org.jooq.Record> getFilteredLeaderboard(LeaderboardFilter filter) {
        var aggregatedData = buildAggregatedLeaderboardQuery(
                filter.competitionId(),
                filter.apparatusId(),
                filter
        );
        return addRankingAndOrder(aggregatedData);
    }

    /**
     * Builds the aggregated leaderboard query with points calculation.
     * This query groups by user and calculates total points, predictions, etc.
     *
     * @param competitionId Optional competition ID filter
     * @param apparatusId   Optional apparatus ID filter
     * @param filter        Optional additional filters (gender, date range)
     * @return Select query with aggregated leaderboard data (without ranking)
     */
    private SelectHavingStep<?> buildAggregatedLeaderboardQuery(
            Long competitionId,
            Long apparatusId,
            LeaderboardFilter filter) {

        // Calculate points on-the-fly using database function
        var pointsField = calculatePoints(PREDICTION.PREDICTED_SCORE, COMPETITION_ENTRY.ACTUAL_SCORE);

        // Define field aliases for aggregations
        var totalPointsField = coalesce(sum(pointsField), 0).as(TOTAL_POINTS);
        var totalPredictionsField = count(PREDICTION.ID).as(TOTAL_PREDICTIONS);
        var exactPredictionsField = count(when(pointsField.eq(3), 1)).as(EXACT_PREDICTIONS);
        var avgPointsField = coalesce(avg(pointsField), 0.0).as(AVG_POINTS);

        // Build base query
        var query = dsl.select(
                        APP_USER.ID,
                        APP_USER.USERNAME,
                        totalPointsField,
                        totalPredictionsField,
                        exactPredictionsField,
                        avgPointsField
                )
                .from(APP_USER)
                .join(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .join(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .join(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID));

        // Add joins and conditions based on filters
        SelectConditionStep<?> conditionStep;

        if (apparatusId != null || (filter != null && filter.apparatusId() != null)) {
            query = query.join(APPARATUS).on(COMPETITION_ENTRY.APPARATUS_ID.eq(APPARATUS.ID));
        }

        if (filter != null && filter.gender() != null) {
            query = query.join(GYMNAST).on(COMPETITION_ENTRY.GYMNAST_ID.eq(GYMNAST.ID));
        }

        conditionStep = query.where(COMPETITION_ENTRY.ACTUAL_SCORE.isNotNull());

        // Apply filters
        if (competitionId != null) {
            conditionStep = conditionStep.and(COMPETITION.ID.eq(competitionId));
        }

        if (apparatusId != null) {
            conditionStep = conditionStep.and(APPARATUS.ID.eq(apparatusId));
        }

        if (filter != null) {
            conditionStep = applyAdditionalFilters(conditionStep, filter);
        }

        return conditionStep.groupBy(APP_USER.ID, APP_USER.USERNAME);
    }

    /**
     * Apply additional filter conditions for gender and date range.
     *
     * @param query  The base query
     * @param filter Filter criteria
     * @return Query with filters applied
     */
    private SelectConditionStep<?> applyAdditionalFilters(
            SelectConditionStep<?> query,
            LeaderboardFilter filter) {

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
     * Wraps the aggregated data with RANK() window function and orders by rank.
     * Implements BR-001 ranking rules:
     * - Primary: Total points (descending)
     * - Tie Breaker 1: Exact predictions (descending)
     * - Tie Breaker 2: Total predictions (descending)
     *
     * @param aggregatedQuery The aggregated leaderboard query
     * @return Query results with rank column, ordered by rank
     */
    private Result<? extends org.jooq.Record> addRankingAndOrder(SelectHavingStep<?> aggregatedQuery) {
        var leaderboardData = aggregatedQuery.asTable("leaderboard_data");

        var totalPoints = leaderboardData.field(TOTAL_POINTS, Integer.class);
        var exactPredictions = leaderboardData.field(EXACT_PREDICTIONS, Integer.class);
        var totalPredictions = leaderboardData.field(TOTAL_PREDICTIONS, Integer.class);

        // Use RANK() window function for ranking (handles ties properly)
        var rankField = rank().over()
                .orderBy(
                        totalPoints.desc(),
                        exactPredictions.desc(),
                        totalPredictions.desc()
                )
                .as(RANK);

        return dsl.select(
                        leaderboardData.asterisk(),
                        rankField
                )
                .from(leaderboardData)
                .orderBy(field(name(RANK)))
                .fetch();
    }
}
