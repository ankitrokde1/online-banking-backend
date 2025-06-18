package com.bankingsystem.controller;

import com.bankingsystem.dto.request.ForgotPasswordRequest;
import com.bankingsystem.dto.request.LoginRequest;
import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.request.ResetPasswordRequest;
import com.bankingsystem.dto.response.JwtResponse;
import com.bankingsystem.exception.InvalidCredentialsException;
import com.bankingsystem.security.JwtTokenProvider;
import com.bankingsystem.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    //done
    @GetMapping("/health-check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application Running...");
    }

    //done
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of ("message", "User registered successfully."));
    }

    //done
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getUsernameOrEmail());
        return ResponseEntity.ok(Map.of("message", "Reset link sent to your email."));
    }

    //done
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    //done
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            JwtResponse jwtResponse = authService.authenticateAndGenerateToken(authentication);

            jwtTokenProvider.addJwtToCookie(response, jwtResponse.getToken());

            return ResponseEntity.ok(jwtResponse); // include token + username + role in response
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }


    //done*
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        jwtTokenProvider.clearJwtCookie(response);
        return ResponseEntity.ok("Logged out successfully");
    }

}

