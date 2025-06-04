package com.bankingsystem.dto.response;

import com.bankingsystem.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private UserRole role;
    private LocalDateTime createdAt;
    private List<AccountResponse> accounts;
}
