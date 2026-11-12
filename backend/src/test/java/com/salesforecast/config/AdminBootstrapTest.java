package com.salesforecast.config;

import com.salesforecast.entity.User;
import com.salesforecast.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrap bootstrap(String profile, String password) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profile);
        return new AdminBootstrap(userRepository, passwordEncoder, env,
                "admin", "admin@salesforecast.com", password);
    }

    private static User seededAdmin() {
        User u = new User();
        u.setUsername("admin");
        u.setPassword(AdminBootstrap.SEEDED_PASSWORD_HASH);
        u.setRole("ADMIN");
        return u;
    }

    @Test
    @DisplayName("ADMIN_PASSWORD set: resets the existing admin's password and keeps role ADMIN")
    void resetsExistingAdmin() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(seededAdmin()));
        when(passwordEncoder.encode("NewStrongPass1")).thenReturn("hashed-new");

        bootstrap("prod", "NewStrongPass1").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-new");
        assertThat(captor.getValue().getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("ADMIN_PASSWORD set and no admin yet: creates one")
    void createsAdminWhenMissing() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NewStrongPass1")).thenReturn("hashed-new");

        bootstrap("dev", "NewStrongPass1").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@salesforecast.com");
        assertThat(captor.getValue().getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("ADMIN_PASSWORD too short -> startup fails")
    void rejectsShortPassword() {
        assertThatThrownBy(() -> bootstrap("dev", "short").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("prod + seeded password still in place + no ADMIN_PASSWORD -> startup fails")
    void prodFailsOnSeededPassword() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(seededAdmin()));

        assertThatThrownBy(() -> bootstrap("prod", "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("dev + seeded password -> only a warning")
    void devOnlyWarns() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(seededAdmin()));

        assertThatCode(() -> bootstrap("dev", "").run(null)).doesNotThrowAnyException();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("prod + password already changed -> starts normally")
    void prodOkWhenPasswordChanged() {
        User changed = seededAdmin();
        changed.setPassword("$2a$10$somethingElse");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(changed));

        assertThatCode(() -> bootstrap("prod", "").run(null)).doesNotThrowAnyException();
    }
}
