# UC-014: Calculate Points

## Brief Description

This use case automatically calculates points earned by users based on the accuracy of their predictions compared to
actual gymnast scores. Points are calculated when actual scores are entered for competition entries and are used to
generate leaderboards and statistics.

## Actors

- **Primary Actor**: System - automatically calculates points when actual scores are available
- **Secondary Actor**: Administrator - indirectly triggers calculation by entering actual scores (UC-011)

## Preconditions

- Competition entry has an actual score entered
- One or more users have made predictions for this competition entry
- Prediction was made before the prediction deadline (30 minutes before competition start)

## Postconditions

- **Success**: Points are calculated and stored for each valid prediction
- **Success**: Prediction record is updated with earned points
- **Success**: User's total score is recalculated for the competition
- **Success**: Leaderboard data is updated
- **Failure**: Points remain at 0 or previous calculated value

## Main Success Scenario (Basic Flow)

1. Administrator enters actual score for a competition entry (UC-011)
2. System commits the actual score to the database
3. System retrieves all predictions for this competition entry
4. For each prediction:
    - System retrieves the predicted score
    - System retrieves the actual score
    - System calculates the difference between predicted and actual
    - System calculates the percentage deviation
    - System determines points based on accuracy (see BR-001)
    - System updates the prediction record with earned points
5. System recalculates total points for each affected user in this competition
6. System updates leaderboard data
7. System logs the calculation completion
8. System triggers notification for affected users (optional, future enhancement)

## Alternative Flows

### 4a. Prediction Made After Deadline

- At step 4, if prediction timestamp is after the deadline:
    1. System skips this prediction (remains at 0 points)
    2. System logs the invalid prediction
    3. System continues with next prediction

### 4b. Invalid Actual Score

- At step 4, if actual score is invalid (null, negative, or unrealistic):
    1. System skips point calculation for this entry
    2. System logs the error with entry details
    3. Use case ends without updating points

### 4c. No Predictions Found

- At step 3, if no predictions exist for this competition entry:
    1. System logs that no predictions need calculation
    2. Use case ends successfully

### 5a. Recalculation Triggered

- At any time, if administrator manually triggers recalculation:
    1. System retrieves all competition entries with actual scores
    2. System repeats steps 3-6 for all entries
    3. System logs the recalculation event

## Exception Flows

### E1: Database Error During Calculation

- If database error occurs during point update:
    1. System rolls back the transaction
    2. System logs the error with details
    3. System retries calculation once
    4. If retry fails, system alerts administrator
    5. Use case ends

### E2: Concurrent Calculation Conflict

- If multiple calculations attempt to update the same prediction:
    1. System uses database locking to prevent race conditions
    2. Second calculation waits for first to complete
    3. Second calculation verifies if recalculation is still needed
    4. Use case continues normally

## Business Rules

### BR-001: Point Calculation Formula

Points are awarded based on the percentage deviation between predicted and actual score:

| Deviation             | Points Awarded | Description             |
|-----------------------|----------------|-------------------------|
| < 0.001 (exact match) | 3              | Exact prediction        |
| ≤ 5%                  | 2              | Within 5% deviation     |
| ≤ 10%                 | 1              | Within 10% deviation    |
| > 10%                 | 0              | More than 10% deviation |

**Calculation Logic**:

```java
double difference = Math.abs(predicted - actual);
double percentage = (difference / actual) * 100;

if(difference< 0.001)return 3;  // Exact (accounting for floating-point precision)
        if(percentage <=5)return 2;      // Within 5%
        if(percentage <=10)return 1;     // Within 10%
        return 0;                           // More than 10%
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

### BR-004: Recalculation Rules

- Points are recalculated if actual score is corrected/updated
- Previous points are overwritten, not accumulated
- Leaderboard is automatically updated after recalculation
- Calculation history is logged for audit purposes

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

- `prediction` (UPDATE) - Store calculated points
- `competition_entry` (READ) - Read actual scores
- `competition` (READ) - Verify competition status and timing

### Fields Updated

```sql
UPDATE prediction
SET points     = ?, -- calculated points (0-3)
    updated_at = ?  -- calculation timestamp
WHERE id = ?
  AND entry_id = ?
  AND user_id = ?;
```

### Fields Read

```sql
-- Get predictions to calculate
SELECT p.id,
       p.entry_id,
       p.user_id,
       p.predicted_score,
       p.created_at,
       e.actual_score,
       e.competition_id,
       c.start_time,
       c.status
FROM prediction p
         JOIN competition_entry e ON p.entry_id = e.id
         JOIN competition c ON e.competition_id = c.id
WHERE e.actual_score IS NOT NULL
    AND p.points IS NULL
   OR p.points != ?; -- recalculation needed
```

## Service Layer Design

### PointsCalculationService

**Responsibilities**:

- Calculate points for individual predictions
- Batch calculate points for competition entries
- Recalculate points for entire competitions
- Update leaderboard data

**Key Methods**:

```java
/**
 * Calculate points for a single prediction.
 * @param predicted The predicted score
 * @param actual The actual score
 * @return Points earned (0-3)
 */
public int calculatePoints(double predicted, double actual);

/**
 * Calculate and store points for all predictions of a competition entry.
 * @param entryId The competition entry ID
 * @return Number of predictions updated
 */
public int calculatePointsForEntry(Long entryId);

/**
 * Recalculate points for all entries in a competition.
 * @param competitionId The competition ID
 * @return Number of predictions recalculated
 */
public int recalculatePointsForCompetition(Long competitionId);

/**
 * Update leaderboard after point calculations.
 * @param competitionId The competition ID
 */
public void updateLeaderboard(Long competitionId);
```

## Integration Points

### UC-011: Enter Actual Scores

- **Trigger**: When administrator saves actual score
- **Action**: System automatically calls calculatePointsForEntry()
- **Result**: Points calculated for all predictions of that entry

### UC-012: Import Results (CSV/Excel)

- **Trigger**: After bulk import completes
- **Action**: System calls recalculatePointsForCompetition()
- **Result**: All predictions recalculated in batch

### UC-015: View Leaderboard

- **Dependency**: Requires up-to-date calculated points
- **Action**: Leaderboard queries aggregated points from predictions
- **Note**: Points must be calculated before leaderboard display

### UC-016: View Competition Details

- **Dependency**: Shows individual prediction results with points
- **Action**: Displays earned points per prediction
- **Note**: Points are pre-calculated, not calculated on-demand

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
