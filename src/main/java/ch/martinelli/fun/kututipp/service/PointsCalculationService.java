package ch.martinelli.fun.kututipp.service;

import ch.martinelli.fun.kututipp.db.tables.records.PredictionRecord;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static ch.martinelli.fun.kututipp.db.Tables.*;
import static org.jooq.impl.DSL.sum;

/**
 * Service for calculating points based on prediction accuracy.
 * Implements UC-014: Calculate Points.
 * <p>
 * Business Rules (BR-001):
 * - Exact match (< 0.001 difference): 3 points
 * - Within 5% deviation: 2 points
 * - Within 10% deviation: 1 point
 * - More than 10% deviation: 0 points
 */
@Service
public class PointsCalculationService {

    private static final Logger log = LoggerFactory.getLogger(PointsCalculationService.class);

    /**
     * Threshold for considering two scores as exactly equal.
     * Accounts for floating-point precision issues.
     */
    private static final double EXACT_MATCH_THRESHOLD = 0.001;

    /**
     * Percentage deviation threshold for 2 points.
     */
    private static final double FIVE_PERCENT_THRESHOLD = 5.0;

    /**
     * Percentage deviation threshold for 1 point.
     */
    private static final double TEN_PERCENT_THRESHOLD = 10.0;

    /**
     * Points awarded for exact match.
     */
    private static final int EXACT_MATCH_POINTS = 3;

    /**
     * Points awarded for predictions within 5% deviation.
     */
    private static final int FIVE_PERCENT_POINTS = 2;

    /**
     * Points awarded for predictions within 10% deviation.
     */
    private static final int TEN_PERCENT_POINTS = 1;

    /**
     * Points awarded for predictions with more than 10% deviation.
     */
    private static final int NO_POINTS = 0;

    /**
     * Minimum valid score (gymnastics scores are non-negative).
     */
    private static final double MIN_VALID_SCORE = 0.0;

    /**
     * Maximum valid score (typical gymnastics range).
     */
    private static final double MAX_VALID_SCORE = 20.0;

    /**
     * Prediction deadline in minutes before competition start.
     */
    private static final int PREDICTION_DEADLINE_MINUTES = 30;

    private final DSLContext dsl;

