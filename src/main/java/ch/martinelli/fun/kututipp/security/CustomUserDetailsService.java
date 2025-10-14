package ch.martinelli.fun.kututipp.security;

import ch.martinelli.fun.kututipp.db.tables.records.AppUserRecord;
import ch.martinelli.fun.kututipp.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for Spring Security authentication.
 * Loads user details from the database using jOOQ.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user by username for Spring Security authentication.
     * Username matching is case-insensitive as per BR-001.
     *
     * @param username the username to search for
     * @return UserDetails object with user information and authorities
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Trim whitespace as per BR-001
        String trimmedUsername = username.trim();

        // Find user in database (case-insensitive)
        AppUserRecord userRecord = userRepository.findByUsername(trimmedUsername)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + trimmedUsername));

        // Build Spring Security UserDetails
        // Role is prefixed with "ROLE_" as per Spring Security convention
        return User.builder()
                .username(userRecord.getUsername())
                .password(userRecord.getPasswordHash())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + userRecord.getRole().getLiteral())
                ))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
