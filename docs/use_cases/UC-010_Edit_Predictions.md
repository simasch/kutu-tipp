# UC-010: Edit Predictions

## Brief Description

This use case allows an authenticated user (Tipper) to edit their existing predictions for a competition before the prediction deadline. Users can modify previously entered scores, add new predictions for entries they skipped, or delete existing predictions.

## Actors

- **Primary Actor**: Tipper (User) - authenticated user who wants to modify their predictions
- **Secondary Actor**: System - validates changes and updates predictions in the database

## Preconditions

- User is authenticated (UC-002)
- User has previously submitted predictions for a competition (UC-008)
- Competition still has status "upcoming"
- Current time is more than 30 minutes before competition start time (deadline not passed)

## Postconditions

- **Success**: User's modified predictions are saved in the database
- **Success**: Previous prediction values are overwritten with new values
- **Success**: User receives confirmation of successful update
- **Success**: Deleted predictions are removed from database
- **Failure**: No changes are saved, original predictions remain intact

## Main Success Scenario (Basic Flow)

1. User navigates to predictions page or "My Predictions" page
2. System displays list of competitions for which user has made predictions
3. System indicates which competitions are still editable (deadline not passed)
4. User selects a competition to edit
5. System retrieves all competition entries for the selected competition
6. System loads user's existing predictions
7. System displays a grid with the following columns:
   - Gymnast name
   - Team name
   - Apparatus name
   - Gender
   - Predicted score (pre-filled with existing prediction or empty)
8. System highlights entries that already have predictions
9. User modifies predicted scores for desired entries
10. User may add predictions for previously unpredicted entries
11. User may clear predictions for specific entries (deletion)
12. User clicks "Update Predictions" button
13. System validates all modified predictions (see Business Rules)
14. System checks that prediction deadline has not passed
15. System updates existing predictions in the database
16. System inserts new predictions for newly added entries
17. System deletes predictions that were cleared
18. System displays summary of changes (updated, added, deleted counts)
19. System displays success notification
20. System refreshes view with updated predictions

## Alternative Flows

### 3a. No Editable Competitions

- At step 3, if all competitions have passed their deadline:
  1. System displays message: "All your predictions are locked. Deadlines have passed."
  2. System offers to view read-only predictions (UC-009)
  3. Use case ends

### 4a. User Cancels Editing

- At step 4, user clicks "Cancel" or navigates away without saving:
  1. System displays confirmation dialog: "Discard unsaved changes?"
  2. If confirmed:
     - System discards all changes
     - Use case ends
  3. If not confirmed:
     - User returns to step 9

### 9a. User Wants to Preview Changes

- At step 9, user clicks "Preview Changes" button:
  1. System displays side-by-side comparison:
     - Original prediction
     - Modified prediction
     - Change indicator (modified/added/deleted)
  2. User can confirm or continue editing
  3. Use case continues from step 9 or proceeds to step 12

### 14a. Validation Errors

- At step 14, if any modified prediction is invalid:
  1. System displays specific error messages next to invalid fields
  2. System highlights invalid entries in the grid
  3. System keeps all valid entries with their values
  4. User returns to step 9 to correct errors

### 15a. Deadline Passed During Edit

- At step 15, if deadline has passed since editing began:
  1. System displays error: "Prediction deadline has passed. Changes cannot be saved."
  2. System shows current time and competition start time
  3. No changes are saved
  4. System offers to view read-only predictions
  5. Use case ends

### 15b. No Changes Made

- At step 15, if user hasn't modified any predictions:
  1. System detects no changes
  2. System displays notification: "No changes to save."
  3. User returns to step 9 or exits

## Exception Flows

### E1: Database Connection Error

- If database is unavailable at step 15-17:
  1. System displays error: "Unable to save changes. Please try again later."
  2. System logs the error for administrators
  3. System attempts to preserve edited data in browser session
  4. Use case ends

### E2: Concurrent Modification Conflict

