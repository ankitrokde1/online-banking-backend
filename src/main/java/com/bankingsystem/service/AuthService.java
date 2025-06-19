package com.bankingsystem.service;

import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.response.JwtResponse;
import com.bankingsystem.entity.PasswordResetToken;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.enums.UserRole;
import com.bankingsystem.exception.*;
import com.bankingsystem.repository.PasswordResetTokenRepository;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.bankingsystem.util.RoleUtils.parseUserRole;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Registration failed: Username '{}' already taken", request.getUsername());
            throw new UserAlreadyExistsException("Username is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed: Email '{}' already used", request.getEmail());
            throw new UserAlreadyExistsException("Email is already in use.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Only admin can assign roles
//        if (request.getRole().equalsIgnoreCase(String.valueOf(UserRole.ADMIN))) {
//            user.setRole(parseUserRole(request.getRole().toString()));
//        } else {
//            user.setRole(UserRole.CUSTOMER);
//        }
        user.setRole(request.getRole() != null ? parseUserRole(request.getRole()) : UserRole.CUSTOMER);
        userRepository.save(user);
        logger.info("User registered successfully with username: {}", request.getUsername());
    }

    // login method
    public JwtResponse authenticateAndGenerateToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        logger.debug("Generated JWT token for user: {}", user.getUsername());
        return new JwtResponse(token, "Bearer", user.getUsername(), user.getRole());
    }

    public void forgotPassword(String usernameOrEmail) {
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() ->
                {
                    logger.error("Password reset failed: User '{}' not found", usernameOrEmail);
                    return new UserNotFoundException("User not found");
                });

        // Delete previous token
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString(); // This goes in the email
        String hashedToken = passwordEncoder.encode(rawToken); // This goes in DB

        Date expiryDate = new Date(System.currentTimeMillis() + 30 * 60 * 1000); // 30 min

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setHashedToken(hashedToken);
        token.setExpiryDate(expiryDate);

        passwordResetTokenRepository.save(token);

        mailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        logger.info("Password reset token generated and email sent to: {}", user.getEmail());
    }

    public void resetPassword(String rawToken, String newPassword) {
        List<PasswordResetToken> allTokens = passwordResetTokenRepository.findAll(); // No direct match

        PasswordResetToken matchedToken = allTokens.stream()
                .filter(t -> passwordEncoder.matches(rawToken, t.getHashedToken()))
                .findFirst()
                .orElseThrow(() -> {
                    logger.error("Invalid password reset token");
                    return new InvalidTokenException("Invalid or expired token");
                    });

        if (matchedToken.isExpired()) {
            logger.warn("Attempted password reset with expired token");
            throw new ResetTokenExpiredException("Token has expired");
        }

        User user = userRepository.findById(matchedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete token
        passwordResetTokenRepository.delete(matchedToken);
        logger.info("Password reset successful for user: {}", user.getUsername());
    }

}
