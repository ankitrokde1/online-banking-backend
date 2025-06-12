package com.bankingsystem.service;

import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.User;
import com.bankingsystem.exception.UserNotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
    }
    
    public User updateUser(String userId, User updatedUser, Authentication authentication) {
        User existingUser = getUserById(userId);

        boolean changed = false;

        // Username uniqueness check and update
        if (updatedUser.getUsername() != null && !updatedUser.getUsername().isBlank()) {
            if (!updatedUser.getUsername().equals(existingUser.getUsername())) {
                if (userRepository.findByUsername(updatedUser.getUsername()).isPresent()) {
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
                    throw new IllegalArgumentException("Email is already in use.");
                }
                existingUser.setEmail(updatedUser.getEmail());
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

        // Role update (if allowed)
        if (updatedUser.getRole() != null && !updatedUser.getRole().equals(existingUser.getRole())) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (updatedUser.getRole().name().equals("ADMIN") && !isAdmin) {
                throw new IllegalArgumentException("Only admin users can assign ADMIN role.");
            }
            existingUser.setRole(updatedUser.getRole());
            changed = true;
        }

        if (!changed) {
            throw new IllegalArgumentException(
                    "No changes detected. Please provide at least one different value to update.");
        }

        return userRepository.save(existingUser);
    }

    public UserResponse getCurrentUserWithAccounts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.isBlank()) {
            throw new UserNotFoundException("No authenticated user found.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return mapToUserResponse(user); // ✅ reuse the mapper
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
