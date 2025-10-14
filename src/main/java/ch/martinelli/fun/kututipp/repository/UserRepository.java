package ch.martinelli.fun.kututipp.repository;

import ch.martinelli.fun.kututipp.db.enums.UserRole;
import ch.martinelli.fun.kututipp.db.tables.records.AppUserRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static ch.martinelli.fun.kututipp.db.tables.AppUser.APP_USER;

/**
 * Repository for user database operations using jOOQ.
 */
@Repository
public class UserRepository {

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Checks if a username already exists in the database (case-insensitive).
     *
     * @param username the username to check
     * @return true if the username exists, false otherwise
     */
    public boolean existsByUsername(String username) {
        return dsl.fetchExists(
                dsl.selectFrom(APP_USER)
                        .where(APP_USER.USERNAME.equalIgnoreCase(username))
        );
    }

    /**
     * Checks if an email already exists in the database (case-insensitive).
     *
     * @param email the email to check
     * @return true if the email exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return dsl.fetchExists(
                dsl.selectFrom(APP_USER)
                        .where(APP_USER.EMAIL.equalIgnoreCase(email))
        );
    }

    /**
     * Finds a user by username (case-insensitive).
     *
     * @param username the username to search for
     * @return an Optional containing the user record if found, empty otherwise
     */
    public Optional<AppUserRecord> findByUsername(String username) {
        return dsl.selectFrom(APP_USER)
                .where(APP_USER.USERNAME.equalIgnoreCase(username))
                .fetchOptional();
    }

    /**
     * Finds a user by email (case-insensitive).
     *
     * @param email the email to search for
     * @return an Optional containing the user record if found, empty otherwise
     */
    public Optional<AppUserRecord> findByEmail(String email) {
        return dsl.selectFrom(APP_USER)
                .where(APP_USER.EMAIL.equalIgnoreCase(email))
                .fetchOptional();
    }

    /**
     * Finds a user by ID.
     *
     * @param id the user ID
     * @return an Optional containing the user record if found, empty otherwise
     */
    public Optional<AppUserRecord> findById(Long id) {
        return dsl.selectFrom(APP_USER)
                .where(APP_USER.ID.eq(id))
                .fetchOptional();
    }

    /**
     * Creates a new user in the database.
     *
     * @param username     the username
     * @param email        the email (will be stored in lowercase)
     * @param passwordHash the BCrypt hashed password
     * @param role         the user role
     * @return the created user record with generated ID
     */
    public AppUserRecord create(String username, String email, String passwordHash, UserRole role) {
        OffsetDateTime now = OffsetDateTime.now();

        AppUserRecord record = dsl.newRecord(APP_USER);
        record.setUsername(username);
        record.setEmail(email.toLowerCase());
        record.setPasswordHash(passwordHash);
        record.setRole(role);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        record.store();

        return record;
    }
}
