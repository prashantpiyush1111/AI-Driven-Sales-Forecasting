package com.salesforecast.controller;

import com.salesforecast.dto.request.UpdateRoleRequest;
import com.salesforecast.dto.response.UserResponse;
import com.salesforecast.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.list(pageable));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateRoleRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(userService.updateRole(id, request.getRole(), authentication.getName()));
    }
}
