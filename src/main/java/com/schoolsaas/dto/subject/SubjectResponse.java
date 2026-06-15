package com.schoolsaas.dto.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private Boolean isActive;
    private Boolean isFree;
    private BigDecimal cost;
    private UUID createdBy;
    private String createdByType;
    private String createdByName;
    private List<UUID> classIds;
    private List<String> classNames;
    private Long enrollmentCount;
    private LocalDateTime createdAt;
}
