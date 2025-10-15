# UC-014: Calculate Points

## Brief Description

This use case automatically calculates points earned by users based on the accuracy of their predictions compared to
actual gymnast scores. Points are calculated on-the-fly using a PostgreSQL database function whenever needed for
leaderboards and statistics. There is no stored points value - points are always calculated fresh from the predicted
and actual scores.

## Actors

- **Primary Actor**: System - automatically calculates points on-the-fly when querying predictions
- **Secondary Actor**: Administrator - creates the conditions for calculation by entering actual scores (UC-011)

## Preconditions

- Competition entry has an actual score entered
- One or more users have made predictions for this competition entry
- Prediction was made before the prediction deadline (30 minutes before competition start)

## Postconditions

- **Success**: Points are calculated on-demand when needed for leaderboards or displays
- **Success**: Points are always accurate and reflect current actual scores
- **Success**: No stored points data to maintain or synchronize
- **Failure**: Points calculation returns 0 for invalid data

## Main Success Scenario (Basic Flow)

1. Administrator enters actual score for a competition entry (UC-011)
2. System commits the actual score to the database
3. When leaderboard or prediction results are requested:
    - System executes query that includes `calculate_points(predicted_score, actual_score)` function
    - Database function calculates points for each prediction on-the-fly
    - Database function applies BR-001 logic (exact match: 3 pts, ≤5%: 2 pts, ≤10%: 1 pt, >10%: 0 pts)
    - Query aggregates calculated points (SUM, AVG, COUNT) as needed
    - Query filters predictions by deadline (30 minutes before competition)
4. System returns calculated results to user
5. Points are always current and reflect latest actual scores

## Alternative Flows

### 3a. Prediction Made After Deadline

- At step 3, query filters out predictions made after deadline:
    1. WHERE clause excludes predictions with `created_at >= (competition.date - interval '30 minutes')`
    2. Excluded predictions contribute 0 to totals
    3. Query continues with valid predictions

### 3b. No Actual Score

- At step 3, if actual score is NULL:
    1. Database function returns 0 (or query filters out entries without actual scores)
    2. Prediction not included in leaderboard calculations
    3. Query continues with entries that have actual scores

### 3c. No Predictions Found

- At step 3, if no predictions exist for the query:
    1. Query returns empty results or zeros for aggregations
    2. No error occurs, it's a valid scenario

### 3d. Actual Score Updated

- At any time, if actual score is corrected:
    1. No recalculation needed - points are always calculated fresh
    2. Next query automatically uses new actual score
    3. Points immediately reflect the correction

## Exception Flows

### E1: Database Function Error

- If database function encounters invalid data:
    1. Function returns 0 for invalid inputs (e.g., negative scores)
    2. Query continues processing remaining rows
    3. Error logged at DEBUG level
    4. Use case ends normally with partial results

### E2: Query Timeout

- If leaderboard query takes too long:
    1. Database connection timeout occurs
    2. System returns error to user
    3. User can retry the request
    4. Consider query optimization or caching (future enhancement)

## Business Rules

### BR-001: Point Calculation Formula

Points are awarded based on the percentage deviation between predicted and actual score:

| Deviation             | Points Awarded | Description             |
|-----------------------|----------------|-------------------------|
| < 0.001 (exact match) | 3              | Exact prediction        |
| ≤ 5%                  | 2              | Within 5% deviation     |
| ≤ 10%                 | 1              | Within 10% deviation    |
| > 10%                 | 0              | More than 10% deviation |

**Database Function** (`calculate_points`):

```sql
CREATE OR REPLACE FUNCTION calculate_points(
    predicted NUMERIC(5, 3),
    actual NUMERIC(5, 3)
) RETURNS INTEGER AS
$$
DECLARE
    difference    NUMERIC;
    percentage    NUMERIC;
    exact_threshold CONSTANT NUMERIC := 0.001;
BEGIN
    -- Validate scores
    IF actual IS NULL OR predicted IS NULL THEN
        RETURN 0;
    END IF;

    IF actual < 0 OR actual > 20 OR predicted < 0 OR predicted > 20 THEN
        RETURN 0;
    END IF;

    -- Handle zero actual score
    IF actual < exact_threshold THEN
        IF ABS(predicted - actual) < exact_threshold THEN
            RETURN 3;  -- Exact match
        ELSE
            RETURN 0;
        END IF;
    END IF;

    -- Calculate deviation
    difference := ABS(predicted - actual);

    -- Check exact match
    IF difference < exact_threshold THEN
        RETURN 3;
    END IF;

    -- Calculate percentage
    percentage := (difference / actual) * 100.0;

    -- Award points
    IF percentage <= 5.0 THEN
        RETURN 2;
    ELSIF percentage <= 10.0 THEN
        RETURN 1;
    ELSE
        RETURN 0;
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
```

