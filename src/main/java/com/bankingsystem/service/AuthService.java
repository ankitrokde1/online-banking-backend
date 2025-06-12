package com.bankingsystem.service;

import com.bankingsystem.dto.request.LoginRequest;
import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.response.JwtResponse;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.enums.UserRole;
import com.bankingsystem.exception.EmailSendFailedException;
import com.bankingsystem.exception.InvalidCredentialsException;
import com.bankingsystem.exception.ResetTokenInvalidException;
import com.bankingsystem.exception.UserAlreadyExistsException;
import com.bankingsystem.exception.UserNotFoundException;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import static com.bankingsystem.util.RoleUtils.parseUserRole;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;

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
        user.setRole(request.getRole() != null ? parseUserRole(request.getRole().toString()) : UserRole.CUSTOMER);
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

    public void forgotPassword(String usernameOrEmail) {
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name(), Duration.ofMinutes(15));

        try {
            mailService.sendPasswordResetEmail(user.getEmail(), token);
        } catch (Exception ex) {
            throw new EmailSendFailedException("Failed to send reset email", ex);
         }
    }

    public void resetPassword(String token, String newPassword) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResetTokenInvalidException("Invalid or expired token");
        }
        String username = jwtTokenProvider.getUsername(token);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
       
    }

}
