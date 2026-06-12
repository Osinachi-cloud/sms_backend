package com.schoolsaas.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolUserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private UUID roleId;
    private String roleName;
    private Boolean isActive;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
}
