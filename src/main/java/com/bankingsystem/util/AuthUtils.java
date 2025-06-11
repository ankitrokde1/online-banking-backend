package com.bankingsystem.util;
import com.bankingsystem.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class AuthUtils {

    public static boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    public static boolean isCustomer(Authentication authentication) {
        return hasRole(authentication, "ROLE_CUSTOMER");
    }

    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals(role));
    }

    public static boolean isOwnerOrAdmin(User entityUser, Authentication auth) {
        boolean isAdmin = isAdmin(auth); // your existing check
        return isAdmin || entityUser.getUsername().equals(auth.getName());
    }
}