**Examples**:

- Actual: 14.50, Predicted: 14.50 → Difference: 0, Points: **3**
- Actual: 14.50, Predicted: 14.70 → Difference: 0.20 (1.38%), Points: **2**
- Actual: 14.50, Predicted: 15.00 → Difference: 0.50 (3.45%), Points: **2**
- Actual: 14.50, Predicted: 15.20 → Difference: 0.70 (4.83%), Points: **2**
- Actual: 14.50, Predicted: 15.30 → Difference: 0.80 (5.52%), Points: **1**
- Actual: 14.50, Predicted: 16.00 → Difference: 1.50 (10.34%), Points: **0**

### BR-002: Prediction Validity

- Only predictions submitted before the deadline are eligible for points
- Deadline: 30 minutes before competition start time
- Predictions made after deadline automatically receive 0 points
- Modified predictions use the last submission time before deadline

### BR-003: Score Constraints

- Actual score must be between 0.0 and 20.0 (typical gymnastics range)
- Predicted score must be between 0.0 and 20.0
- Scores are stored with 2 decimal places precision
- Exact match threshold is 0.001 to account for floating-point precision

### BR-004: On-the-Fly Calculation

- Points are never stored, always calculated fresh
- No recalculation needed when actual scores are updated
- Points automatically reflect latest actual scores in every query
- No synchronization issues between stored and calculated values

### BR-005: Competition Scoring

- User's competition total = sum of points for all predictions in that competition
- Overall leaderboard = sum of points across all competitions
- Apparatus-specific rankings = sum of points for specific apparatus
- Rankings are updated in real-time after each calculation

## Non-Functional Requirements

### Performance

- Single prediction calculation must complete within 50ms
- Batch calculation for 100 predictions must complete within 2 seconds
- Leaderboard update must complete within 1 second after calculation
- Use database transactions to ensure data consistency

### Accuracy

- Calculation must be deterministic (same input = same output)
- Floating-point precision must be handled correctly
- Rounding must be consistent across all calculations
- Percentage calculation must handle edge cases (actual = 0)

### Reliability

- Calculation must be idempotent (can be run multiple times safely)
- Failed calculations must not leave partial/inconsistent data
- Use database transactions for atomic updates
- Calculation must be retryable in case of transient failures

### Scalability

- System must handle calculation for 1000+ predictions per competition
- Batch processing should be used for bulk recalculations
- Consider async processing for large competitions (future enhancement)
- Database queries must be optimized with proper indexes

### Auditability

- All calculations must be logged with timestamp
- Previous point values should be retained for audit (future enhancement)
- Recalculations must be traceable to administrator actions
- Calculation errors must be logged with full context

## Data Model Impact

### Tables Affected

- `prediction` (READ) - Read predicted scores for calculation
- `competition_entry` (READ) - Read actual scores for calculation
- `competition` (READ) - Verify competition status and deadline filtering

### Database Function

- `calculate_points(predicted NUMERIC, actual NUMERIC) RETURNS INTEGER`
- Immutable function (same inputs always produce same output)
- Can be used in SELECT queries, WHERE clauses, and aggregations

### Example Queries

