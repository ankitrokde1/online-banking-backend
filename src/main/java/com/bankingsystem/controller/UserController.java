package com.bankingsystem.controller;

import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.User;
import com.bankingsystem.service.UserService;
import com.bankingsystem.util.AuthUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    //done
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<UserResponse> getCurrentUser() {
        logger.info("Fetching current user info for: {}", SecurityContextHolder.getContext().getAuthentication().getName());
        return ResponseEntity.ok(userService.getCurrentUserWithAccounts());
    }

    //done
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @Valid @RequestBody User updatedUser, Authentication authentication) {
        logger.info("Attempting to update user with ID: {}", id);

        User existingUser = userService.getUserById(id);
        
        if (!AuthUtils.isOwnerOrAdmin(existingUser, authentication)) {
            logger.warn("Unauthorized update attempt by: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: You can only access your own data."));
        }

        User updated = userService.updateUser(id, updatedUser, authentication);
        logger.info("User updated successfully: {}", id);
        return ResponseEntity.ok(userService.mapToUserResponse(updated));
    }

    //done
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication authentication) {
        logger.info("Attempting to delete user with ID: {}", id);

        User user = userService.getUserById(id);
        
        if (!AuthUtils.isOwnerOrAdmin(user, authentication)) {
            logger.warn("Unauthorized delete attempt by: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: You can only access your own data."));
        }

        userService.deleteUser(id);
        logger.info("User deleted successfully: {}", id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }
}

