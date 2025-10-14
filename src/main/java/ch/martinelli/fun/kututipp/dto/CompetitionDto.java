package ch.martinelli.fun.kututipp.dto;

import ch.martinelli.fun.kututipp.db.enums.CompetitionStatus;

import java.time.OffsetDateTime;

/**
 * DTO representing a competition for the prediction view.
 * Used in UC-008: Make Predictions.
 */
public record CompetitionDto(
        Long id,
        String name,
        OffsetDateTime date,
        CompetitionStatus status
) {
    /**
     * Checks if predictions are still allowed for this competition.
     * Business Rule BR-008-001: Predictions must be made at least 30 minutes before competition start.
     *
     * @return true if predictions are allowed, false otherwise
     */
    public boolean isPredictionAllowed() {
        if (status != CompetitionStatus.upcoming) {
            return false;
        }
        var deadline = date.minusMinutes(30);
        return OffsetDateTime.now().isBefore(deadline);
    }

    /**
     * Gets the deadline for making predictions (30 minutes before competition start).
     *
     * @return Prediction deadline
     */
    public OffsetDateTime getPredictionDeadline() {
        return date.minusMinutes(30);
    }

    @Override
    public String toString() {
        return name;
    }
}
