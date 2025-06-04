package com.bankingsystem.dto.response;

import com.bankingsystem.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private UserRole role;
}
