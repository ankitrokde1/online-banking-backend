package com.bankingsystem.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserRole {
    CUSTOMER,
    ADMIN;



    @JsonCreator
    public static UserRole fromString(String value) {
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for field 'role'. Allowed roles are: CUSTOMER, ADMIN.");
        }
    }
}
