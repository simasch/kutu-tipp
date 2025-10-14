# UC-009: View Own Predictions

## Brief Description

This use case allows an authenticated user (Tipper) to view all competitions for which they have made predictions, along with detailed prediction status information. Users can see completion percentages, prediction counts, editability status, and navigate to edit predictions if the deadline has not passed.

## Actors

- **Primary Actor**: Tipper (User) - authenticated user who wants to view their predictions
- **Secondary Actor**: System - retrieves and displays prediction data from the database

## Preconditions

- User is authenticated (UC-002)
- User has made at least one prediction (UC-008) or may view an empty state

## Postconditions

- **Success**: User sees a comprehensive overview of all their predictions
- **Success**: User can navigate to edit predictions for editable competitions
- **Success**: User understands which competitions are still editable vs. locked
- **No side effects**: This is a read-only view - no data is modified

## Main Success Scenario (Basic Flow)

1. User navigates to "My Predictions" page
2. System retrieves current authenticated user information
3. System queries all competitions where user has made predictions
4. For each competition, system calculates:
   - Total competition entries
   - Number of predictions made by user
   - Completion percentage
   - Editability status (based on deadline)
5. System displays a grid with the following columns:
   - Competition name
   - Competition date/time
   - Status badge (upcoming/live/finished)
   - Prediction count (e.g., "15 / 40")
   - Completion percentage with visual indicator
   - Editable indicator (icon showing locked or editable)
   - Action buttons (Edit or View)
6. System sorts competitions by date (most recent first)
7. User can click on any competition to view/edit predictions
8. System navigates to prediction view (UC-010) with pre-selected competition

## Alternative Flows

### 3a. No Predictions Yet

- At step 3, if user has no predictions:
  1. System displays empty state with informative message
  2. System shows icon and text: "No predictions yet"
  3. System provides description: "You haven't made any predictions yet. Visit the Make Predictions page to get started!"
  4. System displays "Make Predictions" button
  5. User can click button to navigate to UC-008
  6. Use case ends

### 7a. View Locked Competition

- At step 7, if user clicks on competition with passed deadline:
  1. System navigates to prediction view in read-only mode
  2. All input fields are disabled
  3. User can view their predictions but not modify them
  4. Use case continues to UC-010 (view-only mode)

### 7b. Edit Editable Competition

- At step 7, if user clicks on competition before deadline:
  1. System navigates to prediction view in edit mode
  2. All existing predictions are pre-filled
  3. User can modify predictions
  4. Use case continues to UC-010 (edit mode)

### 7c. Filter or Search Competitions

- At any point, user may want to filter competitions:
  1. User applies filter (e.g., only "upcoming" competitions)
  2. System updates grid to show filtered results
  3. Use case continues from step 6

### 7d. Sort by Different Criteria

- At step 7, user clicks column header to sort:
  1. System re-sorts grid by selected column
  2. Sort indicator shows current sort direction
  3. Use case continues from step 6

## Exception Flows

### E1: Database Connection Error

- If database is unavailable at step 3:
  1. System displays error: "Error loading your predictions. Please try again later."
  2. System logs the error for administrators
  3. System shows retry button
  4. Use case ends

### E2: Session Expired

- If user session expires during view:
  1. System detects expired authentication
  2. System redirects to login page (UC-002)
  3. After successful login, system returns to My Predictions page
  4. Use case resumes from step 1

### E3: System Error

- If any unexpected error occurs:
  1. System displays generic error message
  2. System logs the error with details
  3. System shows navigation to home or predictions page
  4. Use case ends

## Business Rules

### BR-009-001: Prediction Summary Calculation

- **Total Entries**: Count of all competition entries for the competition
- **Predicted Entries**: Count of user's predictions for that competition
- **Completion Percentage**: `(Predicted Entries / Total Entries) * 100`
- **Complete Status**: `Completion Percentage == 100%`

### BR-009-002: Editability Determination

- Competition is **editable** if:
  - Competition status is "upcoming" AND
  - Current time is at least 30 minutes before competition start time
  - Formula: `current_time + 30 minutes <= competition.start_datetime`
