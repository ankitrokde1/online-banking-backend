package com.bankingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class LoginRequest {

    @NotBlank(message = "Username or email is mandatory")
    private String usernameOrEmail;

    @NotBlank(message = "Password is mandatory")
    private String password;
}

