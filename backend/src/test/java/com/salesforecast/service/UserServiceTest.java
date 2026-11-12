package com.salesforecast.service;

import com.salesforecast.dto.response.UserResponse;
import com.salesforecast.entity.User;
import com.salesforecast.exception.BadRequestException;
import com.salesforecast.exception.ResourceNotFoundException;
import com.salesforecast.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static User user(String username, String role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setRole(role);
        return u;
    }

    @Test
    @DisplayName("updateRole: promotes a user")
    void promote() {
        User target = user("bob", "USER");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateRole(2L, "ADMIN", "admin");

        assertThat(response.role()).isEqualTo("ADMIN");
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("updateRole: unknown user -> not found")
    void unknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(99L, "ADMIN", "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateRole: cannot change your own role")
    void selfChange() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user("admin", "ADMIN")));

        assertThatThrownBy(() -> userService.updateRole(1L, "USER", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own role");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateRole: cannot demote the last administrator")
    void lastAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user("root", "ADMIN")));
        when(userRepository.countByRole("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateRole(1L, "USER", "someone-else"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last administrator");

        verify(userRepository, never()).save(any());
    }
}
