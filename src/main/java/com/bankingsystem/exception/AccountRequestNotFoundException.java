package com.bankingsystem.exception;

public class AccountRequestNotFoundException extends RuntimeException {
    public AccountRequestNotFoundException(String message) {
        super(message);
    }
}
