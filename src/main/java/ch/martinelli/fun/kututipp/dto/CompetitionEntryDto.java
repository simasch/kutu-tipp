package ch.martinelli.fun.kututipp.dto;

import ch.martinelli.fun.kututipp.db.enums.GenderType;

import java.math.BigDecimal;

/**
 * DTO representing a competition entry with gymnast and apparatus information.
 * Used in UC-008: Make Predictions to display the grid of available predictions.
 */
public record CompetitionEntryDto(
        Long competitionEntryId,
        String gymnastName,
        String teamName,
        GenderType gender,
        String apparatusName,
        BigDecimal predictedScore,  // The user's predicted score (if any)
        BigDecimal actualScore      // The actual score (null if not yet available)
) {
    /**
     * Checks if the user has already made a prediction for this entry.
     *
     * @return true if a prediction exists
     */
    public boolean hasPrediction() {
        return predictedScore != null;
    }
}