```sql
-- Calculate points for a single prediction
SELECT p.id,
       p.user_id,
       p.predicted_score,
       e.actual_score,
       calculate_points(p.predicted_score, e.actual_score) AS points
FROM prediction p
         JOIN competition_entry e ON p.competition_entry_id = e.id
WHERE p.id = ?;

-- Leaderboard query with aggregated points
SELECT u.id,
       u.username,
       SUM(calculate_points(p.predicted_score, e.actual_score))     AS total_points,
       COUNT(p.id)                                                   AS total_predictions,
       COUNT(CASE WHEN calculate_points(p.predicted_score, e.actual_score) = 3 THEN 1 END) AS exact_predictions
FROM app_user u
         JOIN prediction p ON u.id = p.user_id
         JOIN competition_entry e ON p.competition_entry_id = e.id
         JOIN competition c ON e.competition_id = c.id
WHERE e.actual_score IS NOT NULL
  AND p.created_at < (c.date - INTERVAL '30 minutes')
GROUP BY u.id, u.username
ORDER BY total_points DESC;
```

## Service Layer Design

### PointsCalculationService

**NOTE**: As of refactoring, points calculation has moved to the database. This Java service is kept for reference and
testing purposes only.

**Responsibilities**:

- Provide reference implementation for testing database function
- Document calculation logic for developers

**Key Method**:

```java
/**
 * Calculate points for a single prediction.
 * NOTE: This is a reference implementation. Production code uses the database function calculate_points().
 * @param predicted The predicted score
 * @param actual The actual score
 * @return Points earned (0-3)
 */
public int calculatePoints(double predicted, double actual);
```

### LeaderboardService

**Responsibilities**:

- Query and aggregate prediction points using database function
- Apply business rules (deadlines, filters)
- Calculate rankings and statistics

**Key Methods**:

```java
/**
 * Get overall leaderboard using calculated points.
 */
public List<LeaderboardEntry> getOverallLeaderboard(String currentUsername);

/**
 * Get leaderboard for specific competition.
 */
public List<LeaderboardEntry> getCompetitionLeaderboard(Long competitionId, String currentUsername);

/**
 * Get leaderboard for specific apparatus.
 */
public List<LeaderboardEntry> getApparatusLeaderboard(Long apparatusId, String currentUsername);
```

**Example Implementation**:

```java
public List<LeaderboardEntry> getOverallLeaderboard(String currentUsername) {
    var pointsField = calculatePoints(PREDICTION.PREDICTED_SCORE, COMPETITION_ENTRY.ACTUAL_SCORE);

    return dsl.select(
                    APP_USER.ID,
                    APP_USER.USERNAME,
                    sum(pointsField).as("total_points"),
                    count(PREDICTION.ID).as("total_predictions"),
                    count(when(pointsField.eq(3), 1)).as("exact_predictions")
            )
            .from(APP_USER)
            .join(PREDICTION).on(APP_USER.ID.eq(PREDICTION.USER_ID))
            .join(COMPETITION_ENTRY).on(PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID))
            .where(COMPETITION_ENTRY.ACTUAL_SCORE.isNotNull())
            .groupBy(APP_USER.ID, APP_USER.USERNAME)
            .fetch();
}
```

## Integration Points

### UC-011: Enter Actual Scores

- **Trigger**: When administrator saves actual score
- **Action**: No special action required - actual score is stored in database
- **Result**: Next leaderboard query automatically includes new points

### UC-012: Import Results (CSV/Excel)

- **Trigger**: After bulk import completes
- **Action**: Actual scores are written to database
- **Result**: Next leaderboard query automatically reflects all imported scores

### UC-015: View Leaderboard

- **Dependency**: Requires actual scores to be entered
- **Action**: Leaderboard queries use calculate_points() function to compute points on-the-fly
- **Note**: Points are always current, never stale

### UC-016: View Competition Details

- **Dependency**: Shows individual prediction results with points
- **Action**: Query uses calculate_points() to display earned points per prediction
- **Note**: Points are calculated on-demand when viewing details

## Test Scenarios

### Calculation Accuracy Tests

1. **Exact Match**: Predicted 14.50, Actual 14.50 → 3 points
2. **Small Deviation**: Predicted 14.70, Actual 14.50 → 2 points (1.38%)
3. **5% Boundary Below**: Predicted 15.22, Actual 14.50 → 2 points (4.97%)
4. **5% Boundary Above**: Predicted 15.23, Actual 14.50 → 1 point (5.03%)
5. **10% Boundary Below**: Predicted 15.94, Actual 14.50 → 1 point (9.93%)
6. **10% Boundary Above**: Predicted 15.96, Actual 14.50 → 0 points (10.07%)
7. **Large Deviation**: Predicted 16.50, Actual 14.50 → 0 points (13.79%)

