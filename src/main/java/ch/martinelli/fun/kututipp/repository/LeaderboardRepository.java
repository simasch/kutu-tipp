package ch.martinelli.fun.kututipp.repository;

import ch.martinelli.fun.kututipp.dto.LeaderboardFilter;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;

import static ch.martinelli.fun.kututipp.db.Routines.calculatePoints;
import static ch.martinelli.fun.kututipp.db.Tables.*;
import static org.jooq.impl.DSL.*;

/**
 * Repository for leaderboard database operations using jOOQ.
 * Handles all leaderboard-related queries.
 */
@Repository
public class LeaderboardRepository {

    private static final String TOTAL_POINTS = "total_points";
    private static final String TOTAL_PREDICTIONS = "total_predictions";
    private static final String EXACT_PREDICTIONS = "exact_predictions";
    private static final String AVG_POINTS = "avg_points";

    private final DSLContext dsl;

    public LeaderboardRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Get overall leaderboard across all competitions.
     *
     * @return Query results with leaderboard data
     */
    public Result<? extends Record> getOverallLeaderboard() {
        // Calculate points on-the-fly using database function
        var pointsField = calculatePoints(PREDICTION.PREDICTED_SCORE, COMPETITION_ENTRY.ACTUAL_SCORE);

        // Define field aliases for aggregations
        var totalPointsField = coalesce(sum(pointsField), 0).as(TOTAL_POINTS);
        var totalPredictionsField = count(PREDICTION.ID).as(TOTAL_PREDICTIONS);
        var exactPredictionsField = count(when(pointsField.eq(3), 1)).as(EXACT_PREDICTIONS);
        var avgPointsField = coalesce(avg(pointsField), 0.0).as(AVG_POINTS);

        // Query with inner join to exclude users without predictions
        return dsl.select(
                        APP_USER.ID,
                        APP_USER.USERNAME,
                        APP_USER.CREATED_AT,
                        totalPointsField,
                        totalPredictionsField,
                        exactPredictionsField,
                        avgPointsField
                )
                .from(APP_USER)
                .join(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .join(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .join(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .where(COMPETITION_ENTRY.ACTUAL_SCORE.isNotNull())
                .groupBy(APP_USER.ID, APP_USER.USERNAME, APP_USER.CREATED_AT)
                .fetch();
    }

    /**
     * Get leaderboard for a specific competition.
     *
     * @param competitionId The competition ID
     * @return Competition-specific query results
     */
    public Result<? extends Record> getCompetitionLeaderboard(Long competitionId) {
        // Calculate points on-the-fly using database function
        var pointsField = calculatePoints(PREDICTION.PREDICTED_SCORE, COMPETITION_ENTRY.ACTUAL_SCORE);

        var totalPointsField = coalesce(sum(pointsField), 0).as(TOTAL_POINTS);
        var totalPredictionsField = count(PREDICTION.ID).as(TOTAL_PREDICTIONS);
        var exactPredictionsField = count(when(pointsField.eq(3), 1)).as(EXACT_PREDICTIONS);
        var avgPointsField = coalesce(avg(pointsField), 0.0).as(AVG_POINTS);

        return dsl.select(
                        APP_USER.ID,
                        APP_USER.USERNAME,
                        APP_USER.CREATED_AT,
                        totalPointsField,
                        totalPredictionsField,
                        exactPredictionsField,
                        avgPointsField
                )
                .from(APP_USER)
                .join(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .join(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .join(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .where(COMPETITION.ID.eq(competitionId))
                .and(COMPETITION_ENTRY.ACTUAL_SCORE.isNotNull())
                .groupBy(APP_USER.ID, APP_USER.USERNAME, APP_USER.CREATED_AT)
                .fetch();
    }

    /**
     * Get leaderboard for a specific apparatus.
     *
     * @param apparatusId The apparatus ID
     * @return Apparatus-specific query results
     */
    public Result<? extends Record> getApparatusLeaderboard(Long apparatusId) {
        // Calculate points on-the-fly using database function
        var pointsField = calculatePoints(PREDICTION.PREDICTED_SCORE, COMPETITION_ENTRY.ACTUAL_SCORE);

        var totalPointsField = coalesce(sum(pointsField), 0).as(TOTAL_POINTS);
        var totalPredictionsField = count(PREDICTION.ID).as(TOTAL_PREDICTIONS);
        var exactPredictionsField = count(when(pointsField.eq(3), 1)).as(EXACT_PREDICTIONS);
        var avgPointsField = coalesce(avg(pointsField), 0.0).as(AVG_POINTS);

        return dsl.select(
                        APP_USER.ID,
                        APP_USER.USERNAME,
                        APP_USER.CREATED_AT,
                        totalPointsField,
                        totalPredictionsField,
                        exactPredictionsField,
                        avgPointsField
                )
                .from(APP_USER)
                .join(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .join(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .join(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .join(APPARATUS).on(COMPETITION_ENTRY.APPARATUS_ID.eq(APPARATUS.ID))
                .where(APPARATUS.ID.eq(apparatusId))
                .and(COMPETITION_ENTRY.ACTUAL_SCORE.isNotNull())
                .groupBy(APP_USER.ID, APP_USER.USERNAME, APP_USER.CREATED_AT)
                .fetch();
    }

    /**
     * Get leaderboard with filters applied.
     *
     * @param filter Filter criteria
     * @return Filtered leaderboard query results
     */
    public Result<? extends Record> getFilteredLeaderboard(LeaderboardFilter filter) {
        // Calculate points on-the-fly using database function
        var pointsField = calculatePoints(PREDICTION.PREDICTED_SCORE, COMPETITION_ENTRY.ACTUAL_SCORE);

        var totalPointsField = coalesce(sum(pointsField), 0).as(TOTAL_POINTS);
        var totalPredictionsField = count(PREDICTION.ID).as(TOTAL_PREDICTIONS);
        var exactPredictionsField = count(when(pointsField.eq(3), 1)).as(EXACT_PREDICTIONS);
        var avgPointsField = coalesce(avg(pointsField), 0.0).as(AVG_POINTS);

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
                .join(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
                .join(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .join(COMPETITION).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .leftJoin(GYMNAST).on(COMPETITION_ENTRY.GYMNAST_ID.eq(GYMNAST.ID))
                .leftJoin(APPARATUS).on(COMPETITION_ENTRY.APPARATUS_ID.eq(APPARATUS.ID))
                .where(COMPETITION_ENTRY.ACTUAL_SCORE.isNotNull());

        // Apply filters
        var filteredQuery = applyFilters(queryBuilder, filter);

        return filteredQuery.groupBy(APP_USER.ID, APP_USER.USERNAME, APP_USER.CREATED_AT)
                .fetch();
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
}
