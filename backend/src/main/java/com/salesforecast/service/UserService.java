package com.salesforecast.service;

import com.salesforecast.dto.response.UserResponse;
import com.salesforecast.entity.User;
import com.salesforecast.exception.BadRequestException;
import com.salesforecast.exception.ResourceNotFoundException;
import com.salesforecast.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse updateRole(Long id, String newRole, String actingUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.getUsername().equals(actingUsername)) {
            throw new BadRequestException("You cannot change your own role");
        }
        if (ADMIN_ROLE.equals(user.getRole()) && !ADMIN_ROLE.equals(newRole)
                && userRepository.countByRole(ADMIN_ROLE) <= 1) {
            throw new BadRequestException("Cannot remove the last administrator");
        }

        user.setRole(newRole);
        return UserResponse.from(userRepository.save(user));
    }
}
