package com.bankingsystem.util;


import java.security.SecureRandom;

public class AccountNumberGenerator {

    private static final String PREFIX = "ACC";
    private static final int NUMBER_LENGTH = 10;

    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < NUMBER_LENGTH; i++) {
            sb.append(random.nextInt(10)); // Append 0-9 digits
        }
        return sb.toString();
    }
}

