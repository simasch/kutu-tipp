# UC-008: Make Predictions

## Brief Description

This use case allows an authenticated user (Tipper) to make predictions for gymnast scores on specific apparatus for an upcoming competition. Users can predict individual scores and submit them before the prediction deadline (30 minutes before competition start).

## Actors

- **Primary Actor**: Tipper (User) - authenticated user who wants to make predictions
- **Secondary Actor**: System - validates predictions and stores them in the database

## Preconditions

- User is authenticated (UC-002)
- At least one competition exists with status "upcoming"
- Competition has configured competition entries (gymnast/apparatus combinations)
- Current time is more than 30 minutes before competition start time
- User has not yet submitted predictions for this competition (or see UC-010 for editing)

## Postconditions

- **Success**: User's predictions are saved in the database
- **Success**: User receives confirmation of successful submission
- **Success**: Predictions are locked for editing once submitted (unless edited via UC-010)
- **Failure**: No predictions are saved

## Main Success Scenario (Basic Flow)

1. User navigates to the predictions page
2. System displays list of available competitions (status: upcoming, deadline not passed)
3. User selects a competition
4. System retrieves all competition entries for the selected competition
5. System displays a grid with the following columns:
   - Gymnast name
   - Team name
   - Apparatus name
   - Gender
   - Predicted score (input field)
6. User enters predicted scores for desired gymnast/apparatus combinations
7. User clicks "Submit Predictions" button
8. System validates all entered predictions (see Business Rules)
9. System checks that prediction deadline has not passed
10. System saves all valid predictions to the database
11. System calculates and displays summary (number of predictions submitted)
12. System displays success notification
13. System updates view to show submitted predictions (read-only or editable based on deadline)

## Alternative Flows

### 2a. No Competitions Available

- At step 2, if no competitions are available for predictions:
  1. System displays message: "No competitions available for predictions at this time."
  2. System shows link to view past competitions or leaderboard
  3. Use case ends

### 3a. User Cancels Selection

- At step 3, user clicks "Cancel" or navigates away
- System discards any unsaved predictions
- Use case ends

### 6a. User Skips Some Entries

- At step 6, user does not provide predictions for all competition entries:
  1. This is acceptable - partial predictions are allowed
  2. Continue to step 7
  3. Only provided predictions are saved

### 6b. User Wants to Save Draft

- At step 6, user clicks "Save Draft" button:
  1. System validates entered scores
  2. System saves predictions with draft status
  3. System displays confirmation
  4. User can continue editing or leave page
  5. Use case continues or ends

### 9a. Validation Errors

- At step 9, if any prediction is invalid:
  1. System displays specific error messages next to invalid fields
  2. System highlights invalid entries in the grid
  3. System keeps all valid entries filled in
  4. User returns to step 6 to correct errors

### 10a. Deadline Passed During Submission

- At step 10, if deadline has passed since page was loaded:
  1. System displays error: "Prediction deadline has passed. Predictions cannot be submitted."
  2. System shows current time and competition start time
  3. No predictions are saved
  4. Use case ends

## Exception Flows

### E1: Database Connection Error

- If database is unavailable at step 10:
  1. System displays error: "Unable to save predictions. Please try again later."
  2. System logs the error for administrators
  3. System attempts to preserve entered data in browser session
  4. Use case ends

### E2: Competition State Changed

- If competition status changed to "live" or "finished" during user interaction:
  1. System detects state change
  2. System displays warning: "This competition has started. Predictions can no longer be submitted."
  3. System disables all input fields
  4. Use case ends

### E3: System Error

- If any unexpected error occurs:
  1. System displays generic error: "An error occurred. Please try again."
  2. System logs the error with details
  3. System attempts to preserve entered data
  4. Use case ends

## Business Rules

### BR-008-001: Prediction Deadline

- Predictions can only be submitted if current time is at least 30 minutes before competition start time
- Formula: `current_time + 30 minutes <= competition.start_datetime`
- Once deadline passes, all prediction inputs are disabled

### BR-008-002: Score Range Validation

- Predicted scores must be numeric values
- **Valid range**: 0.000 to 20.000 (typical gymnastics scoring range)
- **Precision**: Up to 3 decimal places (e.g., 14.500)
- **Format**: Must be a valid decimal number

### BR-008-003: Competition Entry Validation

- Each prediction must reference a valid competition entry
- Competition entry must exist and belong to the selected competition
- No duplicate predictions for the same competition entry by same user

### BR-008-004: Partial Predictions Allowed

