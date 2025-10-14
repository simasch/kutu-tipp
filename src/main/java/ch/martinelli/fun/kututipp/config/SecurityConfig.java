package ch.martinelli.fun.kututipp.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for the application.
 * <p>
 * Configures BCrypt password encoder with cost factor 10 as per BR-003.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    /**
     * Configures BCrypt password encoder.
     * This provides a good balance between security and performance.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Allow public access to registration page and home page
        http.authorizeHttpRequests(auth ->
                auth.requestMatchers("/register", "/").permitAll()
        );

        super.configure(http);

        // Configure login view - redirects to home page after successful login
        setLoginView(http, "/login", "/");
    }
}
