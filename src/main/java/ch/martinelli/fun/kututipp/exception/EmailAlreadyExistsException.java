package ch.martinelli.fun.kututipp.exception;

/**
 * Exception thrown when attempting to register with an email that already exists.
 */
public class EmailAlreadyExistsException extends RegistrationException {

    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
    }
}
