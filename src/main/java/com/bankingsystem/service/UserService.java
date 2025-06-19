package com.bankingsystem.service;

import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.User;
import com.bankingsystem.exception.UserNotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.util.RoleUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getUserById(String id) {
        logger.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });
    }

    public List<User> getAllUsers() {
//        List<User> users = userRepository.findAll();
//        logger.info("Fetched {} users from database", users.size());
        return userRepository.findAll();
    }

    public void deleteUser(String userId) {
        logger.info("User with ID: {} deleted successfully", userId);
        User user = getUserById(userId);
        userRepository.delete(user);
    }

    public User updateUser(String userId, User updatedUser, Authentication authentication) {
        User existingUser = getUserById(userId);
        boolean changed = false;

        logger.debug("Updating user: {}", userId);

        // Username uniqueness check and update
        if (updatedUser.getUsername() != null && !updatedUser.getUsername().isBlank()) {
            if (!updatedUser.getUsername().equals(existingUser.getUsername())) {
                if (userRepository.findByUsername(updatedUser.getUsername()).isPresent()) {
                    logger.warn("Username {} already exists", updatedUser.getUsername());
                    throw new IllegalArgumentException("Username is already taken.");
                }
                existingUser.setUsername(updatedUser.getUsername());
                changed = true;
            }
        }

        // Email uniqueness check and update
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isBlank()) {
            if (!updatedUser.getEmail().equals(existingUser.getEmail())) {
                if (userRepository.existsByEmail(updatedUser.getEmail())) {
                    logger.warn("Email {} already in use", updatedUser.getEmail());
                    throw new IllegalArgumentException("Email is already in use.");
                }
                existingUser.setEmail(updatedUser.getEmail());
                changed = true;
            }
        }

        // Role update (only allowed for ADMIN)
        if (updatedUser.getRole() != null) {
            if (!existingUser.getRole().equals(updatedUser.getRole())) {
                // Only allow role change by ADMIN
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                if (!isAdmin) {
                    logger.warn("Unauthorized role update attempt by non-admin user: {}", authentication.getName());
                    throw new SecurityException("Only ADMIN can change user roles.");
                }
                existingUser.setRole(RoleUtils.parseUserRole(updatedUser.getRole().name()));
                changed = true;
            }
        }

        // Password update
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            if (!passwordEncoder.matches(updatedUser.getPassword(), existingUser.getPassword())) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                changed = true;
            }
        }

        if (!changed) {
            logger.info("No changes provided for update on user: {}", userId);
            throw new IllegalArgumentException(
                    "No changes detected. Please provide at least one different value to update.");
        }

        logger.info("User updated: {}", userId);
        return userRepository.save(existingUser);
    }

    public UserResponse getCurrentUserWithAccounts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.isBlank()) {
            logger.warn("No authenticated user found in SecurityContext");
            throw new UserNotFoundException("No authenticated user found.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found for username: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        logger.info("Fetched user details with accounts for: {}", username);
        return mapToUserResponse(user);
    }


    public UserResponse mapToUserResponse(User user) {
        List<Account> accounts = accountRepository.findByUserId(user.getId());
        List<AccountResponse> accountResponses = accounts.stream()
                .map(AccountResponse::fromAccount)
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                accountResponses);
    }
}
