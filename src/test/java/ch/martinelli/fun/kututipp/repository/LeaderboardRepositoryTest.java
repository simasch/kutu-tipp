package ch.martinelli.fun.kututipp.repository;

import ch.martinelli.fun.kututipp.TestcontainersConfiguration;
import ch.martinelli.fun.kututipp.db.enums.CompetitionStatus;
import ch.martinelli.fun.kututipp.db.enums.GenderType;
import ch.martinelli.fun.kututipp.db.enums.UserRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static ch.martinelli.fun.kututipp.db.Tables.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class LeaderboardRepositoryTest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    private Long competition1Id;
    private Long competition2Id;
    private Long user1Id;
    private Long user2Id;
    private Long user3Id;

    @BeforeEach
    void setUp() {
        // Clean up existing test data
        dsl.deleteFrom(PREDICTION).execute();
        dsl.deleteFrom(COMPETITION_ENTRY).execute();
        dsl.deleteFrom(COMPETITION).execute();
        dsl.deleteFrom(GYMNAST).execute();
        dsl.deleteFrom(APPARATUS).execute();
        dsl.deleteFrom(APP_USER).execute();

        // Create test data
        setupTestData();
    }

    @Test
    void shouldReturnLeaderboardForSpecificCompetition() {
        // Act
        var results = leaderboardRepository.getCompetitionLeaderboard(competition1Id);

        // Assert
        assertThat(results).hasSize(3);

        // Verify leaderboard structure and data
        var firstPlace = results.get(0);
        assertThat(firstPlace.getValue("username")).isEqualTo("alice");
        assertThat(firstPlace.getValue("rank", Integer.class)).isEqualTo(1);
        assertThat(firstPlace.getValue("total_points", Integer.class)).isEqualTo(6); // 3+3
        assertThat(firstPlace.getValue("total_predictions", Integer.class)).isEqualTo(2);
        assertThat(firstPlace.getValue("exact_predictions", Integer.class)).isEqualTo(2);

        var secondPlace = results.get(1);
        assertThat(secondPlace.getValue("username")).isEqualTo("bob");
        assertThat(secondPlace.getValue("rank", Integer.class)).isEqualTo(2);
        assertThat(secondPlace.getValue("total_points", Integer.class)).isEqualTo(4); // 2+2
        assertThat(secondPlace.getValue("total_predictions", Integer.class)).isEqualTo(2);
        assertThat(secondPlace.getValue("exact_predictions", Integer.class)).isZero();

        var thirdPlace = results.get(2);
        assertThat(thirdPlace.getValue("username")).isEqualTo("charlie");
        assertThat(thirdPlace.getValue("rank", Integer.class)).isEqualTo(3);
        assertThat(thirdPlace.getValue("total_points", Integer.class)).isEqualTo(2); // 1+1
        assertThat(thirdPlace.getValue("total_predictions", Integer.class)).isEqualTo(2);
        assertThat(thirdPlace.getValue("exact_predictions", Integer.class)).isZero();
    }

    @Test
    void shouldOnlyIncludeEntriesWithActualScores() {
        // Create a prediction for an entry without an actual score
        var competitionEntryIdWithoutScore = createCompetitionEntry(
                competition1Id,
                createGymnast("John Doe", "Team A", GenderType.M),
                createApparatus("Floor", GenderType.M),
                null // No actual score
        );

        createPrediction(user1Id, competitionEntryIdWithoutScore, new BigDecimal("14.500"));

        // Act
        var results = leaderboardRepository.getCompetitionLeaderboard(competition1Id);

        // Assert - Alice should still have only 2 predictions counted (not 3)
        var aliceResult = results.stream()
                .filter(r -> r.getValue("username").equals("alice"))
                .findFirst()
                .orElseThrow();

        assertThat(aliceResult.getValue("total_predictions", Integer.class)).isEqualTo(2);
    }

    @Test
    void shouldHandleTieBreakingByExactPredictions() {
        // Clean up and create a tie scenario
        dsl.deleteFrom(PREDICTION).execute();

        // Both users get same total points (6), but different exact predictions
        // User1: 3+3 (2 exact predictions)
        var entry1 = createCompetitionEntry(
                competition1Id,
                createGymnast("Gymnast 1", "Team A", GenderType.M),
                createApparatus("Rings", GenderType.M),
                new BigDecimal("14.500")
        );
        createPrediction(user1Id, entry1, new BigDecimal("14.500")); // Exact: 3 points

        var entry2 = createCompetitionEntry(
                competition1Id,
                createGymnast("Gymnast 2", "Team A", GenderType.M),
                createApparatus("Parallel Bars", GenderType.M),
                new BigDecimal("13.000")
        );
        createPrediction(user1Id, entry2, new BigDecimal("13.000")); // Exact: 3 points

        // User2: 2+2+2 (0 exact predictions, 3 within 5%)
        createPrediction(user2Id, entry1, new BigDecimal("14.200")); // Within 5%: 2 points
        createPrediction(user2Id, entry2, new BigDecimal("12.800")); // Within 5%: 2 points

        var entry3 = createCompetitionEntry(
                competition1Id,
                createGymnast("Gymnast 3", "Team A", GenderType.M),
                createApparatus("High Bar", GenderType.M),
                new BigDecimal("15.000")
        );
        createPrediction(user2Id, entry3, new BigDecimal("14.800")); // Within 5%: 2 points

        // Act
        var results = leaderboardRepository.getCompetitionLeaderboard(competition1Id);

        // Assert - User1 should rank higher due to more exact predictions
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);

        var firstPlace = results.getFirst();
        assertThat(firstPlace.getValue("username")).isEqualTo("alice");
        assertThat(firstPlace.getValue("rank", Integer.class)).isEqualTo(1);
        assertThat(firstPlace.getValue("total_points", Integer.class)).isEqualTo(6);
        assertThat(firstPlace.getValue("exact_predictions", Integer.class)).isEqualTo(2);

        var secondPlace = results.get(1);
        assertThat(secondPlace.getValue("username")).isEqualTo("bob");
        assertThat(secondPlace.getValue("rank", Integer.class)).isEqualTo(2);
        assertThat(secondPlace.getValue("total_points", Integer.class)).isEqualTo(6);
        assertThat(secondPlace.getValue("exact_predictions", Integer.class)).isZero();
    }

    @Test
    void shouldHandleTieBreakingByTotalPredictions() {
        // Clean up and create a tie scenario
        dsl.deleteFrom(PREDICTION).execute();

        // Both users get same total points (5) and same exact predictions (1)
        // User1: 3+2 (1 exact, 1 within 5%) - 2 predictions
        var entry1 = createCompetitionEntry(
                competition1Id,
                createGymnast("Gymnast 1", "Team A", GenderType.M),
                createApparatus("Rings", GenderType.M),
                new BigDecimal("14.000")
        );
        createPrediction(user1Id, entry1, new BigDecimal("14.000")); // Exact: 3 points

        var entry2 = createCompetitionEntry(
                competition1Id,
                createGymnast("Gymnast 2", "Team A", GenderType.M),
                createApparatus("Parallel Bars", GenderType.M),
                new BigDecimal("14.000")
        );
        createPrediction(user1Id, entry2, new BigDecimal("13.500")); // Within 5% (0.5 < 0.7): 2 points

        // User2: 3+1+1 (1 exact, 2 within 10%) - 3 predictions
        createPrediction(user2Id, entry1, new BigDecimal("14.000")); // Exact: 3 points
        createPrediction(user2Id, entry2, new BigDecimal("13.000")); // Within 10% (1.0 < 1.4): 1 point

        var entry3 = createCompetitionEntry(
                competition1Id,
                createGymnast("Gymnast 3", "Team A", GenderType.M),
                createApparatus("High Bar", GenderType.M),
                new BigDecimal("15.000")
        );
        createPrediction(user2Id, entry3, new BigDecimal("14.000")); // Within 10% (1.0 < 1.5): 1 point

        // Act
        var results = leaderboardRepository.getCompetitionLeaderboard(competition1Id);

        // Assert - User2 should rank higher due to more total predictions
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);

        var firstPlace = results.getFirst();
        assertThat(firstPlace.getValue("username")).isEqualTo("bob");
        assertThat(firstPlace.getValue("rank", Integer.class)).isEqualTo(1);
        assertThat(firstPlace.getValue("total_points", Integer.class)).isEqualTo(5);
        assertThat(firstPlace.getValue("exact_predictions", Integer.class)).isEqualTo(1);
        assertThat(firstPlace.getValue("total_predictions", Integer.class)).isEqualTo(3);

        var secondPlace = results.get(1);
        assertThat(secondPlace.getValue("username")).isEqualTo("alice");
        assertThat(secondPlace.getValue("rank", Integer.class)).isEqualTo(2);
        assertThat(secondPlace.getValue("total_points", Integer.class)).isEqualTo(5);
        assertThat(secondPlace.getValue("exact_predictions", Integer.class)).isEqualTo(1);
        assertThat(secondPlace.getValue("total_predictions", Integer.class)).isEqualTo(2);
    }

    @Test
    void shouldOnlyIncludePredictionsFromSpecificCompetition() {
        // Act - Get leaderboard for competition1
        var results = leaderboardRepository.getCompetitionLeaderboard(competition1Id);

        // Assert
        assertThat(results).hasSize(3);

        // Verify that scores are only from competition1
        var aliceResult = results.stream()
                .filter(r -> r.getValue("username").equals("alice"))
                .findFirst()
                .orElseThrow();

        // Alice should have 6 points from competition1 (not 11 from both competitions)
        assertThat(aliceResult.getValue("total_points", Integer.class)).isEqualTo(6);
    }

    @Test
    void shouldReturnEmptyLeaderboardForCompetitionWithNoPredictions() {
        // Create a new competition with no predictions
        var emptyCompetitionId = dsl.insertInto(COMPETITION)
                .set(COMPETITION.NAME, "Empty Competition")
                .set(COMPETITION.DATE, OffsetDateTime.now())
                .set(COMPETITION.STATUS, CompetitionStatus.upcoming)
                .returningResult(COMPETITION.ID)
                .fetchOne()
                .value1();

        // Act
        var results = leaderboardRepository.getCompetitionLeaderboard(emptyCompetitionId);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void shouldCalculateAveragePointsCorrectly() {
        // Act
        var results = leaderboardRepository.getCompetitionLeaderboard(competition1Id);

        // Assert
        var aliceResult = results.stream()
                .filter(r -> r.getValue("username").equals("alice"))
                .findFirst()
                .orElseThrow();

        // Alice: 6 points / 2 predictions = 3.0 average
        var avgPoints = aliceResult.getValue("avg_points", BigDecimal.class);
        assertThat(avgPoints).isEqualByComparingTo("3.0");

        var bobResult = results.stream()
                .filter(r -> r.getValue("username").equals("bob"))
                .findFirst()
                .orElseThrow();

        // Bob: 4 points / 2 predictions = 2.0 average
        avgPoints = bobResult.getValue("avg_points", BigDecimal.class);
        assertThat(avgPoints).isEqualByComparingTo("2.0");
    }

    // Helper methods for test data setup

    private void setupTestData() {
        // Create competitions
        competition1Id = createCompetition("Swiss Cup Final 2025", OffsetDateTime.now());
        competition2Id = createCompetition("Swiss Cup Semifinal 2025", OffsetDateTime.now().plusDays(7));

        // Create users
        user1Id = createUser("alice", "alice@example.com");
        user2Id = createUser("bob", "bob@example.com");
        user3Id = createUser("charlie", "charlie@example.com");

        // Create gymnasts
        var gymnast1Id = createGymnast("Max Müller", "TV Wil", GenderType.M);
        var gymnast2Id = createGymnast("Anna Schmidt", "STV Baden", GenderType.F);

        // Create apparatus
        var pommelHorseId = createApparatus("Pommel Horse", GenderType.M);
        var vaultId = createApparatus("Vault", GenderType.F);

        // Create competition entries for competition 1 with actual scores
        var entry1Id = createCompetitionEntry(competition1Id, gymnast1Id, pommelHorseId, new BigDecimal("14.500"));
        var entry2Id = createCompetitionEntry(competition1Id, gymnast2Id, vaultId, new BigDecimal("13.800"));

        // Create competition entries for competition 2 with actual scores
        var entry3Id = createCompetitionEntry(competition2Id, gymnast1Id, pommelHorseId, new BigDecimal("15.200"));

        // Create predictions for competition 1
        // User1 (alice): 2 exact predictions = 6 points
        createPrediction(user1Id, entry1Id, new BigDecimal("14.500")); // Exact: 3 points
        createPrediction(user1Id, entry2Id, new BigDecimal("13.800")); // Exact: 3 points

        // User2 (bob): 2 predictions within 5% = 4 points
        createPrediction(user2Id, entry1Id, new BigDecimal("14.200")); // Within 5%: 2 points
        createPrediction(user2Id, entry2Id, new BigDecimal("13.500")); // Within 5%: 2 points

        // User3 (charlie): 2 predictions within 10% = 2 points
        createPrediction(user3Id, entry1Id, new BigDecimal("13.200")); // Within 10%: 1 point
        createPrediction(user3Id, entry2Id, new BigDecimal("12.600")); // Within 10%: 1 point

        // Create predictions for competition 2 (should not affect competition 1 leaderboard)
        createPrediction(user1Id, entry3Id, new BigDecimal("15.200")); // Exact: 3 points (but in competition 2)
        createPrediction(user2Id, entry3Id, new BigDecimal("15.000")); // Within 5%: 2 points (but in competition 2)
    }

    private Long createCompetition(String name, OffsetDateTime date) {
        return dsl.insertInto(COMPETITION)
                .set(COMPETITION.NAME, name)
                .set(COMPETITION.DATE, date)
                .set(COMPETITION.STATUS, CompetitionStatus.finished)
                .returningResult(COMPETITION.ID)
                .fetchOne()
                .value1();
    }

    private Long createUser(String username, String email) {
        return dsl.insertInto(APP_USER)
                .set(APP_USER.USERNAME, username)
                .set(APP_USER.EMAIL, email)
                .set(APP_USER.PASSWORD_HASH, "dummy_hash")
                .set(APP_USER.ROLE, UserRole.USER)
                .returningResult(APP_USER.ID)
                .fetchOne()
                .value1();
    }

    private Long createGymnast(String name, String teamName, GenderType gender) {
        return dsl.insertInto(GYMNAST)
                .set(GYMNAST.NAME, name)
                .set(GYMNAST.TEAM_NAME, teamName)
                .set(GYMNAST.GENDER, gender)
                .returningResult(GYMNAST.ID)
                .fetchOne()
                .value1();
    }

    private Long createApparatus(String name, GenderType gender) {
        return dsl.insertInto(APPARATUS)
                .set(APPARATUS.NAME, name)
                .set(APPARATUS.GENDER, gender)
                .returningResult(APPARATUS.ID)
                .fetchOne()
                .value1();
    }

    private Long createCompetitionEntry(Long competitionId, Long gymnastId, Long apparatusId, BigDecimal actualScore) {
        return dsl.insertInto(COMPETITION_ENTRY)
                .set(COMPETITION_ENTRY.COMPETITION_ID, competitionId)
                .set(COMPETITION_ENTRY.GYMNAST_ID, gymnastId)
                .set(COMPETITION_ENTRY.APPARATUS_ID, apparatusId)
                .set(COMPETITION_ENTRY.ACTUAL_SCORE, actualScore)
                .returningResult(COMPETITION_ENTRY.ID)
                .fetchOne()
                .value1();
    }

    private void createPrediction(Long userId, Long competitionEntryId, BigDecimal predictedScore) {
        dsl.insertInto(PREDICTION)
                .set(PREDICTION.USER_ID, userId)
                .set(PREDICTION.COMPETITION_ENTRY_ID, competitionEntryId)
                .set(PREDICTION.PREDICTED_SCORE, predictedScore)
                .execute();
    }
}
