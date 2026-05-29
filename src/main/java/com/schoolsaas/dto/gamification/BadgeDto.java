package com.schoolsaas.dto.gamification;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BadgeDto {
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private String color;
    private String criteriaType;
    private Integer criteriaValue;
    private Integer pointsValue;
    private LocalDateTime earnedAt;
}
