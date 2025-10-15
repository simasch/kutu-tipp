package ch.martinelli.fun.kututipp.service;

import ch.martinelli.fun.kututipp.db.enums.UserRole;
import ch.martinelli.fun.kututipp.db.tables.records.AppUserRecord;
import ch.martinelli.fun.kututipp.exception.EmailAlreadyExistsException;
import ch.martinelli.fun.kututipp.exception.RegistrationException;
import ch.martinelli.fun.kututipp.exception.UsernameAlreadyExistsException;
import ch.martinelli.fun.kututipp.repository.UserRepository;
import ch.martinelli.fun.kututipp.validation.PasswordValidator;
import ch.martinelli.fun.kututipp.validation.UsernameValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user-related operations.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with the USER role.
     *
     * @param username            the desired username
     * @param email               the email address
     * @param password            the plain text password
     * @param passwordConfirmation the password confirmation
     * @return the created user record
     * @throws RegistrationException if validation fails or user already exists
     */
    public AppUserRecord registerUser(String username, String email, String password, String passwordConfirmation) {
        // Validate all inputs
        validateRegistrationInput(username, email, password, passwordConfirmation);

        // Check for duplicates
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        // Hash the password
        var passwordHash = passwordEncoder.encode(password);

        // Create the user with USER role
        return userRepository.create(username, email, passwordHash, UserRole.USER);
    }

    /**
     * Validates all registration input according to business rules.
     *
     * @param username            the username to validate
     * @param email               the email to validate
     * @param password            the password to validate
     * @param passwordConfirmation the password confirmation
     * @throws RegistrationException if any validation fails
     */
    private void validateRegistrationInput(String username, String email, String password, String passwordConfirmation) {
        // Validate username
        var usernameErrors = UsernameValidator.validate(username);
        if (!usernameErrors.isEmpty()) {
            throw new RegistrationException(String.join(", ", usernameErrors));
        }

        // Validate password
        var passwordErrors = PasswordValidator.validate(password);
        if (!passwordErrors.isEmpty()) {
            throw new RegistrationException(String.join(", ", passwordErrors));
        }

        // Validate password confirmation
        if (passwordConfirmation == null || passwordConfirmation.isEmpty()) {
            throw new RegistrationException("Password confirmation is required");
        }

        if (!password.equals(passwordConfirmation)) {
            throw new RegistrationException("Passwords do not match");
        }
    }

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return the user record if found, null otherwise
     */
    @Transactional(readOnly = true)
    public AppUserRecord findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Finds a user by email.
     *
     * @param email the email to search for
     * @return the user record if found, null otherwise
     */
    @Transactional(readOnly = true)
    public AppUserRecord findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Gets the user ID for the currently authenticated user.
     * This is a convenience method to avoid duplicating user lookup logic in views.
     *
     * @param username the username of the current user
     * @return the user ID
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public Long getCurrentUserId(String username) {
        return userRepository.findByUsername(username)
                .map(AppUserRecord::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
