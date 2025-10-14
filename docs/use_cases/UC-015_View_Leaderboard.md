# UC-015: View Leaderboard

## Brief Description

This use case allows users and administrators to view rankings and standings of all participants in the prediction game.
The leaderboard displays user rankings based on points earned from prediction accuracy, with various filtering and
sorting options to view overall standings, competition-specific rankings, and apparatus-based statistics.

## Actors

- **Primary Actor**: Tipper (User) - wants to see their ranking and compare with other participants
- **Secondary Actor**: Administrator - can view all leaderboards and user statistics
- **Supporting Actor**: System (UC-014) - provides calculated points for rankings

## Preconditions

- User is authenticated and logged in (UC-002)
- At least one competition has been created
- At least one user has made predictions
- Points have been calculated for at least some predictions (UC-014)

## Postconditions

- **Success**: User views leaderboard with current rankings
- **Success**: User can see their own ranking highlighted
- **Success**: Leaderboard reflects latest point calculations
- **Failure**: No changes to system state

## Main Success Scenario (Basic Flow)

1. User navigates to the leaderboard page
2. System retrieves all users with their total points across all competitions
3. System calculates rankings based on total points (highest to lowest)
4. System displays the leaderboard with the following columns:
    - Rank (position)
    - Username
    - Total points
    - Number of predictions made
    - Average points per prediction
5. System highlights the current user's row in the leaderboard
6. User views their ranking and compares with other participants
7. User can scroll through the complete ranking list
8. User can select different filter options (see Alternative Flows)

## Alternative Flows

### 2a. Filter by Competition

- At step 2, user selects a specific competition from dropdown:
    1. System retrieves points only for the selected competition
    2. System recalculates rankings for that competition
    3. System displays competition-specific leaderboard
    4. Use case continues at step 4

### 2b. Filter by Apparatus

- At step 2, user selects a specific apparatus (e.g., Floor, Vault):
    1. System retrieves points only for predictions on that apparatus
    2. System calculates apparatus-specific rankings
    3. System displays apparatus leaderboard with additional column showing apparatus name
    4. Use case continues at step 4

### 2c. Filter by Gender

- At step 2, user selects gender filter (Men's/Women's):
    1. System retrieves points only for gymnasts of selected gender
    2. System recalculates rankings based on filtered data
    3. Use case continues at step 4

### 2d. Filter by Date Range

- At step 2, user specifies start and end dates:
    1. System retrieves competitions within the date range
    2. System calculates points only from those competitions
    3. Use case continues at step 4

### 4a. View Top 10 Only

- At step 4, user toggles "Top 10" filter:
    1. System displays only the top 10 ranked users
    2. If current user is not in top 10, system shows their ranking separately below
    3. User can toggle back to view full rankings

### 4b. View User Details

- At step 4, user clicks on a username:
    1. System navigates to that user's prediction history (UC-016)
    2. User can view detailed statistics for that user
    3. User returns to leaderboard

### 5a. No Predictions Yet

- At step 5, if user hasn't made any predictions:
    1. System displays message: "You haven't made any predictions yet. Start predicting to join the leaderboard!"
    2. System provides link to current competitions (UC-003)
    3. Use case continues normally

### 8a. Refresh Rankings

- At any time, user clicks "Refresh" button:
    1. System re-fetches latest point calculations
    2. System updates rankings in real-time
    3. System shows timestamp of last update
    4. Use case continues at step 3

### 8b. Export Leaderboard

- At step 8, user clicks "Export" button:
    1. System generates CSV or Excel file with current leaderboard data
    2. System downloads file to user's device
    3. Use case continues normally

### 8c. View Prediction Breakdown

- At step 8, user clicks on their own ranking:
    1. System displays modal with breakdown of their points:
        - Points by competition
        - Points by apparatus
        - Best predictions (3 points)
        - Accuracy statistics
    2. User closes modal
    3. Use case continues normally

## Exception Flows

### E1: No Calculated Points Available

- If no points have been calculated yet:
    1. System displays message: "No rankings available yet. Points will appear after competitions have results entered."
    2. System shows empty leaderboard with explanation
    3. Use case ends

### E2: Database Error During Ranking Calculation

- If database error occurs at step 2 or 3:
    1. System logs the error
    2. System displays error: "Unable to load leaderboard. Please try again later."
    3. System shows cached leaderboard data if available (with timestamp)
    4. Use case ends

### E3: Performance Timeout