- Competition is **locked** if:
  - Competition status is "live" or "finished" OR
  - Deadline has passed (less than 30 minutes before start)

### BR-009-003: Status Badge Display

- **Upcoming** (Green badge): Competition scheduled for future, deadline not passed
- **Live** (Gray/Contrast badge): Competition currently in progress
- **Finished** (Red badge): Competition completed

### BR-009-004: Competition Inclusion

- Only show competitions where user has made at least one prediction
- Do not show competitions where user has zero predictions
- Show all historical predictions (including finished competitions)

### BR-009-005: Data Visibility

- Users can only see their own predictions
- Cannot view other users' prediction counts or percentages
- Cannot access other users' prediction details

### BR-009-006: Sorting and Display Order

- Default sort: Competition date (most recent first)
- Allow sorting by: Name, Date, Status, Completion %
- Maintain sort preference within session

## Non-Functional Requirements

### Performance

- Prediction summary must load within 2 seconds
- Calculation of completion percentages should be efficient
- Support viewing up to 100 competitions without performance degradation
- Lazy loading for large datasets (pagination if needed)

### Usability

- Clear visual distinction between editable and locked competitions
- Progress indicators (completion percentage) should be easy to understand
- Action buttons clearly labeled (Edit vs. View)
- Responsive design for mobile and desktop
- Tooltips for icons and abbreviations
- Empty state should be encouraging and actionable

### Accessibility

- All information must be available to screen readers
- Status badges must not rely solely on color
- Icons must have text alternatives
- Keyboard navigation support for grid and action buttons
- Proper ARIA labels for completion percentages
- Focus indicators for keyboard navigation

### Data Accuracy

- Real-time data - no caching of prediction counts
- Accurate deadline calculations based on server time
- Consistent editability status across all views
- Proper timezone handling for competition dates

## UI Components (Vaadin Flow)

### View Structure

```
MyPredictionsView
├── Header: "My Predictions"
├── Description
│   └── Paragraph: Instructions and context
├── Grid<UserCompetitionSummary>
│   ├── Column: Competition Name
│   ├── Column: Date (formatted)
│   ├── Column: Status (badge component)
│   ├── Column: Predictions (e.g., "15 / 40")
│   ├── Column: Completion (% + checkmark if complete)
│   ├── Column: Editable Indicator (icon)
│   └── Column: Actions (Edit or View button)
└── EmptyState (if no predictions)
    ├── Icon: Info circle (large)
    ├── Heading: "No predictions yet"
    ├── Description: Encouragement text
    └── Button: "Make Predictions" -> navigate to UC-008
```

### Components Used

- `VerticalLayout` - Main container
- `H1` - Page title
- `Paragraph` - Description text
- `Grid<UserCompetitionSummaryDto>` - Main data display
- `Span` - Status badges
- `Icon` - Visual indicators (edit, lock, check, info)
- `HorizontalLayout` - Completion percentage display
- `Button` - Navigation actions
- `Badge` - Status display (using Span with theme)

### Grid Configuration

