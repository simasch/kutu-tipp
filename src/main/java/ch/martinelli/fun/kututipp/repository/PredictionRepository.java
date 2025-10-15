package ch.martinelli.fun.kututipp.repository;

import ch.martinelli.fun.kututipp.db.enums.CompetitionStatus;
import ch.martinelli.fun.kututipp.db.tables.records.PredictionRecord;
import ch.martinelli.fun.kututipp.dto.CompetitionDto;
import ch.martinelli.fun.kututipp.dto.CompetitionEntryDto;
import ch.martinelli.fun.kututipp.dto.UserCompetitionSummaryDto;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.fun.kututipp.db.Tables.*;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.val;

/**
 * Repository for prediction database operations using jOOQ.
 * Implements UC-008: Make Predictions.
 */
@Repository
public class PredictionRepository {

    private final DSLContext dsl;

    public PredictionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Gets all available competitions for predictions.
     * BR-008-001: Only upcoming competitions with deadline not passed.
     *
     * @return List of competitions where predictions are allowed
     */
    public List<CompetitionDto> getAvailableCompetitions() {
        var now = OffsetDateTime.now();
        var deadline = now.plusMinutes(30);

        return dsl.select(
                        COMPETITION.ID,
                        COMPETITION.NAME,
                        COMPETITION.DATE,
                        COMPETITION.STATUS
                )
                .from(COMPETITION)
                .where(COMPETITION.STATUS.eq(CompetitionStatus.upcoming))
                .and(COMPETITION.DATE.gt(deadline))
                .orderBy(COMPETITION.DATE.asc())
                .fetch(Records.mapping(CompetitionDto::new));
    }

