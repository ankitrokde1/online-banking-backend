package com.bankingsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    private String id;

    private String hashedToken;

    private String userId;

//    @Indexed(name = "expiry_index", expireAfterSeconds = 0)
    private Date expiryDate;

    public boolean isExpired() {
        return expiryDate.before(new Date());
    }

}
