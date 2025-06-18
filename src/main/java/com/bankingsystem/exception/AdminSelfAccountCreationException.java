package com.bankingsystem.exception;

public class AdminSelfAccountCreationException extends RuntimeException {
    public AdminSelfAccountCreationException(String message) {
        super(message);
    }
}
