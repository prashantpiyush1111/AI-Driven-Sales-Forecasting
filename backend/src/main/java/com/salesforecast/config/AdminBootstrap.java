package com.salesforecast.config;

import com.salesforecast.entity.User;
import com.salesforecast.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Manages the initial administrator account.
 *
 * <ul>
 *   <li>If {@code ADMIN_PASSWORD} is set, the admin user is created or its password is reset to it
 *       (BCrypt-hashed). Use this once to replace the insecure seeded password.</li>
 *   <li>If it is not set and the admin still has the password seeded by migration V2, the app
 *       refuses to start in the {@code prod} profile and only logs a warning elsewhere.</li>
 * </ul>
 */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    /** BCrypt hash inserted by migration V2__seed_data.sql (publicly visible in the repo). */
    static final String SEEDED_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String username;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          Environment environment,
                          @Value("${bootstrap.admin.username:admin}") String username,
                          @Value("${bootstrap.admin.email:admin@salesforecast.com}") String email,
                          @Value("${bootstrap.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password != null && !password.isBlank()) {
            applyConfiguredPassword();
            return;
        }
        checkSeededPassword();
    }

    private void applyConfiguredPassword() {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        User admin = userRepository.findByUsername(username).orElseGet(() -> {
            User created = new User();
            created.setUsername(username);
            created.setEmail(email);
            return created;
        });
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole("ADMIN");
        userRepository.save(admin);
        log.info("Administrator '{}' is configured from ADMIN_PASSWORD", username);
    }

    private void checkSeededPassword() {
        boolean stillSeeded = userRepository.findByUsername(username)
                .map(u -> SEEDED_PASSWORD_HASH.equals(u.getPassword()))
                .orElse(false);
        if (!stillSeeded) {
            return;
        }
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "User '" + username + "' still has the default seeded password. "
                            + "Set ADMIN_PASSWORD (min " + MIN_PASSWORD_LENGTH + " chars) to replace it.");
        }
        log.warn("User '{}' still has the default seeded password. Set ADMIN_PASSWORD before deploying.",
                username);
    }
}
