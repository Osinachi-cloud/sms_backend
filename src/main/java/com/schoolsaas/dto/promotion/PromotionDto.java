package com.schoolsaas.dto.promotion;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PromotionDto {

    @Data
    @Builder
    public static class StudentPromotionInfo {
        private UUID studentId;
        private String studentName;
        private String admissionNumber;
        private UUID currentClassId;
        private String currentClassName;
        private BigDecimal averageScore;
        private String gradeLetter;
        private boolean eligible;
        private UUID nextClassId;
        private String nextClassName;
        private String promotionStatus;
    }

    @Data
    @Builder
    public static class PromotionResult {
        private UUID studentId;
        private String studentName;
        private boolean promoted;
        private String message;
        private UUID newClassId;
        private String newClassName;
    }

    @Data
    @Builder
    public static class BatchPromotionRequest {
        private java.util.List<UUID> studentIds;
    }

    @Data
    @Builder
    public static class BatchPromotionResult {
        private int totalRequested;
        private int promoted;
        private int failed;
        private java.util.List<PromotionResult> results;
    }

    @Data
    @Builder
    public static class PromotionHistoryItem {
        private UUID id;
        private UUID studentId;
        private String studentName;
        private UUID fromClassId;
        private String fromClassName;
        private UUID toClassId;
        private String toClassName;
        private UUID approvedBy;
        private String approvedByName;
        private LocalDateTime promotedAt;
    }
}
