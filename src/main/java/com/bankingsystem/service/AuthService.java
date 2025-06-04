package com.bankingsystem.service;

import com.bankingsystem.dto.request.LoginRequest;
import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.response.JwtResponse;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.enums.UserRole;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public ResponseEntity<?> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.CUSTOMER);

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    public ResponseEntity<?> login(LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .map(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        String token = jwtTokenProvider.generateToken(user.getUsername(), String.valueOf(user.getRole()));
                        JwtResponse jwtResponse = new JwtResponse(token, "Bearer", user.getUsername(), user.getRole());
                        return ResponseEntity.ok(jwtResponse);
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid credentials");
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: User not found"));
    }
}
