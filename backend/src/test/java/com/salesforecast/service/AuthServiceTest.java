package com.salesforecast.service;

import com.salesforecast.dto.request.LoginRequest;
import com.salesforecast.dto.request.RegisterRequest;
import com.salesforecast.dto.response.AuthResponse;
import com.salesforecast.entity.User;
import com.salesforecast.repository.UserRepository;
import com.salesforecast.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private static RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .username("  prashant  ")
                .email("  Prashant@Example.COM ")
                .password("secret123")
                .build();
    }

    @Test
    @DisplayName("register: trims username, lowercases email, encodes password, forces USER role")
    void register_success() {
        when(userRepository.existsByUsername("prashant")).thenReturn(false);
        when(userRepository.existsByEmail("prashant@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken("prashant", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("prashant");
        assertThat(saved.getEmail()).isEqualTo("prashant@example.com");
        assertThat(saved.getPassword()).isEqualTo("hashed-secret");
        assertThat(saved.getRole()).isEqualTo("USER");

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("prashant");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("register: duplicate username -> 409 and nothing saved")
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("prashant")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: duplicate email -> 409 and nothing saved")
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByUsername("prashant")).thenReturn(false);
        when(userRepository.existsByEmail("prashant@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login: valid credentials -> token with user's role")
    void login_success() {
        User user = new User();
        user.setUsername("admin");
        user.setRole("ADMIN");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("admin-jwt");

        AuthResponse response = authService.login(
                LoginRequest.builder().username("  admin ").password("admin123").build());

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("admin", "admin123"));
        assertThat(response.getToken()).isEqualTo("admin-jwt");
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("login: wrong password -> BadCredentialsException, no token generated")
    void login_badCredentials_propagates() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder().username("admin").password("wrong").build()))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsername(any());
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login: authenticated but user missing in DB -> BadCredentialsException")
    void login_userMissingAfterAuth_throwsBadCredentials() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder().username("ghost").password("whatever").build()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(jwtUtil, never()).generateToken(any(), any());
    }
}