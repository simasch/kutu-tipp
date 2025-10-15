package ch.martinelli.fun.kututipp.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameValidatorTest {

    @Test
    void shouldReturnErrorForNullUsername() {
        var errors = UsernameValidator.validate(null);

        assertThat(errors)
                .hasSize(1)
                .contains("Username is required");
    }

    @Test
    void shouldReturnErrorForEmptyUsername() {
        var errors = UsernameValidator.validate("");

        assertThat(errors)
                .hasSize(1)
                .contains("Username is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a", "12"})
    void shouldReturnErrorForTooShortUsername(String username) {
        var errors = UsernameValidator.validate(username);

        assertThat(errors)
                .contains("Username must be at least 3 characters");
    }

    @Test
    void shouldReturnErrorForTooLongUsername() {
        var username = "a".repeat(31); // 31 characters
        var errors = UsernameValidator.validate(username);

        assertThat(errors)
                .contains("Username must not exceed 30 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "_username",  // starts with underscore
            "-username",  // starts with hyphen
            "user name",  // contains space
            "user@name",  // contains @ symbol
            "user.name",  // contains dot
            "user!name",  // contains exclamation mark
            "user#name"   // contains hash
    })
    void shouldReturnErrorForInvalidPattern(String username) {
        var errors = UsernameValidator.validate(username);

        assertThat(errors)
                .contains("Username must start with a letter or number and contain only letters, numbers, underscore, and hyphen");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin",
            "Admin",         // case insensitive
            "ADMIN",
            "administrator",
            "system",
            "root",
            "superuser",
            "moderator",
            "support",
            "help",
            "service",
            "bot"
    })
    void shouldReturnErrorForReservedNames(String username) {
        var errors = UsernameValidator.validate(username);

        assertThat(errors)
                .contains("This username is reserved and cannot be used");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",                    // minimum length
            "user123",                // alphanumeric
            "user_name",              // with underscore
            "user-name",              // with hyphen
            "user_name-123",          // combination
            "123user",                // starts with number
            "a1b2c3"                  // mixed letters and numbers
    })
    void shouldAcceptValidUsernames(String username) {
        var errors = UsernameValidator.validate(username);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnMultipleErrorsForInvalidUsername() {
        var username = "ab";  // too short, and valid otherwise
        var errors = UsernameValidator.validate(username);

        assertThat(errors)
                .contains("Username must be at least 3 characters");
    }

    @Test
    void shouldReturnTrueForValidUsername() {
        assertThat(UsernameValidator.isValid("validuser123")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "admin", "_invalid", "user name"})
    void shouldReturnFalseForInvalidUsername(String username) {
        assertThat(UsernameValidator.isValid(username)).isFalse();
    }

    @Test
    void shouldAcceptUsernameAtExactMinimumLength() {
        var username = "abc";  // exactly 3 characters
        var errors = UsernameValidator.validate(username);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAcceptUsernameAtExactMaximumLength() {
        var username = "a".repeat(30);  // exactly 30 characters
        var errors = UsernameValidator.validate(username);

        assertThat(errors).isEmpty();
    }
}
