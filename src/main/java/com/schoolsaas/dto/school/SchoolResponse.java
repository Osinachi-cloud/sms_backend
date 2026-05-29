package com.schoolsaas.dto.school;

import com.schoolsaas.model.School;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolResponse {
    private UUID id;
    private String name;
    private String subdomain;
    private String code;
    private String email;
    private String phone;
    private String address;
    private String logoUrl;
    private String status;
    private Map<String, Object> config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SchoolResponse fromEntity(School school) {
        return SchoolResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .subdomain(school.getSubdomain())
                .code(school.getCode())
                .email(school.getEmail())
                .phone(school.getPhone())
                .address(school.getAddress())
                .logoUrl(school.getLogoUrl())
                .status(school.getStatus())
                .config(school.getConfig())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }
}
