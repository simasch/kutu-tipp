package ch.martinelli.fun.kututipp.dto;

import ch.martinelli.fun.kututipp.db.enums.GenderType;

import java.time.OffsetDateTime;

/**
 * Filter criteria for leaderboard queries.
 * All fields are optional - null values indicate no filtering on that dimension.
 *
 * @param competitionId Optional competition ID to filter rankings by specific competition
 * @param apparatusId   Optional apparatus ID to filter rankings by specific apparatus
 * @param gender        Optional gender filter (M/F) for gymnast-based filtering
 * @param startDate     Optional start date for date range filtering
 * @param endDate       Optional end date for date range filtering
 */
public record LeaderboardFilter(
        Long competitionId,
        Long apparatusId,
        GenderType gender,
        OffsetDateTime startDate,
        OffsetDateTime endDate
) {
    /**
     * Creates an empty filter (no filtering applied).
     */
    public static LeaderboardFilter empty() {
        return new LeaderboardFilter(null, null, null, null, null);
    }

    /**
     * Creates a filter for a specific competition.
     */
    public static LeaderboardFilter forCompetition(Long competitionId) {
        return new LeaderboardFilter(competitionId, null, null, null, null);
    }

    /**
     * Creates a filter for a specific apparatus.
     */
    public static LeaderboardFilter forApparatus(Long apparatusId) {
        return new LeaderboardFilter(null, apparatusId, null, null, null);
    }

    /**
     * Creates a filter for a specific gender.
     */
    public static LeaderboardFilter forGender(GenderType gender) {
        return new LeaderboardFilter(null, null, gender, null, null);
    }
}