```java
grid.addColumn(UserCompetitionSummaryDto::competitionName)
    .setHeader("Competition")
    .setAutoWidth(true)
    .setFlexGrow(1);

grid.addColumn(summary -> summary.competitionDate().format(DATE_TIME_FORMATTER))
    .setHeader("Date")
    .setWidth("150px")
    .setFlexGrow(0);

grid.addColumn(new ComponentRenderer<>(summary -> {
    var statusBadge = new Span(summary.status().getLiteral());
    statusBadge.getElement().getThemeList().add("badge");
    // Add color theme based on status
    return statusBadge;
})).setHeader("Status").setWidth("120px").setFlexGrow(0);

grid.addColumn(summary ->
    String.format("%d / %d", summary.predictedEntries(), summary.totalEntries())
).setHeader("Predictions").setWidth("120px").setFlexGrow(0);

grid.addColumn(new ComponentRenderer<>(summary -> {
    var layout = new HorizontalLayout();
    layout.add(new Span(summary.getCompletionPercentage() + "%"));
    if (summary.isComplete()) {
        layout.add(new Icon(VaadinIcon.CHECK_CIRCLE));
    }
    return layout;
})).setHeader("Completion").setWidth("150px").setFlexGrow(0);

grid.addColumn(new ComponentRenderer<>(summary -> {
    return summary.isEditable()
        ? new Icon(VaadinIcon.EDIT)  // Green edit icon
        : new Icon(VaadinIcon.LOCK);  // Red lock icon
})).setHeader("").setWidth("60px").setFlexGrow(0);

grid.addColumn(new ComponentRenderer<>(summary -> {
    var button = summary.isEditable()
        ? new Button("Edit", new Icon(VaadinIcon.EDIT))
        : new Button("View", new Icon(VaadinIcon.EYE));
    button.addClickListener(e -> navigateToPredictionView(summary.competitionId()));
    return button;
})).setHeader("Actions").setWidth("120px").setFlexGrow(0);
```

### CSS Styling

```css
/* Status badges */
.badge {
    padding: 0.25em 0.5em;
    border-radius: var(--lumo-border-radius-s);
    font-size: var(--lumo-font-size-s);
    font-weight: 500;
}

.badge.success {
    color: var(--lumo-success-text-color);
    background-color: var(--lumo-success-color-10pct);
}

.badge.contrast {
    color: var(--lumo-contrast-text-color);
    background-color: var(--lumo-contrast-10pct);
}

.badge.error {
    color: var(--lumo-error-text-color);
    background-color: var(--lumo-error-color-10pct);
}

/* Empty state */
.empty-state {
    text-align: center;
    padding: 3em;
}

.empty-state vaadin-icon {
    color: var(--lumo-secondary-text-color);
}
```

## Data Model Impact

### Tables Queried

- `competition` - Competition details
- `competition_entry` - Count total entries per competition
- `prediction` - User's predictions
- `app_user` - Current user information

### DTO Structure

```java
public record UserCompetitionSummaryDto(
    Long competitionId,
    String competitionName,
    OffsetDateTime competitionDate,
    CompetitionStatus status,
    Integer totalEntries,        // Total competition entries
    Integer predictedEntries,    // User's prediction count
    Boolean editable            // Based on deadline
) {
    public Integer getCompletionPercentage() {
        if (totalEntries == 0) return 0;
        return (predictedEntries * 100) / totalEntries;
    }

    public Boolean isComplete() {
        return totalEntries.equals(predictedEntries);
    }

    public Boolean isEditable() {
        return editable;
    }
}
```

### Main Query

```sql
-- Get competitions with user's prediction summary
SELECT
    c.id as competition_id,
    c.name as competition_name,
    c.start_datetime as competition_date,
    c.status,
    COUNT(DISTINCT ce.id) as total_entries,
    COUNT(DISTINCT p.id) as predicted_entries,
    CASE
        WHEN c.status = 'upcoming'
         AND c.start_datetime > NOW() + INTERVAL '30 minutes'
        THEN true
        ELSE false
    END as editable
FROM competition c
JOIN competition_entry ce ON ce.competition_id = c.id
LEFT JOIN prediction p ON p.competition_entry_id = ce.id
                      AND p.user_id = ?
WHERE EXISTS (
    SELECT 1 FROM prediction p2
    JOIN competition_entry ce2 ON p2.competition_entry_id = ce2.id
    WHERE ce2.competition_id = c.id AND p2.user_id = ?
)
GROUP BY c.id, c.name, c.start_datetime, c.status
ORDER BY c.start_datetime DESC;
```

### Alternative Query (using jOOQ)

