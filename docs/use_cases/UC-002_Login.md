# UC-002: Login

## Brief Description

This use case allows a registered user to authenticate and access the Kutu-Tipp system to participate in the prediction
game or manage competitions (for administrators).

## Actors

- **Primary Actor**: Tipper (User) or Administrator - any registered user who wants to access the system
- **Secondary Actor**: System - validates credentials and establishes user session

## Preconditions

- User has a registered account in the system
- User is not currently authenticated
- User has access to the Kutu-Tipp application

## Postconditions

- **Success**: User is authenticated and logged into the system
- **Success**: User session is established with appropriate role (USER or ADMIN)
- **Success**: User is redirected to appropriate landing page based on role
- **Failure**: User remains unauthenticated

## Main Success Scenario (Basic Flow)

1. User navigates to the login page
2. System displays the login form with the following fields:
    - Username (required)
    - Password (required)
    - Optional: "Remember me" checkbox
3. User enters their username
4. User enters their password
5. User optionally checks "Remember me" for extended session
6. User clicks the "Login" button
7. System validates that both fields are filled
8. System retrieves user record by username (case-insensitive)
9. System verifies the password against the stored BCrypt hash
10. System creates an authenticated session with user details and role
11. System logs the successful login attempt
12. System redirects user to appropriate landing page:
    - USER role → Competition overview page
    - ADMIN role → Admin dashboard
13. System displays welcome message with username

## Alternative Flows

### 2a. User Doesn't Have Account

- At step 2, user clicks "Don't have an account? Register here" link
- System redirects user to UC-001 (Register Account)

### 8a. Username Not Found

- At step 8, if username does not exist:
    1. System displays generic error: "Invalid username or password"
    2. System does NOT reveal whether username exists (security)
    3. System logs the failed login attempt with username
    4. User returns to step 3

### 9a. Password Incorrect

- At step 9, if password does not match:
    1. System displays generic error: "Invalid username or password"
    2. System increments failed login attempt counter for the account
    3. System logs the failed login attempt
    4. User returns to step 3

### 9b. Account Locked (Future Enhancement)

- At step 9, if account is locked due to too many failed attempts:
    1. System displays error: "Account is temporarily locked. Please try again later or contact support."
    2. System logs the login attempt on locked account
    3. Use case ends

### 9c. Account Inactive (Future Enhancement)

- At step 9, if account is inactive (e.g., email not verified):
    1. System displays error: "Account is not active. Please verify your email address."
    2. System provides link to resend verification email
    3. Use case ends

## Exception Flows

### E1: Database Connection Error

- If database is unavailable at step 8:
    1. System displays error: "Login temporarily unavailable. Please try again later."
    2. System logs the error for administrators
    3. Use case ends

### E2: Session Creation Error

- If session cannot be created at step 10:
    1. System displays error: "An error occurred during login. Please try again."
    2. System logs the error with details
    3. Use case ends

### E3: System Error

- If any unexpected error occurs:
    1. System displays generic error: "An error occurred. Please try again."
    2. System logs the error with details
    3. Use case ends

## Business Rules

### BR-001: Username Matching

- Username matching is case-insensitive
- Leading and trailing whitespace is trimmed before lookup
- Username is used as the primary identifier for login (not email)

### BR-002: Password Verification

- Password is verified using BCrypt comparison
- Password is NEVER logged or displayed in error messages
- Generic error message is shown for both invalid username and password (security)

### BR-003: Session Management

- Session includes: user ID, username, email, role
- Session timeout: 30 minutes of inactivity (default Spring Security)
- "Remember me" extends session to 14 days (if implemented)
- Only one active session per user (optional - can be configured)

### BR-004: Failed Login Attempts

- Failed attempts are logged for security monitoring
- Account lockout after N failed attempts (future enhancement)
- Lockout duration: 15-30 minutes (future enhancement)
- Failed attempt counter resets after successful login

### BR-005: Security Logging

- All login attempts (successful and failed) are logged
- Logged information: timestamp, username, IP address, result
- Passwords are NEVER logged

## Non-Functional Requirements

### Performance

- Login process should complete within 1 second under normal load
- BCrypt verification is computationally expensive but necessary for security
- Database query should use indexed username field

### Security

- Implement CSRF protection (Spring Security default)
- Use HTTPS for login page in production
- Generic error messages to prevent username enumeration
- BCrypt password verification (cost factor 10)
- Session fixation protection (Spring Security default)
- XSS protection in all form fields
- Rate limiting on login endpoint (future enhancement)

### Usability

- Clear error messages without revealing security information
- Password field masked by default with option to show
- Auto-focus on username field when page loads
- Enter key submits the form
- Clear indication when login is processing
- Remember previously entered username (browser feature)

