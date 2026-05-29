package com.schoolsaas.dto.gamification;

import lombok.Data;

import java.util.List;

@Data
public class LeaderboardEntryDto {
    private String userId;
    private String fullName;
    private String avatarUrl;
    private Integer totalPoints;
    private Integer badgesCount;
    private Integer rank;
}