- If another session modified same predictions (rare, but possible with multiple devices):
  1. System detects conflict via optimistic locking
  2. System displays warning: "Your predictions were modified in another session."
  3. System offers options:
     - View other session's values
     - Overwrite with current changes
     - Merge changes (if possible)
  4. User makes selection
  5. Use case continues based on selection

### E3: Competition State Changed

- If competition status changed to "live" or "finished" during editing:
  1. System detects state change
  2. System displays warning: "This competition has started. Changes cannot be saved."
  3. System disables all input fields
  4. System shows read-only view
  5. Use case ends

### E4: System Error

- If any unexpected error occurs:
  1. System displays generic error: "An error occurred. Please try again."
  2. System logs the error with details
  3. System attempts to preserve edited data
  4. Use case ends

## Business Rules

### BR-010-001: Edit Deadline (Inherits from UC-008)

- Predictions can only be edited if current time is at least 30 minutes before competition start time
- Formula: `current_time + 30 minutes <= competition.start_datetime`
- Once deadline passes, all editing is disabled
- Same deadline applies to both initial submission (UC-008) and editing (UC-010)

### BR-010-002: Score Range Validation (Inherits from UC-008)

- Modified scores must meet same validation as initial predictions
- **Valid range**: 0.000 to 20.000
- **Precision**: Up to 3 decimal places
- **Format**: Must be a valid decimal number

### BR-010-003: Ownership Validation

- Users can only edit their own predictions
- Cannot view or modify other users' predictions
- System verifies `user_id` matches authenticated user for all operations

### BR-010-004: Partial Edits Allowed

- Users can modify any subset of their predictions
- Not required to edit all predictions at once
- Can edit just one prediction and leave others unchanged

### BR-010-005: Deletion Allowed

- Users can delete predictions before deadline
- Clearing a score field marks prediction for deletion
- Confirmation required for bulk deletions (e.g., "Clear All")

### BR-010-006: Addition Allowed

- Users can add predictions for entries they didn't predict initially
- Same validation applies as in UC-008
- Edit session allows both modification of existing and addition of new predictions

### BR-010-007: Change Tracking

- System tracks when predictions were last modified
- `updated_at` timestamp updated on each change
- Optional: Keep audit trail of changes (future enhancement)

## Non-Functional Requirements

### Performance

- Loading existing predictions should complete within 2 seconds
- Update operation should complete within 3 seconds for up to 100 changes
- Change detection should be efficient (compare only modified fields)

### Usability

- Clear visual distinction between:
  - Entries with existing predictions (e.g., highlighted background)
  - Entries without predictions (normal background)
  - Modified predictions (e.g., colored border or indicator)
- Show original value tooltip on hover for modified fields
- Undo/Redo functionality for changes within session
- Auto-save changes every 60 seconds
- Show unsaved changes indicator

### Accessibility

- All interactive elements must be keyboard accessible
- Screen readers must announce modification state
- Change indicators must not rely solely on color
- Undo/Redo must be keyboard accessible (Ctrl+Z, Ctrl+Y)

### Data Integrity

- Optimistic locking to prevent concurrent edit conflicts
- Transaction support for batch updates (all changes committed or none)
- Backup original values before update in case of rollback
- Audit trail for tracking changes (optional, future enhancement)

## UI Components (Vaadin Flow)

### View Structure

