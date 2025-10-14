package ch.martinelli.fun.kututipp.exception;

/**
 * Base exception for registration-related errors.
 */
public class RegistrationException extends RuntimeException {

    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
