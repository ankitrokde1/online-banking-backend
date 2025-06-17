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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

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
    public JwtResponse authenticateAndGenerateToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new JwtResponse(token, "Bearer", user.getUsername(), user.getRole());
    }

public void forgotPassword(String usernameOrEmail) {
    User user = userRepository.findByUsername(usernameOrEmail)
            .or(() -> userRepository.findByEmail(usernameOrEmail))
            .orElseThrow(() -> new UserNotFoundException("User not found"));

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
}

    public void resetPassword(String rawToken, String newPassword) {
        List<PasswordResetToken> allTokens = passwordResetTokenRepository.findAll(); // No direct match

        PasswordResetToken matchedToken = allTokens.stream()
                .filter(t -> passwordEncoder.matches(rawToken, t.getHashedToken()))
                .findFirst()
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));

        if (matchedToken.isExpired()) {
            throw new ResetTokenExpiredException("Token has expired");
        }

        User user = userRepository.findById(matchedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete token
        passwordResetTokenRepository.delete(matchedToken);
    }

}
