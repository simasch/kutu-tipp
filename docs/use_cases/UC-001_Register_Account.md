# UC-001: Register Account

## Brief Description

This use case allows a new user to create an account in the Kutu-Tipp system to participate in the prediction game.

## Actors

- **Primary Actor**: Tipper (User) - anonymous visitor who wants to create an account
- **Secondary Actor**: System - validates and stores registration data

## Preconditions

- User is not authenticated
- User has access to the Kutu-Tipp application
- User does not have an existing account

## Postconditions

- **Success**: A new user account is created with USER role
- **Success**: User credentials are securely stored in the database
- **Success**: User is redirected to the login page
- **Failure**: No account is created

## Main Success Scenario (Basic Flow)

1. User navigates to the registration page
2. System displays the registration form with the following fields:
    - Username (required)
    - Email (required)
    - Password (required)
    - Password confirmation (required)
3. User enters their desired username
4. User enters their email address
5. User enters their desired password
6. User re-enters password for confirmation
7. User clicks the "Register" button
8. System validates all input fields (see Business Rules)
9. System checks that the username is unique
10. System checks that the email is unique
11. System hashes the password using a secure algorithm (e.g., BCrypt)
12. System creates a new user record with role USER
13. System displays a success message
14. System redirects user to the login page

## Alternative Flows

### 2a. User Already Has Account

- At step 2, user clicks "Already have an account? Login" link
- System redirects user to UC-002 (Login)

### 9a. Validation Errors

- At step 9, if any validation fails:
    1. System displays specific error message(s) next to the invalid field(s)
    2. System keeps valid field values filled in
    3. User returns to step 3 to correct the errors

### 10a. Username Already Exists

- At step 10, if username is already taken:
    1. System displays error: "Username already exists. Please choose another username."
    2. System highlights the username field
    3. User returns to step 3

### 11a. Email Already Exists

- At step 11, if email is already registered:
    1. System displays error: "This email address is already registered."
    2. System provides link to login page
    3. User can either:
        - Return to step 4 with a different email
        - Navigate to login page

## Exception Flows

### E1: Database Connection Error

- If database is unavailable at step 12:
    1. System displays error: "Registration temporarily unavailable. Please try again later."
    2. System logs the error for administrators
    3. Use case ends

### E2: System Error

- If any unexpected error occurs:
    1. System displays generic error: "An error occurred during registration. Please try again."
    2. System logs the error with details
    3. Use case ends

## Business Rules

### BR-001: Username Validation

- **Length**: 3-30 characters
- **Characters**: Alphanumeric characters, underscore, and hyphen allowed
- **Pattern**: Must start with a letter or number
- **Uniqueness**: Must be unique (case-insensitive)
- **Reserved**: Cannot use reserved system names (e.g., "admin", "system", "root")

### BR-002: Email Validation

- **Format**: Must be a valid email format (RFC 5322)
- **Length**: Maximum 255 characters
- **Uniqueness**: Must be unique (case-insensitive)
- **Verification**: Email verification can be added in future enhancement

### BR-003: Password Requirements

- **Length**: Minimum 8 characters
- **Complexity**: Must contain at least:
    - One uppercase letter
    - One lowercase letter
    - One digit
    - One special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
- **Security**: Stored using BCrypt hashing with appropriate salt rounds
- **Confirmation**: Must match password confirmation field

### BR-004: Default Role Assignment

- All self-registered users receive USER role
- ADMIN role can only be assigned by existing administrators through backend/database

### BR-005: Account Activation

- Initial implementation: Accounts are immediately active
- Future enhancement: Email verification before activation

## Non-Functional Requirements

### Performance

- Registration process should complete within 2 seconds under normal load
- Password hashing must use appropriate cost factor to balance security and performance

### Security

- Password must never be stored in plain text
- Use BCrypt with minimum cost factor of 10
- Passwords must not be logged or displayed in error messages
- HTTPS must be enforced for registration page
- Protection against automated registrations (consider CAPTCHA in future)

### Usability

- Form should provide real-time validation feedback
- Error messages should be clear and actionable
- Password strength indicator should guide users
- Password field should have show/hide toggle

### Accessibility

- Form must be keyboard navigable
- All fields must have proper labels for screen readers
- Error messages must be announced to screen readers
- Proper ARIA attributes for form validation states

## UI Components (Vaadin Flow)

### View Structure

```
RegistrationView
├── Header: "Create Your Account"
├── FormLayout
│   ├── TextField: Username
│   ├── EmailField: Email
│   ├── PasswordField: Password
│   ├── PasswordField: Confirm Password
│   └── (Optional) PasswordStrengthIndicator
├── HorizontalLayout (Buttons)
│   ├── Button: "Register" (primary)
│   └── Button: "Cancel" (tertiary)
└── HorizontalLayout (Link)
    └── RouterLink: "Already have an account? Login"
```

### Components Used

- `VerticalLayout` - Main container
- `FormLayout` - Form structure
- `TextField` - Username input
- `EmailField` - Email input with built-in validation
- `PasswordField` - Password inputs
- `Button` - Submit and cancel actions
- `RouterLink` - Navigation to login
- `Notification` - Success/error messages
- `Binder` - Form validation and data binding

## Data Model Impact

### Tables Affected

- `app_user` (INSERT)

### Fields Written

```sql
INSERT INTO app_user (username, -- user input (validated)
                      email, -- user input (validated, lowercase)
                      password_hash, -- BCrypt hash of password
                      role, -- 'USER' (default)
                      created_at, -- current timestamp
                      updated_at -- current timestamp
)
VALUES (?, ?, ?, ?, ?, ?);
```

## Test Scenarios

### Success Cases

1. **Happy Path**: Valid data in all fields → Account created successfully
2. **Minimum Username Length**: 3 characters → Account created
3. **Maximum Username Length**: 30 characters → Account created
4. **Email with Plus Addressing**: user+tag@example.com → Account created

### Validation Failure Cases

5. **Short Username**: 2 characters → Error displayed
6. **Invalid Email**: "notanemail" → Error displayed
7. **Weak Password**: "12345678" → Error displayed
8. **Password Mismatch**: Different passwords → Error displayed
9. **Empty Fields**: Missing required fields → Errors displayed

### Business Rule Violation Cases

10. **Duplicate Username**: Existing username → Error displayed
11. **Duplicate Email**: Existing email → Error displayed
12. **Reserved Username**: "admin" → Error displayed

### Edge Cases

13. **Special Characters in Username**: "user@123" → Error displayed (@ not allowed)
14. **SQL Injection Attempt**: SQL in username → Sanitized, account created or error
15. **Very Long Email**: 256 characters → Error displayed
16. **Unicode Characters**: "üsername" → Handle gracefully (accept or reject with clear message)

## Future Enhancements

1. **Email Verification**
    - Send confirmation email with verification link
    - Account remains inactive until email verified

2. **Social Registration**
    - OAuth integration (Google, Facebook)
    - Link social accounts to profile

3. **CAPTCHA Protection**
    - Add reCAPTCHA to prevent bot registrations

4. **Username Suggestions**
    - Suggest available usernames if chosen one is taken

5. **Profile Information**
    - Optional: First name, last name, preferred team
    - Avatar upload

6. **Terms of Service**
    - Require acceptance of terms and conditions
    - Privacy policy acceptance

7. **Password Strength Meter**
    - Visual indicator of password strength
    - Suggestions for stronger passwords

## Related Use Cases

- **UC-002**: Login - Users navigate here after successful registration
- **UC-009**: View Own Predictions - Requires authenticated account created via registration