- Users are not required to predict all competition entries
- Minimum predictions: At least 1 prediction required for submission
- Users can submit predictions for any subset of available entries

### BR-008-005: Draft vs. Final Submission

- Draft predictions can be saved without final submission
- Draft predictions can be modified unlimited times before deadline
- Final submission locks predictions (can still be edited via UC-010 before deadline)

### BR-008-006: Authentication Requirement

- User must be authenticated throughout the entire process
- If session expires, user must re-authenticate (UC-002)
- Entered data should be preserved through re-authentication if possible

## Non-Functional Requirements

### Performance

- Competition entries grid should load within 2 seconds
- Save operation should complete within 3 seconds for up to 100 predictions
- Page should handle competitions with up to 500 competition entries efficiently

### Usability

- Grid should be sortable and filterable (by gymnast, team, apparatus, gender)
- Input fields should support keyboard navigation (Tab, Enter)
- Score input should accept common formats (14.5, 14.50, 14.500)
- Auto-save draft every 60 seconds to prevent data loss
- Show countdown timer until prediction deadline
- Provide bulk operations (e.g., "Predict same score for all apparatus of one gymnast")

### Accessibility

- Grid must be keyboard navigable
- All input fields must have proper labels
- Error messages must be announced to screen readers
- Deadline countdown must be accessible to screen readers
- Proper ARIA attributes for grid and form elements

### Reliability

- Auto-save functionality to prevent data loss
- Session management to handle long editing sessions
- Optimistic locking to prevent concurrent edit conflicts

## UI Components (Vaadin Flow)

### View Structure

```
PredictionView
├── Header: "Make Predictions"
├── CompetitionSelector
│   ├── ComboBox: Select Competition
│   └── Details: Competition date, status, deadline
├── PredictionDeadlineInfo
│   ├── Label: "Deadline: {datetime}"
│   └── Badge: "{time remaining}" or "CLOSED"
├── FilterPanel (collapsible)
│   ├── TextField: Filter by gymnast name
│   ├── ComboBox: Filter by team
│   ├── ComboBox: Filter by apparatus
│   └── RadioButtonGroup: Filter by gender (All/M/F)
├── Grid<CompetitionEntry>
│   ├── Column: Gymnast Name (sortable)
│   ├── Column: Team Name (sortable)
│   ├── Column: Apparatus (sortable)
│   ├── Column: Gender
│   └── Column: Predicted Score (NumberField)
├── Statistics
│   └── Label: "Predictions entered: {count} of {total}"
├── HorizontalLayout (Buttons)
│   ├── Button: "Save Draft" (tertiary)
│   ├── Button: "Clear All" (tertiary, danger)
│   ├── Button: "Cancel" (tertiary)
│   └── Button: "Submit Predictions" (primary)
└── ConfirmDialog (for submission)
```

### Components Used

- `VerticalLayout` - Main container
- `ComboBox` - Competition selection
- `Grid` - Competition entries display
- `NumberField` - Score input with validation
- `Button` - Actions (save, submit, cancel)
- `Badge` - Status and countdown display
- `ConfirmDialog` - Confirmation before final submission
- `Notification` - Success/error messages
- `ProgressBar` - For save operation feedback
- `Details` - Collapsible filter panel

### Grid Configuration

```java
grid.addColumn(entry -> entry.getGymnast().getName())
    .setHeader("Gymnast")
    .setSortable(true)
    .setResizable(true);

grid.addColumn(entry -> entry.getGymnast().getTeamName())
    .setHeader("Team")
    .setSortable(true);

grid.addColumn(entry -> entry.getApparatus().getName())
    .setHeader("Apparatus")
    .setSortable(true);

grid.addColumn(entry -> entry.getGymnast().getGender())
    .setHeader("Gender");

grid.addComponentColumn(entry -> {
    NumberField scoreField = new NumberField();
    scoreField.setStep(0.001);
    scoreField.setMin(0.0);
    scoreField.setMax(20.0);
    scoreField.setPlaceholder("0.000");
    return scoreField;
}).setHeader("Predicted Score");
```

## Data Model Impact

### Tables Affected

- `prediction` (INSERT, potentially UPDATE if drafts are saved)

### Fields Written

```sql
INSERT INTO prediction (
    user_id,                    -- Current authenticated user ID
    competition_entry_id,        -- Selected competition entry
    predicted_score,             -- User's predicted score (DECIMAL(5,3))
    points_earned,               -- NULL (calculated later after actual scores)
    created_at,                  -- Current timestamp
    updated_at                   -- Current timestamp
)
VALUES (?, ?, ?, NULL, NOW(), NOW());
```