- If ranking calculation takes too long (>5 seconds):
    1. System displays loading indicator
    2. System uses async processing to complete calculation
    3. System shows partial results with "Loading..." indicator
    4. System updates display when calculation completes

## Business Rules

### BR-001: Ranking Calculation

- **Primary Ranking**: Total points (sum of all prediction points)
- **Tie Breaker 1**: Number of exact predictions (3 points)
- **Tie Breaker 2**: Total number of predictions made (more predictions = higher rank)
- **Tie Breaker 3**: Earlier registration date
- **Rank Display**: 1st, 2nd, 3rd, 4th... (ordinal numbers)

### BR-002: Points Aggregation

- **Overall Leaderboard**: Sum of all points from all competitions
- **Competition Leaderboard**: Sum of points from single competition only
- **Apparatus Leaderboard**: Sum of points from specific apparatus across all competitions
- **Only Valid Predictions**: Only predictions made before deadline count
- **Completed Competitions**: Can filter to show only finished competitions

### BR-003: User Privacy

- **Public Information**: Username and total points visible to all
- **Private Information**: Email and personal details not shown
- **Anonymous Viewing**: Non-authenticated users cannot view leaderboard
- **Admin View**: Administrators can see additional statistics (prediction patterns, etc.)

### BR-004: Leaderboard Visibility

- **Real-Time Updates**: Leaderboard reflects latest point calculations
- **Competition Status**:
    - **Upcoming**: No leaderboard (no results yet)
    - **Live**: Leaderboard updates as results come in
    - **Finished**: Final leaderboard shown
- **Historical Data**: Past leaderboards remain accessible

### BR-005: Performance Requirements

- **Fast Loading**: Leaderboard must load within 2 seconds
- **Pagination**: Show 50 users per page for large user bases
- **Caching**: Cache rankings for 1 minute to reduce database load
- **Sorting**: Rankings pre-calculated and stored for performance

### BR-006: Visual Indicators

- **Top 3 Highlighting**: Special badges/icons for 1st, 2nd, 3rd place
- **Current User**: Highlighted with different background color
- **Rank Changes**: Show rank change from previous competition (↑ ↓ →)
- **Point Trends**: Show point gain/loss trend over time

## Non-Functional Requirements

### Performance

- Initial leaderboard load must complete within 2 seconds
- Filtered leaderboard must update within 1 second
- Support up to 10,000 users without performance degradation
- Use database indexes for efficient ranking queries
- Cache frequently accessed rankings

### Usability

- Current user's ranking should be immediately visible (sticky/highlighted)
- Clear visual distinction between overall and filtered rankings
- Intuitive filter controls with clear labels
- Responsive design for mobile viewing
- Smooth scrolling for large leaderboards

### Scalability

- Pagination for leaderboards with >100 users
- Async loading for large datasets
- Efficient SQL queries with proper indexes
- Consider materialized views for pre-calculated rankings
- Background jobs to update ranking cache

### Accessibility

- Screen reader support for rankings
- Keyboard navigation through leaderboard
- Clear contrast for highlighted rows
- Alt text for rank badges and icons
- ARIA labels for interactive elements

### Real-Time Updates

- WebSocket support for live competitions (future enhancement)
- Auto-refresh option during live competitions
- Visual notification when rankings change
- Timestamp showing last update time

## Data Model Impact

### Tables Read

- `app_user` - User information (username, id)
- `prediction` - Points earned by each user
- `competition` - Competition details for filtering
- `competition_entry` - Link to gymnast and apparatus for filtering
- `gymnast` - Gender information for filtering
- `apparatus` - Apparatus details for filtering

### Queries Required

#### Overall Leaderboard Query

```sql
SELECT u.id,
       u.username,
       COALESCE(SUM(p.points), 0)               AS total_points,
       COUNT(p.id)                              AS total_predictions,
       COUNT(CASE WHEN p.points = 3 THEN 1 END) AS exact_predictions,
       ROUND(AVG(p.points), 2)                  AS avg_points
FROM app_user u
         LEFT JOIN prediction p ON u.id = p.user_id
WHERE p.created_at < (SELECT c.start_time - INTERVAL '30 minutes'
FROM competition c
    JOIN competition_entry ce
ON c.id = ce.competition_id
WHERE ce.id = p.entry_id)
GROUP BY u.id, u.username
ORDER BY total_points DESC,
    exact_predictions DESC,
    total_predictions DESC,
    u.created_at ASC;
```

#### Competition-Specific Leaderboard

