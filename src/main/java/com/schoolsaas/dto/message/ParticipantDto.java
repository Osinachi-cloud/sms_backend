package com.schoolsaas.dto.message;

import lombok.Data;

import java.util.UUID;

@Data
public class ParticipantDto {
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private String role;
}
