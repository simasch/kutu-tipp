package ch.martinelli.fun.kututipp.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for username according to business rules BR-001.
 * <p>
 * Username requirements:
 * - Length: 3-30 characters
 * - Characters: Alphanumeric characters, underscore, and hyphen allowed
 * - Pattern: Must start with a letter or number
 * - Reserved: Cannot use reserved system names
 */
public class UsernameValidator {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 30;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]*$");
    private static final Set<String> RESERVED_NAMES = Set.of(
            "admin", "administrator", "system", "root", "superuser",
            "moderator", "support", "help", "service", "bot"
    );

    /**
     * Validates a username and returns a list of error messages.
     * If the list is empty, the username is valid.
     *
     * @param username the username to validate
     * @return list of error messages, empty if valid
     */
    public static List<String> validate(String username) {
        List<String> errors = new ArrayList<>();

        if (username == null || username.isEmpty()) {
            errors.add("Username is required");
            return errors;
        }

        if (username.length() < MIN_LENGTH) {
            errors.add("Username must be at least " + MIN_LENGTH + " characters");
        }

        if (username.length() > MAX_LENGTH) {
            errors.add("Username must not exceed " + MAX_LENGTH + " characters");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            errors.add("Username must start with a letter or number and contain only letters, numbers, underscore, and hyphen");
        }

        if (RESERVED_NAMES.contains(username.toLowerCase())) {
            errors.add("This username is reserved and cannot be used");
        }

        return errors;
    }

    /**
     * Checks if a username is valid.
     *
     * @param username the username to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String username) {
        return validate(username).isEmpty();
    }
}