### Constraints

- Unique constraint: `(user_id, competition_entry_id)` - one prediction per user per entry
- Foreign key: `user_id` references `app_user(id)`
- Foreign key: `competition_entry_id` references `competition_entry(id)`

### Queries Required

```sql
-- Get available competitions for predictions
SELECT * FROM competition
WHERE status = 'upcoming'
  AND start_datetime > NOW() + INTERVAL '30 minutes'
ORDER BY start_datetime ASC;

-- Get competition entries for selected competition
SELECT ce.*, g.name, g.team_name, g.gender, a.name as apparatus_name
FROM competition_entry ce
JOIN gymnast g ON ce.gymnast_id = g.id
JOIN apparatus a ON ce.apparatus_id = a.id
WHERE ce.competition_id = ?
ORDER BY g.name, a.name;

-- Check for existing predictions
SELECT * FROM prediction
WHERE user_id = ?
  AND competition_entry_id IN (?);
```

## Test Scenarios

### Success Cases

1. **Happy Path**: User selects competition, enters predictions for all entries, submits successfully
2. **Partial Predictions**: User predicts only some entries → Saved successfully
3. **Save Draft**: User saves draft multiple times → All changes preserved
4. **Different Score Formats**: User enters 14.5, 14.50, 14.500 → All normalized correctly
5. **Filter and Predict**: User filters by apparatus, enters predictions → Correct entries saved

### Validation Failure Cases

6. **Score Too High**: User enters 25.000 → Error: "Score must be between 0.000 and 20.000"
7. **Score Too Low**: User enters -5.000 → Error displayed
8. **Invalid Format**: User enters "abc" → Error: "Please enter a valid number"
9. **Too Many Decimals**: User enters 14.12345 → Rounded to 14.123 or error
10. **Empty Submission**: User clicks submit without any predictions → Error: "Please enter at least one prediction"

### Business Rule Cases

11. **Deadline Check**: 25 minutes before start → Predictions allowed
12. **Deadline Passed**: 15 minutes before start → Predictions blocked
13. **Competition Started**: Status changed to "live" → All inputs disabled
14. **Session Timeout**: User session expires → Prompted to login, data preserved

### Edge Cases

15. **Large Competition**: 500 entries → Grid loads and performs well
16. **Concurrent Users**: Multiple users predict same competition → All saved independently
17. **Browser Refresh**: User refreshes page with unsaved data → Warning displayed or auto-save recovered
18. **Network Interruption**: Connection lost during save → Retry mechanism or error handling
19. **Duplicate Submission**: User clicks submit twice → Only one set saved, duplicate prevented

### Integration Cases

20. **Existing Predictions**: User already has predictions for this competition → Redirect to UC-010 (Edit)
21. **Competition Data Changed**: Admin modifies entries during user session → User notified of changes

## Future Enhancements

1. **Smart Predictions**
   - Suggest predictions based on gymnast historical scores
   - Auto-fill based on competition averages
   - Machine learning predictions as guidance

2. **Bulk Operations**
   - Predict same score for all apparatus of one gymnast
   - Copy predictions from previous similar competition
   - Import predictions from CSV/Excel

3. **Real-time Collaboration**
   - See how many other users have predicted each entry
   - Show average community prediction (optional, may influence users)
   - Live countdown synchronized across all users

4. **Enhanced UX**
   - Keyboard shortcuts for rapid entry
   - Mobile-optimized interface
   - Touch gestures for navigation
   - Voice input for predictions

5. **Analytics**
   - Show user's historical accuracy per apparatus
   - Suggest which entries are most important (high-scoring gymnasts)
   - Risk/reward indicators

6. **Gamification**
   - Achievement badges for prediction milestones
   - Bonus points for completing all predictions
   - Streak tracking for consecutive competitions

7. **Notifications**
   - Email reminder 24 hours before deadline
   - Push notification 1 hour before deadline
   - Mobile app notifications

8. **Draft Sharing**
   - Share draft predictions with friends
   - Team prediction pools
   - Discussion threads per competition

## Related Use Cases

- **UC-002**: Login - Required before making predictions (include relationship)
- **UC-003**: View Competitions - Users may navigate here to see competition details
- **UC-010**: Edit Predictions - User can modify predictions before deadline (extend relationship)
- **UC-009**: View Own Predictions - View submitted predictions after completion
- **UC-014**: Calculate Points - Points calculated after actual scores are entered
- **UC-015**: View Leaderboard - See how predictions perform compared to others
