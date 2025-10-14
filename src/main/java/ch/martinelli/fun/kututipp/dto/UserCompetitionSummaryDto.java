package ch.martinelli.fun.kututipp.dto;

import ch.martinelli.fun.kututipp.db.enums.CompetitionStatus;

import java.time.OffsetDateTime;

/**
 * DTO representing a user's prediction summary for a competition.
 * Used in UC-010: Edit Predictions to show which competitions have predictions.
 */
public record UserCompetitionSummaryDto(
        Long competitionId,
        String competitionName,
        OffsetDateTime competitionDate,
        CompetitionStatus status,
        int totalEntries,
        int predictedEntries,
        boolean isEditable
) {
    /**
     * Gets the prediction deadline for this competition.
     *
     * @return Prediction deadline (30 minutes before competition start)
     */
    public OffsetDateTime getPredictionDeadline() {
        return competitionDate.minusMinutes(30);
    }

    /**
     * Gets the completion percentage.
     *
     * @return Percentage of entries predicted (0-100)
     */
    public int getCompletionPercentage() {
        if (totalEntries == 0) {
            return 0;
        }
        return (predictedEntries * 100) / totalEntries;
    }

    /**
     * Checks if all entries have predictions.
     *
     * @return true if all entries are predicted
     */
    public boolean isComplete() {
        return totalEntries > 0 && predictedEntries == totalEntries;
    }
}
