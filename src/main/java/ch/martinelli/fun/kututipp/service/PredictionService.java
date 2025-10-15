package ch.martinelli.fun.kututipp.service;

import ch.martinelli.fun.kututipp.dto.CompetitionDto;
import ch.martinelli.fun.kututipp.dto.CompetitionEntryDto;
import ch.martinelli.fun.kututipp.dto.PredictionInputDto;
import ch.martinelli.fun.kututipp.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling prediction business logic.
 * Implements UC-008: Make Predictions.
 */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    private final PredictionRepository predictionRepository;

    public PredictionService(PredictionRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
    }

    /**
     * Gets all available competitions where predictions can be made.
     * BR-008-001: Only upcoming competitions with deadline not passed.
     *
     * @return List of available competitions
     */
    public List<CompetitionDto> getAvailableCompetitions() {
        log.debug("Fetching available competitions for predictions");
        return predictionRepository.getAvailableCompetitions();
    }

    /**
     * Gets all competition entries for a competition with user's existing predictions.
     *
     * @param competitionId The competition ID
     * @param userId        The user ID
     * @return List of competition entries with prediction data
     */
    public List<CompetitionEntryDto> getCompetitionEntriesWithPredictions(Long competitionId, Long userId) {
        log.debug("Fetching competition entries for competition {} and user {}", competitionId, userId);
        return predictionRepository.getCompetitionEntriesWithPredictions(competitionId, userId);
    }

    /**
     * Validates and saves a single prediction.
     * BR-008-002: Score range validation.
     * BR-008-001: Deadline validation.
     *
     * @param userId             The user ID
     * @param competitionEntryId The competition entry ID
     * @param predictedScore     The predicted score
     * @throws PredictionValidationException if validation fails
     */
    @Transactional
    public void savePrediction(Long userId, Long competitionEntryId, BigDecimal predictedScore) {
        log.debug("Saving prediction for user {} on entry {}: {}", userId, competitionEntryId, predictedScore);

        // Validate score range
        var input = new PredictionInputDto(competitionEntryId, predictedScore);
        if (!input.isValid()) {
            throw new PredictionValidationException(input.getValidationError());
        }

        // Check deadline
        var competition = predictionRepository.getCompetitionByEntryId(competitionEntryId)
                .orElseThrow(() -> new PredictionValidationException("Competition entry not found"));

        if (!competition.isPredictionAllowed()) {
            throw new PredictionDeadlinePassedException(
                    "Prediction deadline has passed for competition: " + competition.name()
            );
        }

        // Save prediction
        predictionRepository.savePrediction(userId, competitionEntryId, predictedScore);
        log.info("Prediction saved successfully for user {} on entry {}", userId, competitionEntryId);
    }

    /**
     * Validates and saves multiple predictions.
     * BR-008-004: Partial predictions allowed (minimum 1 required).
     *
     * @param userId      The user ID
     * @param predictions List of predictions to save
     * @return Number of predictions saved
     * @throws PredictionValidationException if validation fails
     */
    @Transactional
    public int savePredictions(Long userId, List<PredictionInputDto> predictions) {
        log.debug("Saving {} predictions for user {}", predictions.size(), userId);

        // BR-008-004: At least 1 prediction required
        if (predictions.isEmpty()) {
            throw new PredictionValidationException("Please enter at least one prediction");
        }

        // Validate all predictions first
        var errors = new ArrayList<String>();
        for (var i = 0; i < predictions.size(); i++) {
            var prediction = predictions.get(i);
            if (!prediction.isValid()) {
                errors.add("Entry " + (i + 1) + ": " + prediction.getValidationError());
            }
        }

        if (!errors.isEmpty()) {
            throw new PredictionValidationException("Validation errors: " + String.join(", ", errors));
        }

        // Check deadline for first entry (assuming all entries are from same competition)
        var firstEntryId = predictions.getFirst().competitionEntryId();
        var competition = predictionRepository.getCompetitionByEntryId(firstEntryId)
                .orElseThrow(() -> new PredictionValidationException("Competition entry not found"));

        if (!competition.isPredictionAllowed()) {
            throw new PredictionDeadlinePassedException(
                    "Prediction deadline has passed for competition: " + competition.name()
            );
        }

        // Save all predictions
        var savedCount = 0;
        for (var prediction : predictions) {
            predictionRepository.savePrediction(
                    userId,
                    prediction.competitionEntryId(),
                    prediction.predictedScore()
            );
            savedCount++;
        }

        log.info("Successfully saved {} predictions for user {}", savedCount, userId);
        return savedCount;
    }

    /**
     * Deletes a prediction.
     *
     * @param userId             The user ID
     * @param competitionEntryId The competition entry ID
     * @throws PredictionValidationException if deadline has passed
     */
    @Transactional
    public void deletePrediction(Long userId, Long competitionEntryId) {
        log.debug("Deleting prediction for user {} on entry {}", userId, competitionEntryId);

        // Check deadline
        var competition = predictionRepository.getCompetitionByEntryId(competitionEntryId)
                .orElseThrow(() -> new PredictionValidationException("Competition entry not found"));

        if (!competition.isPredictionAllowed()) {
            throw new PredictionDeadlinePassedException(
                    "Cannot delete prediction - deadline has passed for competition: " + competition.name()
            );
        }

        predictionRepository.deletePrediction(userId, competitionEntryId);
        log.info("Prediction deleted successfully for user {} on entry {}", userId, competitionEntryId);
    }

    /**
     * Deletes all predictions for a user for a specific competition.
     *
     * @param userId        The user ID
     * @param competitionId The competition ID
     * @return Number of predictions deleted
     * @throws PredictionValidationException if deadline has passed
     */
    @Transactional
    public int deleteAllPredictions(Long userId, Long competitionId) {
        log.debug("Deleting all predictions for user {} in competition {}", userId, competitionId);

        var deletedCount = predictionRepository.deleteAllPredictionsForCompetition(userId, competitionId);
        log.info("Deleted {} predictions for user {} in competition {}", deletedCount, userId, competitionId);
        return deletedCount;
    }

    /**
     * Gets the number of predictions made by a user for a competition.
     *
     * @param userId        The user ID
     * @param competitionId The competition ID
     * @return Number of predictions made
     */
    public int getPredictionCount(Long userId, Long competitionId) {
        return predictionRepository.countPredictionsForCompetition(userId, competitionId);
    }

    /**
     * Checks if a user has any predictions for a competition.
     *
     * @param userId        The user ID
     * @param competitionId The competition ID
     * @return true if user has predictions
     */
    public boolean hasPredictions(Long userId, Long competitionId) {
        return predictionRepository.hasPredictionsForCompetition(userId, competitionId);
    }

    /**
     * Exception thrown when prediction validation fails.
     */
    public static class PredictionValidationException extends RuntimeException {
        public PredictionValidationException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when prediction deadline has passed.
     */
    public static class PredictionDeadlinePassedException extends RuntimeException {
        public PredictionDeadlinePassedException(String message) {
            super(message);
        }
    }
}
