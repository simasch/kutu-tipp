package ch.martinelli.fun.kututipp.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    @Test
    void shouldReturnErrorForNullPassword() {
        var errors = PasswordValidator.validate(null);

        assertThat(errors)
                .hasSize(1)
                .contains("Password is required");
    }

    @Test
    void shouldReturnErrorForEmptyPassword() {
        var errors = PasswordValidator.validate("");

        assertThat(errors)
                .hasSize(1)
                .contains("Password is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Pass1!",      // 6 characters
            "Pass12!",     // 7 characters
            "a"            // 1 character
    })
    void shouldReturnErrorForTooShortPassword(String password) {
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .contains("Password must be at least 8 characters");
    }

    @Test
    void shouldReturnErrorForMissingUppercase() {
        var password = "password123!"; // no uppercase
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .contains("Password must contain at least one uppercase letter");
    }

    @Test
    void shouldReturnErrorForMissingLowercase() {
        var password = "PASSWORD123!"; // no lowercase
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .contains("Password must contain at least one lowercase letter");
    }

    @Test
    void shouldReturnErrorForMissingDigit() {
        var password = "Password!"; // no digit
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .contains("Password must contain at least one digit");
    }

    @Test
    void shouldReturnErrorForMissingSpecialCharacter() {
        var password = "Password123"; // no special character
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .contains("Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Password1!",          // basic valid password
            "MyP@ssw0rd",          // @ special char
            "Secur3#Pass",         // # special char
            "Test$Pass1",          // $ special char
            "Valid%123Pass",       // % special char
            "Strong^Pass9",        // ^ special char
            "My&Pass123",          // & special char
            "Pass*word1",          // * special char
            "Test(Pass)1",         // () special chars
            "My_Pass123!",         // _ special char
            "Pass+word1",          // + special char
            "Test-Pass1!",         // - special char
            "Pass=word1!",         // = special char
            "My[Pass]1!",          // [] special chars
            "Test{Pass}1!",        // {} special chars
            "Pass|word1!",         // | special char
            "My;Pass1!",           // ; special char
            "Test:Pass1!",         // : special char
            "Pass,word1!",         // , special char
            "My.Pass1!",           // . special char
            "Test<Pass>1!",        // <> special chars
            "Pass?word1!"          // ? special char
    })
    void shouldAcceptValidPasswords(String password) {
        var errors = PasswordValidator.validate(password);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnMultipleErrorsForCompletelyInvalidPassword() {
        var password = "pass"; // too short, no uppercase, no digit, no special char
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .hasSize(4)
                .contains(
                        "Password must be at least 8 characters",
                        "Password must contain at least one uppercase letter",
                        "Password must contain at least one digit",
                        "Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)"
                );
    }

    @Test
    void shouldReturnTrueForValidPassword() {
        assertThat(PasswordValidator.isValid("ValidPass123!")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "short",           // too short, no uppercase, no digit, no special char
            "password123!",    // no uppercase
            "PASSWORD123!",    // no lowercase
            "Password!",       // no digit
            "Password123"      // no special character
    })
    void shouldReturnFalseForInvalidPassword(String password) {
        assertThat(PasswordValidator.isValid(password)).isFalse();
    }

    @Test
    void shouldAcceptPasswordAtExactMinimumLength() {
        var password = "Pass123!";  // exactly 8 characters
        var errors = PasswordValidator.validate(password);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAcceptPasswordWithMultipleSpecialCharacters() {
        var password = "Pass!@#$123";
        var errors = PasswordValidator.validate(password);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAcceptPasswordWithMultipleUppercaseLetters() {
        var password = "PASSWORD123!";
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .doesNotContain("Password must contain at least one uppercase letter");
    }

    @Test
    void shouldAcceptPasswordWithMultipleLowercaseLetters() {
        var password = "password123!";
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .doesNotContain("Password must contain at least one lowercase letter");
    }

    @Test
    void shouldAcceptPasswordWithMultipleDigits() {
        var password = "Pass123456!";
        var errors = PasswordValidator.validate(password);

        assertThat(errors)
                .doesNotContain("Password must contain at least one digit");
    }
}