```java
var competitions = dsl
    .select(
        COMPETITION.ID,
        COMPETITION.NAME,
        COMPETITION.START_DATETIME,
        COMPETITION.STATUS,
        DSL.countDistinct(COMPETITION_ENTRY.ID).as("total_entries"),
        DSL.countDistinct(PREDICTION.ID).as("predicted_entries"),
        DSL.when(
            COMPETITION.STATUS.eq(CompetitionStatus.upcoming)
                .and(COMPETITION.START_DATETIME.gt(
                    DSL.currentOffsetDateTime().plus(Duration.ofMinutes(30))
                )),
            true
        ).otherwise(false).as("editable")
    )
    .from(COMPETITION)
    .join(COMPETITION_ENTRY).on(COMPETITION_ENTRY.COMPETITION_ID.eq(COMPETITION.ID))
    .leftJoin(PREDICTION).on(
        PREDICTION.COMPETITION_ENTRY_ID.eq(COMPETITION_ENTRY.ID)
            .and(PREDICTION.USER_ID.eq(currentUserId))
    )
    .whereExists(
        DSL.selectOne()
            .from(PREDICTION.as("p2"))
            .join(COMPETITION_ENTRY.as("ce2")).on(...)
            .where(...)
    )
    .groupBy(COMPETITION.ID, COMPETITION.NAME, COMPETITION.START_DATETIME, COMPETITION.STATUS)
    .orderBy(COMPETITION.START_DATETIME.desc())
    .fetch();
```

## Test Scenarios

### Success Cases

1. **Happy Path**: User has predictions for 3 competitions → All displayed with correct counts
2. **Mixed Status**: Competitions with different statuses (upcoming/live/finished) → All displayed with appropriate badges
3. **Various Completion Levels**: Some 100% complete, some partial → Percentages calculated correctly
4. **Editable and Locked**: Mix of editable and locked competitions → Icons and buttons show correct state
5. **Click to Edit**: User clicks "Edit" on editable competition → Navigates to UC-010 with competition pre-selected
6. **Click to View**: User clicks "View" on locked competition → Navigates to read-only prediction view

### Empty State Cases

7. **New User**: User with zero predictions → Empty state displayed with "Make Predictions" button
8. **After Deletion**: User deletes all predictions → Empty state appears
9. **Empty State Navigation**: User clicks "Make Predictions" from empty state → Navigates to UC-008

### Data Accuracy Cases

10. **Completion Percentage**: User with 10/40 predictions → Shows "25%" correctly
11. **Full Completion**: User predicted all entries → Shows "100%" with checkmark
12. **Zero Predictions Edge Case**: Should not appear (BR-009-004), but if it does → Shows "0 / 40" and "0%"
13. **Large Numbers**: Competition with 500 entries → Counts display correctly

### Editability Cases

14. **Upcoming, Before Deadline**: 2 hours before start → Shows editable (green edit icon)
15. **Upcoming, After Deadline**: 15 minutes before start → Shows locked (red lock icon)
16. **Live Competition**: Status "live" → Shows locked with "View" button
17. **Finished Competition**: Status "finished" → Shows locked with "View" button
18. **Exactly at Deadline**: 30 minutes before start → Edge case, should be locked

### Sorting and Filtering Cases

19. **Default Sort**: By date descending → Most recent competition first
20. **Sort by Name**: Alphabetically → Correct order
21. **Sort by Status**: Group by status → All upcoming together, etc.
22. **Sort by Completion**: By percentage → 100% first or last depending on direction

### Navigation Cases

23. **Navigate to Edit**: From "Edit" button → Prediction view with correct competition selected
24. **Navigate from Menu**: From main menu → "My Predictions" page loads
25. **Direct URL**: User visits `/my-predictions` directly → Page loads correctly
26. **Back Navigation**: User goes to edit, then back → Returns to My Predictions with same state

### Permission and Security Cases

27. **Own Predictions Only**: User sees only their own predictions → No other users' data visible
28. **Authentication Required**: Unauthenticated user tries to access → Redirected to login
29. **Different Users**: Two users have different predictions → Each sees only their own

### Edge Cases