```sql
SELECT u.id,
       u.username,
       COALESCE(SUM(p.points), 0) AS competition_points,
       COUNT(p.id)                AS predictions_in_competition
FROM app_user u
         LEFT JOIN prediction p ON u.id = p.user_id
         JOIN competition_entry ce ON p.entry_id = ce.id
WHERE ce.competition_id = ?
  AND p.created_at < (SELECT start_time - INTERVAL '30 minutes'
FROM competition
WHERE id = ?)
GROUP BY u.id, u.username
ORDER BY competition_points DESC;
```

#### Apparatus-Specific Leaderboard

```sql
SELECT u.id,
       u.username,
       a.name                     AS apparatus_name,
       COALESCE(SUM(p.points), 0) AS apparatus_points,
       COUNT(p.id)                AS apparatus_predictions
FROM app_user u
         LEFT JOIN prediction p ON u.id = p.user_id
         JOIN competition_entry ce ON p.entry_id = ce.id
         JOIN apparatus a ON ce.apparatus_id = a.id
WHERE a.id = ?
  AND p.created_at < (SELECT c.start_time - INTERVAL '30 minutes'
FROM competition c
WHERE c.id = ce.competition_id)
GROUP BY u.id, u.username, a.name
ORDER BY apparatus_points DESC;
```

### Indexes Required

```sql
-- Index for efficient user points aggregation
CREATE INDEX idx_prediction_user_points ON prediction (user_id, points);

-- Index for competition filtering
CREATE INDEX idx_competition_entry_competition ON competition_entry (competition_id);

-- Index for apparatus filtering
CREATE INDEX idx_competition_entry_apparatus ON competition_entry (apparatus_id);

-- Index for deadline filtering
CREATE INDEX idx_prediction_created_at ON prediction (created_at);

-- Composite index for leaderboard queries
CREATE INDEX idx_prediction_user_entry ON prediction (user_id, entry_id, points);
```

## UI Components (Vaadin Flow)

### View Structure

```
LeaderboardView
├── Header: "Leaderboard"
├── HorizontalLayout (Filters)
│   ├── ComboBox: "Competition Filter" (All Competitions / Specific)
│   ├── ComboBox: "Apparatus Filter" (All / Specific)
│   ├── ComboBox: "Gender Filter" (All / Men / Women)
│   ├── DateRangePicker: "Date Range" (optional)
│   └── Button: "Reset Filters"
├── HorizontalLayout (Actions)
│   ├── Button: "Refresh" (icon)
│   ├── Button: "Export CSV" (icon)
│   └── Checkbox: "Show Top 10 Only"
├── Grid<LeaderboardEntry>
│   ├── Column: Rank (with medals for top 3)
│   ├── Column: Username (clickable)
│   ├── Column: Total Points
│   ├── Column: Predictions Made
│   ├── Column: Avg Points
│   ├── Column: Exact Predictions (3pts)
│   └── Column: Trend (↑ ↓ →)
├── HorizontalLayout (Pagination)
│   └── Pagination controls (if needed)
└── Footer: "Last updated: [timestamp]"
```

### Components Used

- `VerticalLayout` - Main container
- `HorizontalLayout` - Filter and action bars
- `Grid<LeaderboardEntry>` - Main leaderboard display
- `ComboBox` - Filter selections
- `DatePicker` or `DateRangePicker` - Date filtering
- `Button` - Refresh, export, and action buttons
- `Checkbox` - Toggle options
- `Badge` or `Icon` - Rank badges (1st, 2nd, 3rd)
- `ComponentRenderer` - Custom rendering for rank and username columns
- `Notification` - Success/error messages

### DTO Classes

```java
public record LeaderboardEntry(
        Long userId,
        String username,
        int rank,
        int totalPoints,
        int totalPredictions,
        int exactPredictions,
        double avgPoints,
        RankTrend trend,
        boolean isCurrentUser
) {
}

public enum RankTrend {
    UP,      // ↑ Rank improved
    DOWN,    // ↓ Rank decreased
    STABLE,  // → Rank unchanged
    NEW      // New to leaderboard
}
```

## Service Layer Design

### LeaderboardService

**Responsibilities**:

- Calculate user rankings from prediction points
- Apply filters to leaderboard data
- Aggregate statistics for display
- Cache frequently accessed rankings
- Export leaderboard data

**Key Methods**:

