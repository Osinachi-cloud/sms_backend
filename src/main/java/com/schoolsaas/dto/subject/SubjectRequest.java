package com.schoolsaas.dto.subject;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SubjectRequest {
    @NotBlank(message = "Subject name is required")
    private String name;
    private String code;
    private String description;
    private Boolean isFree;
    private BigDecimal cost;
    private String createdByType;
    private List<java.util.UUID> classIds;
}
