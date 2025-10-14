package ch.martinelli.fun.kututipp.service;

import ch.martinelli.fun.kututipp.TestcontainersConfiguration;
import ch.martinelli.fun.kututipp.db.enums.CompetitionStatus;
import ch.martinelli.fun.kututipp.db.enums.GenderType;
import ch.martinelli.fun.kututipp.db.enums.UserRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static ch.martinelli.fun.kututipp.db.Tables.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive unit and integration tests for PointsCalculationService.
 * Tests cover UC-014: Calculate Points and all related business rules.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PointsCalculationServiceTest {

    @Autowired
    private PointsCalculationService pointsCalculationService;

    @Autowired
    private DSLContext dsl;

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

    /**
     * Integration tests for database operations.
     * Tests calculatePointsForEntry() and related methods.
     */
    @Nested
    @DisplayName("Database Integration Tests")
    class DatabaseIntegrationTests {

        private Long competitionId;
        private Long gymnastId;
        private Long apparatusId;
        private Long userId;
        private Long entryId;

        @BeforeEach
        void setupTestData() {
            // Create competition (2 hours in the future)
            var futureTime = OffsetDateTime.now().plusHours(2);
            competitionId = dsl.insertInto(COMPETITION)
                    .set(COMPETITION.NAME, "Test Competition")
                    .set(COMPETITION.DATE, futureTime)
                    .set(COMPETITION.STATUS, CompetitionStatus.upcoming)
                    .returning(COMPETITION.ID)
                    .fetchOne()
                    .getId();

            // Create gymnast
            gymnastId = dsl.insertInto(GYMNAST)
                    .set(GYMNAST.NAME, "Test Gymnast")
                    .set(GYMNAST.TEAM_NAME, "Test Team")
                    .set(GYMNAST.GENDER, GenderType.M)
                    .returning(GYMNAST.ID)
                    .fetchOne()
                    .getId();

            // Create or get apparatus
            var existingApparatus = dsl.selectFrom(APPARATUS)
                    .where(APPARATUS.NAME.eq("Floor"))
                    .and(APPARATUS.GENDER.eq(GenderType.M))
                    .fetchOne();

            if (existingApparatus != null) {
                apparatusId = existingApparatus.getId();
            } else {
                apparatusId = dsl.insertInto(APPARATUS)
                        .set(APPARATUS.NAME, "Floor")
                        .set(APPARATUS.GENDER, GenderType.M)
                        .returning(APPARATUS.ID)
                        .fetchOne()
                        .getId();
            }

            // Create user
            userId = dsl.insertInto(APP_USER)
                    .set(APP_USER.USERNAME, "testuser")
                    .set(APP_USER.EMAIL, "test@example.com")
                    .set(APP_USER.PASSWORD_HASH, "hash")
                    .set(APP_USER.ROLE, UserRole.USER)
                    .returning(APP_USER.ID)
                    .fetchOne()
                    .getId();

            // Create competition entry (without actual score initially)
            entryId = dsl.insertInto(COMPETITION_ENTRY)
                    .set(COMPETITION_ENTRY.COMPETITION_ID, competitionId)
                    .set(COMPETITION_ENTRY.GYMNAST_ID, gymnastId)
                    .set(COMPETITION_ENTRY.APPARATUS_ID, apparatusId)
                    .returning(COMPETITION_ENTRY.ID)
                    .fetchOne()
                    .getId();
        }

        @Test
        @DisplayName("Calculate points for entry with single prediction")
        void calculatePointsForEntrySinglePrediction() {
            // Create prediction (made before deadline)
            var predictionTime = OffsetDateTime.now(); // Now (before competition)
            var predictionId = dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, userId)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entryId)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("14.70"))
                    .set(PREDICTION.CREATED_AT, predictionTime)
                    .returning(PREDICTION.ID)
                    .fetchOne()
                    .getId();

            // Enter actual score
            dsl.update(COMPETITION_ENTRY)
                    .set(COMPETITION_ENTRY.ACTUAL_SCORE, new BigDecimal("14.50"))
                    .where(COMPETITION_ENTRY.ID.eq(entryId))
                    .execute();

            // Calculate points
            var updatedCount = pointsCalculationService.calculatePointsForEntry(entryId);

            // Verify
            assertThat(updatedCount).isEqualTo(1);

            var prediction = dsl.selectFrom(PREDICTION)
                    .where(PREDICTION.ID.eq(predictionId))
                    .fetchOne();

            assertThat(prediction.getPointsEarned()).isEqualTo(2); // 1.38% deviation
        }

        @Test
        @DisplayName("Calculate points for entry with multiple predictions")
        void calculatePointsForEntryMultiplePredictions() {
            // Create multiple users and predictions
            var user2Id = dsl.insertInto(APP_USER)
                    .set(APP_USER.USERNAME, "user2")
                    .set(APP_USER.EMAIL, "user2@example.com")
                    .set(APP_USER.PASSWORD_HASH, "hash")
                    .set(APP_USER.ROLE, UserRole.USER)
                    .returning(APP_USER.ID)
                    .fetchOne()
                    .getId();

            var user3Id = dsl.insertInto(APP_USER)
                    .set(APP_USER.USERNAME, "user3")
                    .set(APP_USER.EMAIL, "user3@example.com")
                    .set(APP_USER.PASSWORD_HASH, "hash")
                    .set(APP_USER.ROLE, UserRole.USER)
                    .returning(APP_USER.ID)
                    .fetchOne()
                    .getId();

            // Create predictions with different accuracies
            dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, userId)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entryId)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("14.50")) // Exact
                    .execute();

            dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, user2Id)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entryId)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("15.00")) // 3.45%
                    .execute();

            dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, user3Id)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entryId)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("16.00")) // 10.34%
                    .execute();

            // Enter actual score
            dsl.update(COMPETITION_ENTRY)
                    .set(COMPETITION_ENTRY.ACTUAL_SCORE, new BigDecimal("14.50"))
                    .where(COMPETITION_ENTRY.ID.eq(entryId))
                    .execute();

            // Calculate points
            var updatedCount = pointsCalculationService.calculatePointsForEntry(entryId);

            // Verify
            assertThat(updatedCount).isEqualTo(3);

            var predictions = dsl.selectFrom(PREDICTION)
                    .where(PREDICTION.COMPETITION_ENTRY_ID.eq(entryId))
                    .orderBy(PREDICTION.USER_ID)
                    .fetch();

            assertThat(predictions.get(0).getPointsEarned()).isEqualTo(3); // Exact
            assertThat(predictions.get(1).getPointsEarned()).isEqualTo(2); // 3.45%
            assertThat(predictions.get(2).getPointsEarned()).isZero(); // 10.34%
        }

        @Test
        @DisplayName("BR-002: Prediction after deadline should get 0 points")
        void predictionAfterDeadlineShouldGetZeroPoints() {
            // Competition is 2 hours in future, deadline is 30 minutes before
            // So deadline is 1.5 hours in future
            var afterDeadline = OffsetDateTime.now().plusHours(2).minusMinutes(15); // 15 min before competition

            // Create prediction after deadline
            var predictionId = dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, userId)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entryId)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("14.50")) // Would be exact match
                    .set(PREDICTION.CREATED_AT, afterDeadline)
                    .returning(PREDICTION.ID)
                    .fetchOne()
                    .getId();

            // Enter actual score
            dsl.update(COMPETITION_ENTRY)
                    .set(COMPETITION_ENTRY.ACTUAL_SCORE, new BigDecimal("14.50"))
                    .where(COMPETITION_ENTRY.ID.eq(entryId))
                    .execute();

            // Calculate points
            pointsCalculationService.calculatePointsForEntry(entryId);

            // Verify - should get 0 points even though it's an exact match
            var prediction = dsl.selectFrom(PREDICTION)
                    .where(PREDICTION.ID.eq(predictionId))
                    .fetchOne();

            assertThat(prediction.getPointsEarned()).isZero();
        }

        @Test
        @DisplayName("Entry without actual score should throw exception")
        void entryWithoutActualScoreShouldThrowException() {
            // Don't set actual score
            assertThatThrownBy(() -> pointsCalculationService.calculatePointsForEntry(entryId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("has no actual score");
        }

        @Test
        @DisplayName("Non-existent entry should throw exception")
        void nonExistentEntryShouldThrowException() {
            assertThatThrownBy(() -> pointsCalculationService.calculatePointsForEntry(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Entry with no predictions should return 0 updated count")
        void entryWithNoPredictionsShouldReturnZero() {
            // Enter actual score
            dsl.update(COMPETITION_ENTRY)
                    .set(COMPETITION_ENTRY.ACTUAL_SCORE, new BigDecimal("14.50"))
                    .where(COMPETITION_ENTRY.ID.eq(entryId))
                    .execute();

            // Calculate points (no predictions exist)
            var updatedCount = pointsCalculationService.calculatePointsForEntry(entryId);

            assertThat(updatedCount).isZero();
        }

        @Test
        @DisplayName("BR-004: Recalculation should overwrite previous points")
        void recalculationShouldOverwritePreviousPoints() {
            // Create prediction with incorrect initial points value
            var predictionId = dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, userId)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entryId)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("14.70"))
                    .set(PREDICTION.POINTS_EARNED, 0) // Incorrect initial value (should be 2)
                    .returning(PREDICTION.ID)
                    .fetchOne()
                    .getId();

            // Enter actual score
            dsl.update(COMPETITION_ENTRY)
                    .set(COMPETITION_ENTRY.ACTUAL_SCORE, new BigDecimal("14.50"))
                    .where(COMPETITION_ENTRY.ID.eq(entryId))
                    .execute();

            // Calculate points
            pointsCalculationService.calculatePointsForEntry(entryId);

            // Verify points are overwritten with correct value
            var prediction = dsl.selectFrom(PREDICTION)
                    .where(PREDICTION.ID.eq(predictionId))
                    .fetchOne();

            assertThat(prediction.getPointsEarned()).isEqualTo(2); // Corrected value (was 0)
        }
    }

    /**
     * Tests for recalculation and batch operations.
     */
    @Nested
    @DisplayName("Recalculation and Batch Operations")
    class RecalculationTests {

        @Test
        @DisplayName("Recalculate points for entire competition")
        void recalculatePointsForCompetition() {
            // Create competition
            var futureTime = OffsetDateTime.now().plusHours(2);
            var competitionId = dsl.insertInto(COMPETITION)
                    .set(COMPETITION.NAME, "Test Competition")
                    .set(COMPETITION.DATE, futureTime)
                    .set(COMPETITION.STATUS, CompetitionStatus.upcoming)
                    .returning(COMPETITION.ID)
                    .fetchOne()
                    .getId();

            // Create 2 entries with actual scores
            var entry1Id = createEntryWithScore(competitionId, new BigDecimal("14.50"));
            var entry2Id = createEntryWithScore(competitionId, new BigDecimal("15.00"));

            // Create user
            var userId = dsl.insertInto(APP_USER)
                    .set(APP_USER.USERNAME, "testuser")
                    .set(APP_USER.EMAIL, "test@example.com")
                    .set(APP_USER.PASSWORD_HASH, "hash")
                    .set(APP_USER.ROLE, UserRole.USER)
                    .returning(APP_USER.ID)
                    .fetchOne()
                    .getId();

            // Create predictions for both entries
            dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, userId)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entry1Id)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("14.50"))
                    .execute();

            dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, userId)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entry2Id)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("15.50"))
                    .execute();

            // Recalculate all
            var updatedCount = pointsCalculationService.recalculatePointsForCompetition(competitionId);

            // Verify
            assertThat(updatedCount).isEqualTo(2);

            var predictions = dsl.selectFrom(PREDICTION)
                    .where(PREDICTION.USER_ID.eq(userId))
                    .orderBy(PREDICTION.COMPETITION_ENTRY_ID)
                    .fetch();

            assertThat(predictions.get(0).getPointsEarned()).isEqualTo(3); // Exact
            assertThat(predictions.get(1).getPointsEarned()).isEqualTo(2); // 3.33%
        }

        @Test
        @DisplayName("Recalculation for non-existent competition should throw exception")
        void recalculationForNonExistentCompetitionShouldThrowException() {
            assertThatThrownBy(() -> pointsCalculationService.recalculatePointsForCompetition(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        private Long createEntryWithScore(Long competitionId, BigDecimal actualScore) {
            // Create gymnast
            var gymnastId = dsl.insertInto(GYMNAST)
                    .set(GYMNAST.NAME, "Test Gymnast " + System.currentTimeMillis())
                    .set(GYMNAST.TEAM_NAME, "Test Team")
                    .set(GYMNAST.GENDER, GenderType.M)
                    .returning(GYMNAST.ID)
                    .fetchOne()
                    .getId();

            // Create apparatus (or reuse)
            var apparatusRecord = dsl.select(APPARATUS.ID)
                    .from(APPARATUS)
                    .where(APPARATUS.NAME.eq("Floor"))
                    .and(APPARATUS.GENDER.eq(GenderType.M))
                    .fetchOne();

            Long apparatusId;
            if (apparatusRecord == null) {
                apparatusId = dsl.insertInto(APPARATUS)
                        .set(APPARATUS.NAME, "Floor")
                        .set(APPARATUS.GENDER, GenderType.M)
                        .returning(APPARATUS.ID)
                        .fetchOne()
                        .getId();
            } else {
                apparatusId = apparatusRecord.value1();
            }

            // Create entry with score
            return dsl.insertInto(COMPETITION_ENTRY)
                    .set(COMPETITION_ENTRY.COMPETITION_ID, competitionId)
                    .set(COMPETITION_ENTRY.GYMNAST_ID, gymnastId)
                    .set(COMPETITION_ENTRY.APPARATUS_ID, apparatusId)
                    .set(COMPETITION_ENTRY.ACTUAL_SCORE, actualScore)
                    .returning(COMPETITION_ENTRY.ID)
                    .fetchOne()
                    .getId();
        }
    }

    /**
     * Tests for leaderboard calculations.
     */
    @Nested
    @DisplayName("Leaderboard Calculations")
    class LeaderboardTests {

        @Test
        @DisplayName("Calculate user competition total")
        void calculateUserCompetitionTotal() {
            // Create competition
            var futureTime = OffsetDateTime.now().plusHours(2);
            var competitionId = dsl.insertInto(COMPETITION)
                    .set(COMPETITION.NAME, "Test Competition")
                    .set(COMPETITION.DATE, futureTime)
                    .set(COMPETITION.STATUS, CompetitionStatus.upcoming)
                    .returning(COMPETITION.ID)
                    .fetchOne()
                    .getId();

            // Create user
            var userId = dsl.insertInto(APP_USER)
                    .set(APP_USER.USERNAME, "testuser")
                    .set(APP_USER.EMAIL, "test@example.com")
                    .set(APP_USER.PASSWORD_HASH, "hash")
                    .set(APP_USER.ROLE, UserRole.USER)
                    .returning(APP_USER.ID)
                    .fetchOne()
                    .getId();

            // Create 3 entries with predictions
            for (int i = 0; i < 3; i++) {
                var entryId = createEntryWithScore(competitionId, new BigDecimal("14.50"));
                dsl.insertInto(PREDICTION)
                        .set(PREDICTION.USER_ID, userId)
                        .set(PREDICTION.COMPETITION_ENTRY_ID, entryId)
                        .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("14.50"))
                        .set(PREDICTION.POINTS_EARNED, 3)
                        .execute();
            }

            // Calculate total
            var total = pointsCalculationService.calculateUserCompetitionTotal(userId, competitionId);

            assertThat(total).isEqualTo(9); // 3 predictions * 3 points each
        }

        @Test
        @DisplayName("Calculate user overall total across competitions")
        void calculateUserOverallTotal() {
            // Create 2 competitions
            var futureTime = OffsetDateTime.now().plusHours(2);
            var comp1Id = dsl.insertInto(COMPETITION)
                    .set(COMPETITION.NAME, "Competition 1")
                    .set(COMPETITION.DATE, futureTime)
                    .set(COMPETITION.STATUS, CompetitionStatus.upcoming)
                    .returning(COMPETITION.ID)
                    .fetchOne()
                    .getId();

            var comp2Id = dsl.insertInto(COMPETITION)
                    .set(COMPETITION.NAME, "Competition 2")
                    .set(COMPETITION.DATE, futureTime.plusDays(1))
                    .set(COMPETITION.STATUS, CompetitionStatus.upcoming)
                    .returning(COMPETITION.ID)
                    .fetchOne()
                    .getId();

            // Create user
            var userId = dsl.insertInto(APP_USER)
                    .set(APP_USER.USERNAME, "testuser")
                    .set(APP_USER.EMAIL, "test@example.com")
                    .set(APP_USER.PASSWORD_HASH, "hash")
                    .set(APP_USER.ROLE, UserRole.USER)
                    .returning(APP_USER.ID)
                    .fetchOne()
                    .getId();

            // Create predictions in both competitions
            var entry1Id = createEntryWithScore(comp1Id, new BigDecimal("14.50"));
            dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, userId)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entry1Id)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("14.50"))
                    .set(PREDICTION.POINTS_EARNED, 3)
                    .execute();

            var entry2Id = createEntryWithScore(comp2Id, new BigDecimal("15.00"));
            dsl.insertInto(PREDICTION)
                    .set(PREDICTION.USER_ID, userId)
                    .set(PREDICTION.COMPETITION_ENTRY_ID, entry2Id)
                    .set(PREDICTION.PREDICTED_SCORE, new BigDecimal("15.50"))
                    .set(PREDICTION.POINTS_EARNED, 2)
                    .execute();

            // Calculate overall total
            var total = pointsCalculationService.calculateUserOverallTotal(userId);

            assertThat(total).isEqualTo(5); // 3 + 2
        }

        private Long createEntryWithScore(Long competitionId, BigDecimal actualScore) {
            // Create gymnast
            var gymnastId = dsl.insertInto(GYMNAST)
                    .set(GYMNAST.NAME, "Test Gymnast " + System.currentTimeMillis())
                    .set(GYMNAST.TEAM_NAME, "Test Team")
                    .set(GYMNAST.GENDER, GenderType.M)
                    .returning(GYMNAST.ID)
                    .fetchOne()
                    .getId();

            // Get or create apparatus
            var apparatusResult = dsl.select(APPARATUS.ID)
                    .from(APPARATUS)
                    .where(APPARATUS.NAME.eq("Floor"))
                    .and(APPARATUS.GENDER.eq(GenderType.M))
                    .fetchOne();

            Long apparatusId;
            if (apparatusResult == null) {
                apparatusId = dsl.insertInto(APPARATUS)
                        .set(APPARATUS.NAME, "Floor")
                        .set(APPARATUS.GENDER, GenderType.M)
                        .returning(APPARATUS.ID)
                        .fetchOne()
                        .getId();
            } else {
                apparatusId = apparatusResult.value1();
            }

            // Create entry with score
            return dsl.insertInto(COMPETITION_ENTRY)
                    .set(COMPETITION_ENTRY.COMPETITION_ID, competitionId)
                    .set(COMPETITION_ENTRY.GYMNAST_ID, gymnastId)
                    .set(COMPETITION_ENTRY.APPARATUS_ID, apparatusId)
                    .set(COMPETITION_ENTRY.ACTUAL_SCORE, actualScore)
                    .returning(COMPETITION_ENTRY.ID)
                    .fetchOne()
                    .getId();
        }
    }
}
