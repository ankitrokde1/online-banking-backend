package com.bankingsystem.controller;

import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.User;
import com.bankingsystem.service.UserService;
import com.bankingsystem.util.AuthUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    //done
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserWithAccounts());
    }

    //done*
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @Valid @RequestBody User updatedUser, Authentication authentication) {
        User existingUser = userService.getUserById(id);
        
        if (!AuthUtils.isOwnerOrAdmin(existingUser, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: You can only access your own data."));
        }

        User updated = userService.updateUser(id, updatedUser, authentication);
        return ResponseEntity.ok(userService.mapToUserResponse(updated)); 
    }

    //done
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication authentication) {
        User user = userService.getUserById(id);
        
        if (!AuthUtils.isOwnerOrAdmin(user, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: You can only access your own data."));
        }

        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }
}

