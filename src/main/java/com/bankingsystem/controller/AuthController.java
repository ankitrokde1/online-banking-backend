package com.bankingsystem.controller;

import com.bankingsystem.dto.request.ForgotPasswordRequest;
import com.bankingsystem.dto.request.LoginRequest;
import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.request.ResetPasswordRequest;
import com.bankingsystem.dto.response.JwtResponse;
import com.bankingsystem.exception.InvalidCredentialsException;
import com.bankingsystem.security.JwtTokenProvider;
import com.bankingsystem.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {


    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;


    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    //done
    @GetMapping("/health-check")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "message", "Application Running..."
        ));
    }

    //done
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registering user with username: {}", request.getUsername());
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of ("message", "User registered successfully."));
    }

    //done
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        logger.info("Password reset requested for user: {}", request.getUsernameOrEmail());
        authService.forgotPassword(request.getUsernameOrEmail());
        return ResponseEntity.ok(Map.of("message", "Reset link sent to your email."));
    }

    //done
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        logger.info("Resetting password using token");
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    //done
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        logger.info("Login attempt for: {}", request.getUsernameOrEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            JwtResponse jwtResponse = authService.authenticateAndGenerateToken(authentication);

            jwtTokenProvider.addJwtToCookie(response, jwtResponse.getToken());

            logger.info("Login successful for user: {}", jwtResponse.getUsername());
            return ResponseEntity.ok(jwtResponse); // include token + username + role in response
        } catch (BadCredentialsException ex) {
            logger.warn("Login failed: Invalid credentials for {}", request.getUsernameOrEmail());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }


    //done
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtTokenProvider.getJwtFromCookies(request);

        if (token == null || token.isBlank()) {
            logger.warn("Logout attempt with no token present. User likely already logged out.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "User is already logged out."));
        }

        if (!jwtTokenProvider.validateToken(token)) {
            logger.warn("Logout attempt with expired or invalid token.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Session expired or token is invalid. User is already logged out."));
        }

        jwtTokenProvider.clearJwtCookie(response);
        logger.info("User logged out successfully.");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

}