### Edge Cases

8. **Zero Actual Score**: Predicted 5.00, Actual 0.00 → Handle gracefully (percentage undefined)
9. **Maximum Score**: Predicted 20.00, Actual 20.00 → 3 points
10. **Floating Point Precision**: Predicted 14.499999, Actual 14.500000 → 3 points
11. **Negative Difference**: Predicted 14.00, Actual 14.50 → Same as positive (absolute value)

### Business Rule Tests

12. **Late Prediction**: Prediction after deadline → 0 points (skipped)
13. **No Actual Score**: Entry without actual score → No calculation
14. **Multiple Predictions**: Same user, same entry → Only latest counts
15. **Score Correction**: Actual score updated → Points recalculated

### Performance Tests

16. **Single Calculation**: One prediction → < 50ms
17. **Batch Calculation**: 100 predictions → < 2 seconds
18. **Competition Recalculation**: 500 predictions → < 10 seconds
19. **Concurrent Calculations**: Multiple entries simultaneously → No data corruption

### Error Handling Tests

20. **Database Error**: Transaction failure → Rollback, no partial updates
21. **Invalid Actual Score**: Negative or > 20 → Skip, log error
22. **Missing Prediction**: Entry with no predictions → Complete successfully
23. **Null Values**: Handle null scores gracefully → Skip, log error

## Future Enhancements

1. **Bonus Points System**
    - Perfect competition bonus (all predictions within 5%)
    - Streak bonus for consecutive accurate predictions
    - Apparatus specialist bonus

2. **Weighted Scoring**
    - Different point values for finals vs. preliminaries
    - Higher points for difficult apparatus predictions
    - Adjusted scoring based on competition importance

3. **Partial Credit**
    - More granular point scale (e.g., 0-10 points)
    - Smooth curve instead of step function
    - Bonus for predicting relative ranking correctly

4. **Real-time Calculation**
    - Live calculation as scores are posted
    - Progressive leaderboard updates
    - WebSocket notifications for point changes

5. **Calculation History**
    - Track all point value changes
    - Show calculation details to users
    - Audit trail for dispute resolution

6. **Statistical Analysis**
    - Average points per user/apparatus
    - Prediction accuracy trends
    - Difficulty-adjusted scoring

7. **Manual Adjustments**
    - Administrator override for disputed scores
    - Bonus/penalty points for special circumstances
    - Retroactive adjustments with audit log

8. **Asynchronous Processing**
    - Queue-based calculation for large competitions
    - Background jobs for recalculations
    - Progress tracking for long-running calculations

## Related Use Cases

- **UC-011**: Enter Actual Scores - Triggers point calculation
- **UC-012**: Import Results - Triggers batch calculation
- **UC-015**: View Leaderboard - Depends on calculated points
- **UC-016**: View Competition Details - Displays calculated points
- **UC-008**: Make Predictions - Creates predictions that will be calculated
- **UC-010**: Edit Predictions - Updated predictions are recalculated

## Implementation Notes

### Calculation Timing

- **Immediate**: Calculate when actual score is entered (UC-011)
- **Batch**: Calculate all entries after CSV import (UC-012)
- **On-Demand**: Recalculation triggered by administrator
- **Scheduled**: Optional nightly recalculation for data consistency

### Database Optimization

```sql
-- Index for efficient prediction lookup
CREATE INDEX idx_prediction_entry_id ON prediction (entry_id);

-- Index for leaderboard queries
CREATE INDEX idx_prediction_user_competition ON prediction (user_id, competition_id);

-- Index for points aggregation
CREATE INDEX idx_prediction_points ON prediction (points) WHERE points IS NOT NULL;
```

### Transaction Boundaries

- **Single Entry**: One transaction per competition entry calculation
- **Batch Processing**: Consider batch size for memory and transaction timeout
- **Leaderboard Update**: Separate transaction after calculations complete
- **Rollback Strategy**: Failed calculation doesn't affect other entries

### Error Recovery

- **Retry Logic**: Automatic retry for transient database errors
- **Dead Letter Queue**: Failed calculations logged for manual review
- **Alerting**: Administrator notified of calculation failures
- **Data Validation**: Pre-validate data before calculation to prevent errors