    public PointsCalculationService(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Calculate points for a single prediction based on accuracy.
     * <p>
     * Examples:
     * - Actual: 14.50, Predicted: 14.50 → 3 points (exact)
     * - Actual: 14.50, Predicted: 14.70 → 2 points (1.38% deviation)
     * - Actual: 14.50, Predicted: 15.30 → 1 point (5.52% deviation)
     * - Actual: 14.50, Predicted: 16.00 → 0 points (10.34% deviation)
     *
     * @param predicted The predicted score
     * @param actual    The actual score
     * @return Points earned (0-3), or 0 if actual score is invalid
     */
    public int calculatePoints(double predicted, double actual) {
        // BR-003: Validate scores are within valid range
        if (!isValidScore(actual) || !isValidScore(predicted)) {
            log.warn("Invalid scores: predicted={}, actual={}", predicted, actual);
            return NO_POINTS;
        }

        // Handle edge case where actual score is zero
        if (actual < EXACT_MATCH_THRESHOLD) {
            // If actual is essentially zero, only exact match gets points
            return Math.abs(predicted - actual) < EXACT_MATCH_THRESHOLD ? EXACT_MATCH_POINTS : NO_POINTS;
        }

        // Calculate absolute difference
        var difference = Math.abs(predicted - actual);

        // BR-001: Check for exact match (accounting for floating-point precision)
        if (difference < EXACT_MATCH_THRESHOLD) {
            return EXACT_MATCH_POINTS;
        }

        // BR-001: Calculate percentage deviation
        var percentageDeviation = (difference / actual) * 100.0;

        // BR-001: Award points based on deviation thresholds
        if (percentageDeviation <= FIVE_PERCENT_THRESHOLD) {
            return FIVE_PERCENT_POINTS;
        } else if (percentageDeviation <= TEN_PERCENT_THRESHOLD) {
            return TEN_PERCENT_POINTS;
        } else {
            return NO_POINTS;
        }
    }

    /**
     * Calculate and store points for all predictions of a competition entry.
     * Triggered when an administrator enters an actual score (UC-011).
     * <p>
     * Only processes predictions made before the deadline (30 minutes before competition start).
     *
     * @param entryId The competition entry ID
     * @return Number of predictions updated
     * @throws IllegalArgumentException if entry does not exist or has no actual score
     */
    @Transactional
    public int calculatePointsForEntry(Long entryId) {
        log.info("Calculating points for competition entry: {}", entryId);

        // Retrieve competition entry with actual score
        var entryRecord = dsl.selectFrom(COMPETITION_ENTRY)
                .where(COMPETITION_ENTRY.ID.eq(entryId))
                .fetchOne();

        if (entryRecord == null) {
            throw new IllegalArgumentException("Competition entry not found: " + entryId);
        }

        var actualScore = entryRecord.getActualScore();
        if (actualScore == null) {
            throw new IllegalArgumentException("Competition entry has no actual score: " + entryId);
        }

        // Get competition start time for deadline calculation
        var competitionId = entryRecord.getCompetitionId();
        var competition = dsl.selectFrom(COMPETITION)
                .where(COMPETITION.ID.eq(competitionId))
                .fetchOne();

        if (competition == null) {
            throw new IllegalArgumentException("Competition not found: " + competitionId);
        }

        var competitionStartTime = competition.getDate();
        var deadline = competitionStartTime.minusMinutes(PREDICTION_DEADLINE_MINUTES);

        // Retrieve all predictions for this entry
        var predictions = dsl.selectFrom(PREDICTION)
                .where(PREDICTION.COMPETITION_ENTRY_ID.eq(entryId))
                .fetch();

        if (predictions.isEmpty()) {
            log.info("No predictions found for entry {}, nothing to calculate", entryId);
            return 0;
        }

        var actualScoreDouble = actualScore.doubleValue();
        var updatedCount = 0;

        // Calculate points for each prediction
        for (var prediction : predictions) {
            var points = calculatePointsForPrediction(prediction, actualScoreDouble, deadline);
            updatePredictionPoints(prediction, points);
            updatedCount++;
        }

        log.info("Updated {} predictions for entry {}", updatedCount, entryId);
        return updatedCount;
    }

    /**
     * Recalculate points for all entries in a competition.
     * Used after bulk imports or when recalculation is manually triggered.
     *
     * @param competitionId The competition ID
     * @return Number of predictions recalculated
     * @throws IllegalArgumentException if competition does not exist
     */
    @Transactional
    public int recalculatePointsForCompetition(Long competitionId) {
        log.info("Recalculating points for competition: {}", competitionId);

        // Verify competition exists
        var competition = dsl.selectFrom(COMPETITION)
                .where(COMPETITION.ID.eq(competitionId))
                .fetchOne();

        if (competition == null) {
            throw new IllegalArgumentException("Competition not found: " + competitionId);
        }

        var competitionStartTime = competition.getDate();
        var deadline = competitionStartTime.minusMinutes(PREDICTION_DEADLINE_MINUTES);

        // Get all entries with actual scores for this competition
        var entries = dsl.selectFrom(COMPETITION_ENTRY)
                .where(COMPETITION_ENTRY.COMPETITION_ID.eq(competitionId))
                .and(COMPETITION_ENTRY.ACTUAL_SCORE.isNotNull())
                .fetch();

        if (entries.isEmpty()) {
            log.info("No entries with actual scores found for competition {}", competitionId);
            return 0;
        }

        var totalUpdated = 0;

        for (var entry : entries) {
            var predictions = dsl.selectFrom(PREDICTION)
                    .where(PREDICTION.COMPETITION_ENTRY_ID.eq(entry.getId()))
                    .fetch();

            var actualScoreDouble = entry.getActualScore().doubleValue();

            for (var prediction : predictions) {
                var points = calculatePointsForPrediction(prediction, actualScoreDouble, deadline);
                updatePredictionPoints(prediction, points);
                totalUpdated++;
            }
        }

        log.info("Recalculated {} predictions for competition {}", totalUpdated, competitionId);
        return totalUpdated;
    }

    /**
     * Calculate total points for a user in a specific competition.
     * Used for leaderboard calculations.
     *
     * @param userId        The user ID
     * @param competitionId The competition ID
     * @return Total points earned in this competition
     */
    public int calculateUserCompetitionTotal(Long userId, Long competitionId) {
        var result = dsl.select(sum(PREDICTION.POINTS_EARNED))
                .from(PREDICTION)
                .join(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .where(PREDICTION.USER_ID.eq(userId))
                .and(COMPETITION_ENTRY.COMPETITION_ID.eq(competitionId))
                .and(PREDICTION.POINTS_EARNED.isNotNull())
                .fetchOne();

        return result != null && result.value1() != null ? result.value1().intValue() : 0;
    }

    /**
     * Calculate total points for a user across all competitions.
     * Used for overall leaderboard.
     *
     * @param userId The user ID
     * @return Total points earned across all competitions
     */
    public int calculateUserOverallTotal(Long userId) {
        var result = dsl.select(sum(PREDICTION.POINTS_EARNED))
                .from(PREDICTION)
                .where(PREDICTION.USER_ID.eq(userId))
                .and(PREDICTION.POINTS_EARNED.isNotNull())
                .fetchOne();

        return result != null && result.value1() != null ? result.value1().intValue() : 0;
    }

    /**
     * Calculate points for a single prediction, considering deadline validity.
     * BR-002: Only predictions submitted before the deadline are eligible for points.
     *
     * @param prediction The prediction record
     * @param actualScore The actual score
     * @param deadline The prediction deadline
     * @return Points earned (0-3)
     */
    private int calculatePointsForPrediction(PredictionRecord prediction, double actualScore, OffsetDateTime deadline) {
        // BR-002: Check if prediction was made before deadline
        if (prediction.getCreatedAt().isAfter(deadline)) {
            log.debug("Prediction {} made after deadline, awarding 0 points", prediction.getId());
            return NO_POINTS;
        }

        var predictedScore = prediction.getPredictedScore().doubleValue();
        return calculatePoints(predictedScore, actualScore);
    }

    /**
     * Update prediction record with calculated points.
     * BR-004: Previous points are overwritten, not accumulated.
     *
     * @param prediction The prediction to update
     * @param points The calculated points
     */
    private void updatePredictionPoints(PredictionRecord prediction, int points) {
        dsl.update(PREDICTION)
                .set(PREDICTION.POINTS_EARNED, points)
                .set(PREDICTION.UPDATED_AT, OffsetDateTime.now())
                .where(PREDICTION.ID.eq(prediction.getId()))
                .execute();

        log.debug("Updated prediction {} with {} points (user={}, entry={})",
                prediction.getId(), points, prediction.getUserId(), prediction.getCompetitionEntryId());
    }

    /**
     * Validate if a score is within the valid range.
     * BR-003: Scores must be between 0.0 and 20.0.
     *
     * @param score The score to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidScore(double score) {
        return score >= MIN_VALID_SCORE && score <= MAX_VALID_SCORE;
    }
}
