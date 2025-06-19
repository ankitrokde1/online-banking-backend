package com.bankingsystem.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Value("${ratelimit.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${ratelimit.requests:5}")
    private int requestLimit;

    @Value("${ratelimit.duration:60}")
    private int durationInSeconds;

    private static final List<String> RATE_LIMITED_PATHS = List.of(
            "/api/auth",                         // login/register/reset-password
            "/api/accounts/create",             // account requests
            "/api/transactions/transfer",       // transfers
            "/api/transactions/deposit",        // deposits
            "/api/transactions/withdraw",       // withdrawals
            "/api/admin/process/transaction",   // admin transaction process
            "/api/auth/reset-password-request", // forgot password
            "/api/auth/reset-password"          // reset with token
    );

    private Bucket createNewBucket() {
        Refill refill = Refill.greedy(requestLimit, Duration.ofSeconds(durationInSeconds));
        Bandwidth limit = Bandwidth.classic(requestLimit, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, k -> createNewBucket());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitingEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        if (RATE_LIMITED_PATHS.stream().anyMatch(path::startsWith)){
            Bucket bucket = resolveBucket(ip);
            if (!bucket.tryConsume(1)) {
                log.warn("🔒 Rate limit exceeded for IP: {} on endpoint: {}", ip, path);
                response.setStatus(429);
                response.getWriter().write("Too many requests - try again later.");
                return;
            } else {
                String method = request.getMethod();
                log.debug("✅ Allowed request from IP: {} on endpoint: {} [{}]", ip, path, method);
            }
        }

        filterChain.doFilter(request, response);
    }
}
