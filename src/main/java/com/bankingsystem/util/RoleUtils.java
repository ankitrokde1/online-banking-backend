package com.bankingsystem.util;

import com.bankingsystem.entity.enums.UserRole;
import com.bankingsystem.exception.InvalidUserRoleException;

public class RoleUtils {

    public static UserRole parseUserRole(String input) {
        try {
            return UserRole.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserRoleException("Invalid role: " + input + ". Allowed roles: CUSTOMER, ADMIN.");
        }
    }
}

