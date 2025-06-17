package com.bankingsystem.entity;

import com.bankingsystem.entity.enums.AccountType;
import com.bankingsystem.entity.enums.RequestStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "account_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequest {

    @Id
    private String id;

    private String userId;
    private AccountType accountType;
    private LocalDateTime requestedAt;
    
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;
}