```
EditPredictionView (extends/reuses PredictionView)
├── Header: "Edit Predictions"
├── CompetitionSelector
│   ├── ComboBox: Select Competition (shows only editable ones)
│   └── Details: Competition date, status, deadline, edit deadline
├── PredictionDeadlineInfo
│   ├── Label: "Edit Deadline: {datetime}"
│   └── Badge: "{time remaining}" or "LOCKED"
├── ChangesSummary
│   ├── Label: "Modified: {count}"
│   ├── Label: "Added: {count}"
│   └── Label: "Deleted: {count}"
├── FilterPanel (collapsible)
│   ├── TextField: Filter by gymnast name
│   ├── ComboBox: Filter by team
│   ├── ComboBox: Filter by apparatus
│   ├── RadioButtonGroup: Filter by gender (All/M/F)
│   └── Checkbox: "Show only my predictions"
├── Grid<CompetitionEntry>
│   ├── Column: Gymnast Name (sortable)
│   ├── Column: Team Name (sortable)
│   ├── Column: Apparatus (sortable)
│   ├── Column: Gender
│   ├── Column: Original Score (read-only, styled)
│   └── Column: Predicted Score (NumberField, pre-filled or empty)
├── HorizontalLayout (Buttons)
│   ├── Button: "Undo" (tertiary, disabled if no changes)
│   ├── Button: "Redo" (tertiary, disabled if nothing to redo)
│   ├── Button: "Preview Changes" (tertiary)
│   ├── Button: "Clear All" (tertiary, danger)
│   ├── Button: "Cancel" (tertiary)
│   └── Button: "Update Predictions" (primary, disabled if no changes)
└── ConfirmDialog (for update confirmation)
    └── Shows summary of changes before saving
```

### Visual Indicators

```java
// Grid row styling based on prediction status
grid.setClassNameGenerator(entry -> {
    if (hasExistingPrediction(entry)) {
        return "has-prediction";
    }
    return "";
});

// Add change indicator to modified fields
scoreField.addValueChangeListener(event -> {
    if (!Objects.equals(event.getOldValue(), event.getValue())) {
        scoreField.addClassName("modified");
        updateChangesSummary();
    }
});
```

### CSS Styling

```css
/* Highlight rows with existing predictions */
.has-prediction {
    background-color: var(--lumo-primary-color-10pct);
}

/* Indicate modified fields */
.modified {
    border-left: 3px solid var(--lumo-warning-color);
}

/* Show delete indication */
.marked-for-deletion {
    text-decoration: line-through;
    opacity: 0.6;
}
```

## Data Model Impact

### Tables Affected

- `prediction` (UPDATE, INSERT, DELETE)

### Update Operation

```sql
-- Update existing prediction
UPDATE prediction
SET predicted_score = ?,      -- New predicted score
    updated_at = NOW()         -- Update timestamp
WHERE id = ?
  AND user_id = ?              -- Verify ownership
  AND EXISTS (
    SELECT 1 FROM competition_entry ce
    JOIN competition c ON ce.competition_id = c.id
    WHERE ce.id = prediction.competition_entry_id
      AND c.start_datetime > NOW() + INTERVAL '30 minutes'
  );  -- Verify deadline not passed
```

### Insert Operation (for new predictions added during edit)

```sql
-- Same as UC-008
INSERT INTO prediction (
    user_id,
    competition_entry_id,
    predicted_score,
    points_earned,
    created_at,
    updated_at
)
VALUES (?, ?, ?, NULL, NOW(), NOW());
```

### Delete Operation

```sql
-- Delete prediction (user cleared the field)
DELETE FROM prediction
WHERE id = ?
  AND user_id = ?              -- Verify ownership
  AND EXISTS (
    SELECT 1 FROM competition_entry ce
    JOIN competition c ON ce.competition_id = c.id
    WHERE ce.id = prediction.competition_entry_id
      AND c.start_datetime > NOW() + INTERVAL '30 minutes'
  );  -- Verify deadline not passed
```

### Queries Required

```sql
-- Get user's existing predictions for a competition
SELECT p.*, ce.gymnast_id, ce.apparatus_id
FROM prediction p
JOIN competition_entry ce ON p.competition_entry_id = ce.id
WHERE p.user_id = ?
  AND ce.competition_id = ?;

-- Check if competition is still editable
SELECT c.*
FROM competition c
WHERE c.id = ?
  AND c.status = 'upcoming'
  AND c.start_datetime > NOW() + INTERVAL '30 minutes';
```

## Test Scenarios

### Success Cases

1. **Happy Path**: User modifies several predictions, updates successfully
2. **Add New Predictions**: User adds predictions for previously skipped entries → All saved
3. **Delete Predictions**: User clears some predictions → Deleted successfully
4. **Mixed Operations**: User modifies, adds, and deletes in one session → All changes applied correctly
5. **Single Change**: User modifies just one prediction → Only that prediction updated

