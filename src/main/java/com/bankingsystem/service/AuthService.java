package com.bankingsystem.service;

import com.bankingsystem.dto.request.LoginRequest;
import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.response.JwtResponse;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.enums.UserRole;
import com.bankingsystem.exception.InvalidCredentialsException;
import com.bankingsystem.exception.InvalidUserRoleException;
import com.bankingsystem.exception.UserAlreadyExistsException;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? parseUserRole(String.valueOf(request.getRole())) : UserRole.CUSTOMER);
        userRepository.save(user);
    }

    // login method

    public JwtResponse authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username/email or password."));
    
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username/email or password.");
        }
    
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new JwtResponse(token, "Bearer", user.getUsername(), user.getRole());
    }

    public UserRole parseUserRole(String input) {
        try {
            return UserRole.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserRoleException("Invalid role: " + input + ". Allowed roles: CUSTOMER, ADMIN.");
        }
    }
}
