package com.bankingsystem.controller;

import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.User;
import com.bankingsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserWithAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id, Authentication authentication) {
        User user = userService.getUserById(id);
        String loggedInUsername = authentication.getName();

        if (!user.getUsername().equals(loggedInUsername) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: You can only access your own data.");
        }

        return ResponseEntity.ok(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @Valid @RequestBody User updatedUser, Authentication authentication) {
        User existingUser = userService.getUserById(id);
        String loggedInUsername = authentication.getName();

        if (!existingUser.getUsername().equals(loggedInUsername) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: You can only update your own account.");
        }

        User updated = userService.updateUser(id, updatedUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication authentication) {
        User user = userService.getUserById(id);
        String loggedInUsername = authentication.getName();

        if (!user.getUsername().equals(loggedInUsername) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: You can only delete your own account.");
        }

        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully.");
    }
}

