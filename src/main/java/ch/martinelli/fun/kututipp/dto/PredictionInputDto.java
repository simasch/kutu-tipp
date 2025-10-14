package ch.martinelli.fun.kututipp.dto;

import java.math.BigDecimal;

/**
 * DTO for capturing prediction input from the user.
 * Used in UC-008: Make Predictions.
 */
public record PredictionInputDto(
        Long competitionEntryId,
        BigDecimal predictedScore
) {
    /**
     * Validates the prediction according to business rules.
     * BR-008-002: Score range validation (0.000 to 20.000).
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        if (predictedScore == null) {
            return false;
        }
        var score = predictedScore.doubleValue();
        return score >= 0.0 && score <= 20.0;
    }

    /**
     * Gets validation error message if prediction is invalid.
     *
     * @return Error message, or null if valid
     */
    public String getValidationError() {
        if (predictedScore == null) {
            return "Score is required";
        }
        var score = predictedScore.doubleValue();
        if (score < 0.0 || score > 20.0) {
            return "Score must be between 0.000 and 20.000";
        }
        return null;
    }
}
