package ch.martinelli.fun.kututipp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PointsCalculationService.
 * Tests cover UC-014: Calculate Points business rules.
 * <p>
 * NOTE: These tests verify the Java implementation matches the database function.
 * The actual points calculation in production happens in the PostgreSQL function calculate_points().
 */
class PointsCalculationServiceTest {

    private PointsCalculationService pointsCalculationService;

    @BeforeEach
    void setUp() {
        pointsCalculationService = new PointsCalculationService();
    }

    /**
     * Tests for the core calculatePoints() method.
     * These are pure unit tests that don't require database access.
     */
    @Nested
    @DisplayName("BR-001: Point Calculation Formula")
    class CalculationAccuracyTests {

        @Test
        @DisplayName("Exact match should award 3 points")
        void exactMatch() {
            var points = pointsCalculationService.calculatePoints(14.50, 14.50);
            assertThat(points).isEqualTo(3);
        }

        @Test
        @DisplayName("Small deviation (1.38%) should award 2 points")
        void smallDeviation() {
            // Actual: 14.50, Predicted: 14.70 → 1.38% deviation
            var points = pointsCalculationService.calculatePoints(14.70, 14.50);
            assertThat(points).isEqualTo(2);
        }

        @Test
        @DisplayName("3.45% deviation should award 2 points")
        void mediumDeviationWithinFivePercent() {
            // Actual: 14.50, Predicted: 15.00 → 3.45% deviation
            var points = pointsCalculationService.calculatePoints(15.00, 14.50);
            assertThat(points).isEqualTo(2);
        }

        @Test
        @DisplayName("4.83% deviation should award 2 points (just below 5%)")
        void justBelowFivePercent() {
            // Actual: 14.50, Predicted: 15.20 → 4.83% deviation
            var points = pointsCalculationService.calculatePoints(15.20, 14.50);
            assertThat(points).isEqualTo(2);
        }

        @Test
        @DisplayName("5% boundary below should award 2 points")
        void fivePercentBoundaryBelow() {
            // Actual: 14.50, Predicted: 15.225 → 5.0% deviation
            var points = pointsCalculationService.calculatePoints(15.225, 14.50);
            assertThat(points).isEqualTo(2);
        }

        @Test
        @DisplayName("5.52% deviation should award 1 point (above 5%, below 10%)")
        void betweenFiveAndTenPercent() {
            // Actual: 14.50, Predicted: 15.30 → 5.52% deviation
            var points = pointsCalculationService.calculatePoints(15.30, 14.50);
            assertThat(points).isEqualTo(1);
        }

        @Test
        @DisplayName("9.93% deviation should award 1 point (just below 10%)")
        void justBelowTenPercent() {
            // Actual: 14.50, Predicted: 15.94 → 9.93% deviation
            var points = pointsCalculationService.calculatePoints(15.94, 14.50);
            assertThat(points).isEqualTo(1);
        }

        @Test
        @DisplayName("10% boundary below should award 1 point")
        void tenPercentBoundaryBelow() {
            // Actual: 14.50, Predicted: 15.95 → 10.0% deviation
            var points = pointsCalculationService.calculatePoints(15.95, 14.50);
            assertThat(points).isEqualTo(1);
        }

        @Test
        @DisplayName("10.07% deviation should award 0 points (above 10%)")
        void justAboveTenPercent() {
            // Actual: 14.50, Predicted: 15.96 → 10.07% deviation
            var points = pointsCalculationService.calculatePoints(15.96, 14.50);
            assertThat(points).isZero();
        }

        @Test
        @DisplayName("10.34% deviation should award 0 points")
        void aboveTenPercent() {
            // Actual: 14.50, Predicted: 16.00 → 10.34% deviation
            var points = pointsCalculationService.calculatePoints(16.00, 14.50);
            assertThat(points).isZero();
        }

        @Test
        @DisplayName("13.79% deviation should award 0 points")
        void largeDeviation() {
            // Actual: 14.50, Predicted: 16.50 → 13.79% deviation
            var points = pointsCalculationService.calculatePoints(16.50, 14.50);
            assertThat(points).isZero();
        }

        @Test
        @DisplayName("Negative difference should use absolute value")
        void negativeDeviation() {
            // Predicted lower than actual should work the same way
            var points = pointsCalculationService.calculatePoints(14.00, 14.50);
            assertThat(points).isEqualTo(2); // 3.45% deviation
        }
    }

    /**
     * Tests for edge cases and boundary conditions.
     */
    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("Zero actual score with exact match should award 3 points")
        void zeroActualScoreExactMatch() {
            var points = pointsCalculationService.calculatePoints(0.00, 0.00);
            assertThat(points).isEqualTo(3);
        }

        @Test
        @DisplayName("Zero actual score with prediction should award 0 points")
        void zeroActualScoreWithPrediction() {
            // When actual is 0, percentage calculation is undefined
            // Only exact match should get points
            var points = pointsCalculationService.calculatePoints(5.00, 0.00);
            assertThat(points).isZero();
        }

        @Test
        @DisplayName("Maximum valid score (20.00) exact match should award 3 points")
        void maximumScoreExactMatch() {
            var points = pointsCalculationService.calculatePoints(20.00, 20.00);
            assertThat(points).isEqualTo(3);
        }

        @Test
        @DisplayName("Floating point precision should be handled (14.499999 vs 14.500000)")
        void floatingPointPrecision() {
            var points = pointsCalculationService.calculatePoints(14.499999, 14.500000);
            assertThat(points).isEqualTo(3); // Within 0.001 threshold
        }

        @Test
        @DisplayName("Invalid negative actual score should award 0 points")
        void invalidNegativeActualScore() {
            var points = pointsCalculationService.calculatePoints(14.50, -1.00);
            assertThat(points).isZero();
        }

        @Test
        @DisplayName("Invalid negative predicted score should award 0 points")
        void invalidNegativePredictedScore() {
            var points = pointsCalculationService.calculatePoints(-1.00, 14.50);
            assertThat(points).isZero();
        }

        @Test
        @DisplayName("Invalid too high actual score (> 20) should award 0 points")
        void invalidTooHighActualScore() {
            var points = pointsCalculationService.calculatePoints(15.00, 25.00);
            assertThat(points).isZero();
        }

        @Test
        @DisplayName("Invalid too high predicted score (> 20) should award 0 points")
        void invalidTooHighPredictedScore() {
            var points = pointsCalculationService.calculatePoints(25.00, 15.00);
            assertThat(points).isZero();
        }

        @Test
        @DisplayName("Very small actual score (0.1) with 5% deviation should work correctly")
        void verySmallActualScore() {
            // Actual: 0.1, Predicted: 0.105 → 5% deviation
            var points = pointsCalculationService.calculatePoints(0.105, 0.10);
            assertThat(points).isEqualTo(2);
        }
    }
}
