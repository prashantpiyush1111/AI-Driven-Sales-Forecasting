package com.salesforecast.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    @Test
    @DisplayName("locks the username after max failed attempts")
    void locksAfterMaxFailures() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        service.recordFailure("admin");
        service.recordFailure("admin");
        assertThatCode(() -> service.assertNotLocked("admin")).doesNotThrowAnyException();

        service.recordFailure("admin");
        assertThatThrownBy(() -> service.assertNotLocked("admin"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    @DisplayName("successful login resets the failure counter")
    void successResetsCounter() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        service.recordFailure("admin");
        service.recordFailure("admin");
        service.recordSuccess("admin");
        service.recordFailure("admin");
        service.recordFailure("admin");

        assertThatCode(() -> service.assertNotLocked("admin")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("username matching is case-insensitive and trimmed; other users are unaffected")
    void keyIsNormalisedAndIsolated() {
        LoginAttemptService service = new LoginAttemptService(2, 15);

        service.recordFailure("Admin");
        service.recordFailure("  admin ");

        assertThatThrownBy(() -> service.assertNotLocked("ADMIN"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatCode(() -> service.assertNotLocked("someone-else")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lock expires after the lock duration")
    void lockExpires() {
        LoginAttemptService service = new LoginAttemptService(1, 0); // 0-minute lock

        service.recordFailure("admin");

        assertThatCode(() -> service.assertNotLocked("admin")).doesNotThrowAnyException();
    }
}
