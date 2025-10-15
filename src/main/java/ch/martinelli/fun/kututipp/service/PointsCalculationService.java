package ch.martinelli.fun.kututipp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for points calculation logic reference.
 * <p>
 * NOTE: As of UC-014 refactoring, points are now calculated in the database using the
 * PostgreSQL function `calculate_points(predicted, actual)`. This service is kept for
 * reference, documentation, and testing purposes to verify the database function implementation.
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
     * Calculate points for a single prediction based on accuracy.
     * <p>
     * NOTE: This method is kept for reference and testing. In production, points are calculated
     * by the database function `calculate_points(predicted, actual)`.
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