```java
/**
 * Get overall leaderboard across all competitions.
 * @return List of leaderboard entries sorted by rank
 */
public List<LeaderboardEntry> getOverallLeaderboard();

/**
 * Get leaderboard for a specific competition.
 * @param competitionId The competition ID
 * @return Competition-specific rankings
 */
public List<LeaderboardEntry> getCompetitionLeaderboard(Long competitionId);

/**
 * Get leaderboard for a specific apparatus.
 * @param apparatusId The apparatus ID
 * @return Apparatus-specific rankings
 */
public List<LeaderboardEntry> getApparatusLeaderboard(Long apparatusId);

/**
 * Get leaderboard with filters applied.
 * @param filter Filter criteria (competition, apparatus, gender, date range)
 * @return Filtered leaderboard entries
 */
public List<LeaderboardEntry> getFilteredLeaderboard(LeaderboardFilter filter);

/**
 * Get user's current rank in overall leaderboard.
 * @param userId The user ID
 * @return User's rank (position)
 */
public int getUserRank(Long userId);

/**
 * Get user's detailed statistics.
 * @param userId The user ID
 * @return Detailed breakdown of user's performance
 */
public UserStatistics getUserStatistics(Long userId);

/**
 * Export leaderboard to CSV format.
 * @param entries Leaderboard entries to export
 * @return CSV content as byte array
 */
public byte[] exportLeaderboardToCsv(List<LeaderboardEntry> entries);

/**
 * Calculate rank trend by comparing with previous competition.
 * @param userId The user ID
 * @param currentRank Current rank position
 * @return Rank trend indicator
 */
public RankTrend calculateRankTrend(Long userId, int currentRank);
```

## Integration Points

### UC-014: Calculate Points

- **Dependency**: Leaderboard requires calculated points from predictions
- **Trigger**: Leaderboard updates automatically when UC-014 completes
- **Data Flow**: Points → Aggregation → Rankings → Display

### UC-016: View Competition Details

- **Link**: Clicking on username navigates to detailed view
- **Context**: Leaderboard provides context for detailed statistics
- **Navigation**: Two-way navigation between views

### UC-008: Make Predictions

- **Motivation**: Leaderboard motivates users to make predictions
- **Link**: "Start predicting" link when user has no predictions
- **Flow**: View leaderboard → Make predictions → Return to leaderboard

### UC-013: View Live Results

- **Real-Time**: During live competitions, leaderboard updates as results come in
- **Synchronization**: Both views should show consistent data
- **Integration**: Live results page can show mini leaderboard

## Test Scenarios

### Basic Display Tests

1. **Empty Leaderboard**: No predictions yet → Show empty state with guidance
2. **Single User**: One user with predictions → Show rank 1
3. **Multiple Users**: 10 users → Show correct rankings 1-10
4. **Tied Scores**: Users with same points → Apply tiebreaker rules
5. **Current User Highlighting**: User views leaderboard → Their row highlighted

### Filtering Tests

6. **Competition Filter**: Select specific competition → Show competition rankings
7. **Apparatus Filter**: Select Floor → Show only Floor predictions
8. **Gender Filter**: Select Women → Show only women's apparatus predictions
9. **Date Range Filter**: Select range → Show competitions in range
10. **Combined Filters**: Competition + Apparatus → Show intersection
11. **Reset Filters**: Click reset → Return to overall leaderboard

### Ranking Calculation Tests

12. **Correct Ordering**: Points descending → Higher points ranked higher
13. **Tiebreaker - Exact Predictions**: Same points → More exact predictions wins
14. **Tiebreaker - Total Predictions**: Same exact → More predictions wins
15. **Tiebreaker - Registration Date**: Same everything → Earlier registration wins
16. **Deadline Filtering**: Late predictions → Not counted in points

### Performance Tests

17. **Large Dataset**: 1000 users → Load within 2 seconds
18. **Concurrent Access**: 50 users viewing → No performance degradation
19. **Complex Filters**: Multiple filters applied → Results within 1 second
20. **Pagination**: Large leaderboard → Smooth pagination

### Export Tests

21. **Export CSV**: Full leaderboard → Valid CSV file generated
22. **Export Filtered**: Competition filter → Export only filtered data
23. **Export Top 10**: Top 10 filter → Export only top 10

### Real-Time Update Tests

24. **Auto Refresh**: New points calculated → Leaderboard updates
25. **Manual Refresh**: Click refresh → Latest data loaded
26. **Timestamp Display**: After refresh → Updated timestamp shown
27. **Live Competition**: Points update → Rankings change in real-time

### Edge Cases