30. **Many Competitions**: User has 50 competitions with predictions → All load and display correctly
31. **Same Competition Date**: Multiple competitions on same day → All displayed, secondary sort by name
32. **Very Long Competition Name**: Name truncation or wrapping → Readable display
33. **Zero Total Entries**: Edge case (shouldn't happen) → Handled gracefully

### Performance Cases

34. **Large Dataset**: User with 100 competitions → Loads within 2 seconds
35. **Complex Calculations**: Many competitions with many entries → Percentages calculated quickly
36. **Refresh**: User refreshes page → Data reloads correctly

## Future Enhancements

1. **Enhanced Filtering**
   - Filter by status (upcoming/live/finished)
   - Filter by completion (complete/partial/empty)
   - Filter by editability (editable/locked)
   - Date range filter
   - Search by competition name

2. **Statistics Dashboard**
   - Overall prediction count across all competitions
   - Average completion percentage
   - Total points earned (from finished competitions)
   - Ranking position summary
   - Win/loss record

3. **Visual Enhancements**
   - Progress bars for completion percentage
   - Charts showing prediction trends over time
   - Calendar view of competitions
   - Timeline view with upcoming deadlines
   - Color-coded cards instead of grid

4. **Quick Actions**
   - "Complete Predictions" quick link for partial competitions
   - "Review Before Deadline" for competitions closing soon
   - Bulk actions (e.g., "Delete all predictions for this competition")
   - Export predictions to PDF or CSV

5. **Comparison Features**
   - Compare predictions across competitions
   - Show how predictions changed over time (if edit history available)
   - Compare with average community predictions
   - Highlight best and worst performing predictions

6. **Notifications Integration**
   - Badge showing number of incomplete predictions
   - Highlight competitions with approaching deadlines
   - Alert for predictions that haven't been reviewed in a while
   - Push notifications for deadline reminders

7. **Mobile Optimization**
   - Card-based layout for mobile devices
   - Swipe gestures for actions
   - Simplified mobile view with essential information
   - Progressive Web App (PWA) capabilities

8. **Detailed Prediction View**
   - Expand row to see individual predictions inline
   - Quick preview of predictions without navigating away
   - Hover tooltips showing prediction details
   - Modal dialog with full prediction breakdown

9. **Historical Analysis**
   - Archive old predictions
   - Show historical accuracy trends
   - Best/worst performing apparatus
   - Learning insights and recommendations

10. **Social Features**
    - Share prediction summary with friends
    - Challenge friends to complete more predictions
    - Leaderboard integration (show rank directly in view)
    - Comments or notes on predictions

## Related Use Cases

- **UC-002**: Login - Required before viewing predictions (include relationship)
- **UC-008**: Make Predictions - Users navigate here to create first predictions
- **UC-010**: Edit Predictions - Users navigate here to modify predictions (primary workflow)
- **UC-014**: Calculate Points - Points shown in this view for completed competitions
- **UC-015**: View Leaderboard - May navigate from leaderboard to see own predictions
- **UC-016**: View Competition Details - May want to see full competition context

## Implementation Notes

### Implementation Status

- ✅ **Implemented** in `MyPredictionsView.java`
- Route: `/my-predictions`
- Page Title: "My Predictions - Kutu-Tipp"
- Security: `@PermitAll` (requires authentication)

### Key Classes

- `MyPredictionsView.java` - Main view component
- `UserCompetitionSummaryDto.java` - Data transfer object
- `PredictionRepository.java` - Data access with `getCompetitionsWithPredictions()`
- `PredictionView.java` - Target navigation for editing

### Repository Method

```java
public List<UserCompetitionSummaryDto> getCompetitionsWithPredictions(Long userId);
```

This method returns all competitions where the user has made at least one prediction, with calculated summary information.

### Navigation Pattern

```java
private void navigateToPredictionView(Long competitionId) {
    var queryParams = QueryParameters.simple(
        Map.of("competitionId", competitionId.toString())
    );
    UI.getCurrent().navigate(PredictionView.class, queryParams);
}
```

The competition ID is passed as a query parameter, and `PredictionView` implements `BeforeEnterObserver` to handle the parameter and pre-select the competition.