### Validation Cases

6. **Invalid Modified Score**: User changes score to invalid value → Error displayed, update blocked
7. **No Changes Made**: User clicks update without changes → Notification shown, no database operation
8. **Partial Valid Changes**: Some valid, some invalid changes → Only valid changes saved or all blocked
9. **Clear and Re-enter**: User clears field then re-enters same value → No database operation needed

### Business Rule Cases

10. **Deadline Check During Edit**: User starts editing with 40 mins left, deadline passes → Update blocked
11. **Just Before Deadline**: 31 minutes before start → Update allowed
12. **Competition Started During Edit**: Status changes to "live" → Changes blocked, warning shown
13. **Edit After Initial Submission**: User submits via UC-008, immediately edits → Both operations succeed

### Ownership and Security Cases

14. **Edit Own Predictions**: User edits their own predictions → Allowed
15. **Cannot Edit Others**: User attempts to modify another user's prediction ID → Blocked by system
16. **Session Validation**: User's session validated on each operation → Unauthorized changes prevented

### Edge Cases

17. **Large Number of Changes**: User modifies 200 predictions → All updated in single transaction
18. **Concurrent Edits from Different Devices**: User edits from laptop and phone → Conflict detected and resolved
19. **Browser Refresh with Unsaved Changes**: User refreshes page → Warning shown or auto-save recovered
20. **Rapid Consecutive Edits**: User modifies same field multiple times quickly → Debounced, final value saved
21. **Delete All Predictions**: User clears all predictions → All deleted after confirmation

### Integration Cases

22. **Edit After Partial Submission**: User submitted 10 predictions via UC-008, edits and adds 5 more → Total 15 predictions
23. **Switch Between Competitions**: User edits one competition, switches to another → Changes to first must be saved or discarded
24. **Competition Modified by Admin**: Admin changes competition entries during user edit → User notified, data refreshed

## Future Enhancements

1. **Change History**
   - View complete edit history for predictions
   - See timeline of modifications
   - Restore previous prediction values
   - Show who made changes and when (audit trail)

2. **Advanced Edit Features**
   - Bulk edit operations (apply same change to multiple entries)
   - Find and replace functionality
   - Copy predictions from one apparatus to another
   - Increment/decrement all predictions by percentage

3. **Comparison Tools**
   - Compare current predictions with previous competition
   - Side-by-side diff view of changes
   - Show potential points impact of changes
   - "What if" scenario analysis

4. **Collaboration Features**
   - Comments on specific predictions
   - Share changes with friends for feedback
   - Team editing sessions (if team pools implemented)
   - Challenge friends with prediction differences

5. **Smart Edit Suggestions**
   - Flag predictions that are outliers compared to averages
   - Suggest corrections based on gymnast historical performance
   - Warn if prediction seems unrealistic
   - Show confidence intervals

6. **Undo/Redo Enhancement**
   - Extended undo history (multiple sessions)
   - Named save points
   - Branch and merge different prediction versions
   - Export/import prediction snapshots

7. **Mobile Optimization**
   - Swipe gestures to edit predictions
   - Quick edit mode for rapid changes
   - Offline editing with sync when online
   - Dedicated mobile app with enhanced editing

8. **Notifications**
   - Email notification when predictions are successfully updated
   - Reminder if user started editing but didn't save
   - Alert when deadline is approaching (e.g., 1 hour remaining)
   - Push notification for important changes

## Related Use Cases

- **UC-002**: Login - Required before editing predictions (include relationship)
- **UC-008**: Make Predictions - UC-010 extends this use case (extend relationship)
- **UC-009**: View Own Predictions - Users may view before deciding to edit
- **UC-003**: View Competitions - View competition details before editing
- **UC-014**: Calculate Points - Points recalculated if predictions modified
- **UC-015**: View Leaderboard - Standings may change after prediction edits