    /**
     * Gets all competition entries for a specific competition with user's predictions if they exist.
     *
     * @param competitionId The competition ID
     * @param userId        The user ID
     * @return List of competition entries with prediction data
     */
    public List<CompetitionEntryDto> getCompetitionEntriesWithPredictions(Long competitionId, Long userId) {
        return dsl.select(
                        COMPETITION_ENTRY.ID,
                        GYMNAST.NAME,
                        GYMNAST.TEAM_NAME,
                        GYMNAST.GENDER,
                        APPARATUS.NAME,
                        PREDICTION.PREDICTED_SCORE,
                        COMPETITION_ENTRY.ACTUAL_SCORE
                )
                .from(COMPETITION_ENTRY)
                .join(GYMNAST).on(COMPETITION_ENTRY.GYMNAST_ID.eq(GYMNAST.ID))
                .join(APPARATUS).on(COMPETITION_ENTRY.APPARATUS_ID.eq(APPARATUS.ID))
                .leftJoin(PREDICTION).on(
                        PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID)
                                .and(PREDICTION.USER_ID.eq(userId))
                )
                .where(COMPETITION_ENTRY.COMPETITION_ID.eq(competitionId))
                .orderBy(GYMNAST.NAME, APPARATUS.NAME)
                .fetch(Records.mapping(CompetitionEntryDto::new));
    }

    /**
     * Saves or updates a prediction for a user.
     * BR-008-003: One prediction per user per competition entry.
     *
     * @param userId             The user ID
     * @param competitionEntryId The competition entry ID
     * @param predictedScore     The predicted score
     * @return The created or updated prediction record
     */
    public PredictionRecord savePrediction(Long userId, Long competitionEntryId, BigDecimal predictedScore) {
        var now = OffsetDateTime.now();

        // Check if prediction already exists
        var existingPrediction = dsl.selectFrom(PREDICTION)
                .where(PREDICTION.USER_ID.eq(userId))
                .and(PREDICTION.COMPETITION_ENTRY_ID.eq(competitionEntryId))
                .fetchOptional();

        if (existingPrediction.isPresent()) {
            // Update existing prediction
            var prediction = existingPrediction.get();
            prediction.setPredictedScore(predictedScore);
            prediction.setUpdatedAt(now);
            prediction.update();
            return prediction;
        } else {
            // Create new prediction
            var prediction = dsl.newRecord(PREDICTION);
            prediction.setUserId(userId);
            prediction.setCompetitionEntryId(competitionEntryId);
            prediction.setPredictedScore(predictedScore);
            prediction.setPointsEarned(null); // Will be calculated later
            prediction.setCreatedAt(now);
            prediction.setUpdatedAt(now);
            prediction.store();
            return prediction;
        }
    }

    /**
     * Deletes a prediction.
     *
     * @param userId             The user ID
     * @param competitionEntryId The competition entry ID
     * @return Number of deleted records (0 or 1)
     */
    public int deletePrediction(Long userId, Long competitionEntryId) {
        return dsl.deleteFrom(PREDICTION)
                .where(PREDICTION.USER_ID.eq(userId))
                .and(PREDICTION.COMPETITION_ENTRY_ID.eq(competitionEntryId))
                .execute();
    }

    /**
     * Deletes all predictions for a user for a specific competition.
     *
     * @param userId        The user ID
     * @param competitionId The competition ID
     * @return Number of deleted records
     */
    public int deleteAllPredictionsForCompetition(Long userId, Long competitionId) {
        return dsl.deleteFrom(PREDICTION)
                .where(PREDICTION.USER_ID.eq(userId))
                .and(PREDICTION.COMPETITION_ENTRY_ID.in(
                        dsl.select(COMPETITION_ENTRY.ID)
                                .from(COMPETITION_ENTRY)
                                .where(COMPETITION_ENTRY.COMPETITION_ID.eq(competitionId))
                ))
                .execute();
    }

    /**
     * Gets a specific prediction.
     *
     * @param userId             The user ID
     * @param competitionEntryId The competition entry ID
     * @return Optional containing the prediction if found
     */
    public Optional<PredictionRecord> getPrediction(Long userId, Long competitionEntryId) {
        return dsl.selectFrom(PREDICTION)
                .where(PREDICTION.USER_ID.eq(userId))
                .and(PREDICTION.COMPETITION_ENTRY_ID.eq(competitionEntryId))
                .fetchOptional();
    }

    /**
     * Counts the number of predictions made by a user for a specific competition.
     *
     * @param userId        The user ID
     * @param competitionId The competition ID
     * @return Number of predictions made
     */
    public int countPredictionsForCompetition(Long userId, Long competitionId) {
        return dsl.fetchCount(
                dsl.selectFrom(PREDICTION)
                        .where(PREDICTION.USER_ID.eq(userId))
                        .and(PREDICTION.COMPETITION_ENTRY_ID.in(
                                dsl.select(COMPETITION_ENTRY.ID)
                                        .from(COMPETITION_ENTRY)
                                        .where(COMPETITION_ENTRY.COMPETITION_ID.eq(competitionId))
                        ))
        );
    }

    /**
     * Checks if a user has any predictions for a specific competition.
     *
     * @param userId        The user ID
     * @param competitionId The competition ID
     * @return true if user has predictions, false otherwise
     */
    public boolean hasPredictionsForCompetition(Long userId, Long competitionId) {
        return dsl.fetchExists(
                dsl.selectFrom(PREDICTION)
                        .where(PREDICTION.USER_ID.eq(userId))
                        .and(PREDICTION.COMPETITION_ENTRY_ID.in(
                                dsl.select(COMPETITION_ENTRY.ID)
                                        .from(COMPETITION_ENTRY)
                                        .where(COMPETITION_ENTRY.COMPETITION_ID.eq(competitionId))
                        ))
        );
    }

    /**
     * Gets the competition for a specific competition entry.
     *
     * @param competitionEntryId The competition entry ID
     * @return Optional containing the competition if found
     */
    public Optional<CompetitionDto> getCompetitionByEntryId(Long competitionEntryId) {
        return dsl.select(
                        COMPETITION.ID,
                        COMPETITION.NAME,
                        COMPETITION.DATE,
                        COMPETITION.STATUS
                )
                .from(COMPETITION)
                .join(COMPETITION_ENTRY).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
                .where(COMPETITION_ENTRY.ID.eq(competitionEntryId))
                .fetchOptional(Records.mapping(CompetitionDto::new));
    }

    /**
     * Gets all competitions where the user has made predictions.
     * UC-010: For viewing and editing existing predictions.
     *
     * @param userId The user ID
     * @return List of competitions with prediction summary
     */
    public List<UserCompetitionSummaryDto> getCompetitionsWithPredictions(Long userId) {
        var now = OffsetDateTime.now();
        var deadline = now.plusMinutes(30);

        // Subquery to count total entries per competition
        var totalEntriesSubquery = dsl.select(
                        COMPETITION_ENTRY.COMPETITION_ID,
                        count(COMPETITION_ENTRY.ID).as("total_entries")
                )
                .from(COMPETITION_ENTRY)
                .groupBy(COMPETITION_ENTRY.COMPETITION_ID)
                .asTable("total_entries");

        // Subquery to count predicted entries per competition for this user
        var predictedEntriesSubquery = dsl.select(
                        COMPETITION_ENTRY.COMPETITION_ID,
                        count(PREDICTION.ID).as("predicted_entries")
                )
                .from(PREDICTION)
                .join(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
                .where(PREDICTION.USER_ID.eq(userId))
                .groupBy(COMPETITION_ENTRY.COMPETITION_ID)
                .asTable("predicted_entries");

        return dsl.select(
                        COMPETITION.ID,
                        COMPETITION.NAME,
                        COMPETITION.DATE,
                        COMPETITION.STATUS,
                        totalEntriesSubquery.field("total_entries", Integer.class),
                        predictedEntriesSubquery.field("predicted_entries", Integer.class),
                        COMPETITION.STATUS.eq(CompetitionStatus.upcoming).and(COMPETITION.DATE.gt(val(deadline)))
                )
                .from(COMPETITION)
                .join(totalEntriesSubquery).on(
                        totalEntriesSubquery.field(COMPETITION_ENTRY.COMPETITION_ID).eq(COMPETITION.ID)
                )
                .join(predictedEntriesSubquery).on(
                        predictedEntriesSubquery.field(COMPETITION_ENTRY.COMPETITION_ID).eq(COMPETITION.ID)
                )
                .orderBy(COMPETITION.DATE.desc())
                .fetch(Records.mapping(UserCompetitionSummaryDto::new));
    }
}