### Accessibility

- Form must be keyboard navigable
- All fields must have proper labels for screen readers
- Error messages must be announced to screen readers
- Proper ARIA attributes for form validation states
- High contrast for login button

## UI Components (Vaadin Flow)

### View Structure

```
LoginView
├── Header: "Kutu-Tipp Login"
├── FormLayout
│   ├── TextField: Username
│   └── PasswordField: Password
├── HorizontalLayout (Actions)
│   ├── Checkbox: "Remember me" (optional)
│   └── Button: "Login" (primary)
└── HorizontalLayout (Links)
    └── RouterLink: "Don't have an account? Register here"
```

### Components Used

- `VerticalLayout` - Main container
- `LoginForm` - Vaadin's built-in login form component
- `TextField` - Username input
- `PasswordField` - Password input
- `Button` - Submit action
- `RouterLink` - Navigation to registration
- `Notification` - Success/error messages

## Data Model Impact

### Tables Accessed

- `app_user` (SELECT) - Read user record for authentication

### Fields Read

```sql
SELECT id, username, email, password_hash, role, created_at, updated_at
FROM app_user
WHERE LOWER(username) = LOWER(?)
```

## Authentication Flow

### Spring Security Integration

1. **Login Form Submission**
    - Form data posted to `/login` (Spring Security default)
    - Spring Security intercepts the request

2. **UserDetailsService**
    - Custom implementation loads user by username
    - Returns `UserDetails` object with authorities

3. **Password Verification**
    - Spring Security uses configured `PasswordEncoder` (BCrypt)
    - Compares submitted password with stored hash

4. **Authentication Success**
    - Spring Security creates `Authentication` object
    - Stores in `SecurityContext`
    - Session is established

5. **Redirect**
    - Success: Redirect to default success URL or requested page
    - Failure: Redirect to `/login?error`

## Test Scenarios

### Success Cases

1. **Happy Path**: Valid username and password → Logged in successfully
2. **Case-Insensitive Username**: "JohnDoe" vs "johndoe" → Both work
3. **Whitespace Handling**: " username " → Trimmed and works
4. **USER Role**: Regular user → Redirected to competition overview
5. **ADMIN Role**: Admin user → Redirected to admin dashboard

### Authentication Failure Cases

6. **Invalid Username**: Non-existent username → Generic error
7. **Invalid Password**: Wrong password → Generic error
8. **Empty Username**: Missing username → Validation error
9. **Empty Password**: Missing password → Validation error
10. **Empty Both Fields**: Missing both → Validation errors

### Security Cases

11. **SQL Injection**: Malicious username → Sanitized, rejected
12. **XSS Attempt**: Script in username → Escaped, rejected
13. **Brute Force**: Multiple failed attempts → Logged (lockout in future)

### Session Cases

14. **Already Logged In**: Authenticated user visits login → Redirected to home
15. **Session Timeout**: Inactive for 30 minutes → Must login again
16. **Remember Me**: Extended session → Works for 14 days

## Integration Points

### UC-001: Register Account

- Registration success redirects to login page
- New users can login immediately after registration

### UC-003: View Competitions

- Successful login redirects USER role to competition overview

### UC-008: Make Predictions

- Requires authenticated session from login
- Session provides user identity for predictions

### Admin Functions (UC-004 to UC-007)

- Require ADMIN role from authenticated session
- Role-based access control enforced

## Future Enhancements

1. **Account Lockout**
    - Lock account after 5 failed login attempts
    - Automatic unlock after 30 minutes
    - Manual unlock by administrator

2. **Email-based Login**
    - Allow login with email address instead of username
    - Maintain backward compatibility with username login

3. **Two-Factor Authentication (2FA)**
    - Optional 2FA for enhanced security
    - TOTP-based (Google Authenticator, Authy)
    - Recovery codes for 2FA backup

4. **Social Login**
    - OAuth integration (Google, Facebook)
    - Link social accounts to existing accounts

5. **Password Reset**
    - "Forgot password?" link
    - Email-based password reset flow
    - Secure token with expiration

6. **Remember Me**
    - Persistent cookie for extended sessions
    - 14-day expiration
    - Revocable from user settings

7. **Login Activity Log**
    - User-visible login history
    - Show recent login locations and times
    - Alert on suspicious activity

8. **Rate Limiting**
    - Limit login attempts per IP address
    - Prevent brute force attacks
    - CAPTCHA after failed attempts

## Related Use Cases

- **UC-001**: Register Account - New users register before logging in
- **UC-003**: View Competitions - Post-login landing page for users
- **UC-008**: Make Predictions - Requires authenticated session
- **UC-004-UC-007**: Admin functions - Require ADMIN role from login