28. **User with No Predictions**: Never predicted → Rank N/A or bottom
29. **User with All Zero Points**: Predictions all >10% off → Shown at bottom
30. **Deleted User**: User deleted → Not shown in leaderboard
31. **Invalid Filter Combination**: No data matches → Show empty state
32. **Very Long Username**: Display properly → Truncate with tooltip

### User Interaction Tests

33. **Click Username**: Navigate to details → UC-016 opens
34. **Click Own Ranking**: Show details modal → Breakdown displayed
35. **Scroll Large List**: 500+ users → Smooth scrolling
36. **Mobile Responsive**: View on mobile → Proper layout

## Future Enhancements

1. **Live Updates**
    - WebSocket integration for real-time ranking changes
    - Notification when user's rank changes
    - Live ticker of recent point gains

2. **Advanced Statistics**
    - Historical rank progression graph
    - Prediction accuracy by apparatus
    - Comparison with average user
    - Percentile ranking

3. **Social Features**
    - Friend leaderboard (compare with friends only)
    - Team leaderboards for group competitions
    - Share leaderboard position on social media

4. **Achievements and Badges**
    - Award badges for milestones (100 predictions, top 10, etc.)
    - Display badges on leaderboard
    - Achievement points separate from prediction points

5. **Predictions for Leaderboard**
    - Predict final leaderboard standings
    - Meta-game for predicting who will win
    - Bonus points for accurate leaderboard predictions

6. **Customizable Views**
    - User can save favorite filters
    - Customize visible columns
    - Personal leaderboard layout preferences

7. **Mobile App Push Notifications**
    - Notify when user moves up in rankings
    - Daily/weekly ranking summary
    - Competition leaderboard finalized notification

8. **Historical Leaderboards**
    - View past competition leaderboards
    - Year-over-year comparison
    - All-time leaderboard across seasons

9. **Export and Reporting**
    - PDF export with charts and graphs
    - Excel export with formatted data
    - Automatic email reports for top users

10. **Predictive Analytics**
    - Show projected final ranking
    - Estimate points needed to reach specific rank
    - Difficulty rating for upcoming predictions

## Related Use Cases

- **UC-001**: Register Account - Users must register to appear on leaderboard
- **UC-002**: Login - Users must log in to view leaderboard
- **UC-003**: View Competitions - Filter leaderboard by competitions
- **UC-008**: Make Predictions - Predictions contribute to leaderboard ranking
- **UC-014**: Calculate Points - Provides point data for rankings
- **UC-016**: View Competition Details - Detailed view of user statistics
- **UC-017**: View History - Historical leaderboard data
- **UC-018**: View Statistics by Apparatus - Related apparatus-specific views

## Implementation Notes

### Ranking Algorithm

```java
public class LeaderboardService {

    public List<LeaderboardEntry> calculateRankings(List<UserPoints> userPoints) {
        // Sort by: total points (desc), exact predictions (desc),
        // total predictions (desc), registration date (asc)
        userPoints.sort(Comparator
                .comparing(UserPoints::getTotalPoints).reversed()
                .thenComparing(UserPoints::getExactPredictions).reversed()
                .thenComparing(UserPoints::getTotalPredictions).reversed()
                .thenComparing(UserPoints::getRegistrationDate)
        );

        // Assign ranks handling ties
        int rank = 1;
        for (int i = 0; i < userPoints.size(); i++) {
            if (i > 0 && !hasSameScore(userPoints.get(i), userPoints.get(i - 1))) {
                rank = i + 1;
            }
            userPoints.get(i).setRank(rank);
        }

        return userPoints;
    }

    private boolean hasSameScore(UserPoints u1, UserPoints u2) {
        return u1.getTotalPoints() == u2.getTotalPoints()
                && u1.getExactPredictions() == u2.getExactPredictions()
                && u1.getTotalPredictions() == u2.getTotalPredictions();
    }
}
```

### Caching Strategy

```java

@Service
public class LeaderboardService {

    @Cacheable(value = "leaderboard", key = "'overall'",
            unless = "#result == null || #result.isEmpty()")
    public List<LeaderboardEntry> getOverallLeaderboard() {
        // Cache for 1 minute to reduce DB load
        return calculateOverallRankings();
    }

    @CacheEvict(value = "leaderboard", allEntries = true)
    public void clearLeaderboardCache() {
        // Evict cache when new points are calculated
    }
}
```

### Performance Optimization

- Use database views for pre-calculated rankings
- Materialize leaderboard data for faster queries
- Use Redis for caching frequently accessed rankings
- Implement pagination for large user bases
- Consider read replicas for heavy read load
- Batch update rank calculations during off-peak hours
