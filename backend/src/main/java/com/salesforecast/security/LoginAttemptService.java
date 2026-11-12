package com.salesforecast.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection for login. After {@code maxAttempts} consecutive
 * failures a username is locked for {@code lockDuration}.
 *
 * <p>State is per application instance. If the backend is scaled to several replicas,
 * move this to Redis (or enforce limits at the gateway) so counters are shared.
 */
@Service
public class LoginAttemptService {

    private static final class Attempt {
        int failures;
        Instant lockedUntil;
    }

    private final int maxAttempts;
    private final Duration lockDuration;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${security.login.max-attempts:5}") int maxAttempts,
                               @Value("${security.login.lock-minutes:15}") long lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    /** Throws 429 if the username is currently locked. */
    public void assertNotLocked(String username) {
        String key = key(username);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        synchronized (attempt) {
            if (attempt.lockedUntil != null) {
                if (Instant.now().isBefore(attempt.lockedUntil)) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Too many failed login attempts. Try again later.");
                }
                // Lock expired: start fresh.
                attempts.remove(key, attempt);
            }
        }
    }

    public void recordFailure(String username) {
        Attempt attempt = attempts.computeIfAbsent(key(username), k -> new Attempt());
        synchronized (attempt) {
            attempt.failures++;
            if (attempt.failures >= maxAttempts) {
                attempt.lockedUntil = Instant.now().plus(lockDuration);
            }
        }
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
