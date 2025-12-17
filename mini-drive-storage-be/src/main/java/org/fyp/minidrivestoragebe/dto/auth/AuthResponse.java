package org.fyp.minidrivestoragebe.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type;
    private String userId;
    private String email;
    private String fullName;
    private Long storageUsed;
    private Long storageQuota;

    public static AuthResponse of(String token, String userId, String email, String fullName, 
                                   Long storageUsed, Long storageQuota) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(userId)
                .email(email)
                .fullName(fullName)
                .storageUsed(storageUsed)
                .storageQuota(storageQuota)
                .build();
    }
}
