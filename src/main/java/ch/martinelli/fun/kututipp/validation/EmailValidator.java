package ch.martinelli.fun.kututipp.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for email according to business rules BR-002.
 * <p>
 * Email requirements:
 * - Format: Must be a valid email format (RFC 5322 simplified)
 * - Length: Maximum 255 characters
 */
public class EmailValidator {

    private static final int MAX_LENGTH = 255;
    // Simplified RFC 5322 email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    /**
     * Validates an email and returns a list of error messages.
     * If the list is empty, the email is valid.
     *
     * @param email the email to validate
     * @return list of error messages, empty if valid
     */
    public static List<String> validate(String email) {
        List<String> errors = new ArrayList<>();

        if (email == null || email.isEmpty()) {
            errors.add("Email is required");
            return errors;
        }

        if (email.length() > MAX_LENGTH) {
            errors.add("Email must not exceed " + MAX_LENGTH + " characters");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Email must be a valid email address");
        }

        return errors;
    }

    /**
     * Checks if an email is valid.
     *
     * @param email the email to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String email) {
        return validate(email).isEmpty();
    }
}
