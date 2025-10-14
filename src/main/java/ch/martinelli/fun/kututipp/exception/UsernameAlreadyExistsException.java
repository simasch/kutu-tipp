package ch.martinelli.fun.kututipp.exception;

/**
 * Exception thrown when attempting to register with a username that already exists.
 */
public class UsernameAlreadyExistsException extends RegistrationException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}
