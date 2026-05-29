package com.schoolsaas.dto.idcard;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
public class IdCardTemplateDto {
    private UUID id;
    private String name;
    private Map<String, Object> layoutConfig;
    private Map<String, Object> frontDesign;
    private Map<String, Object> backDesign;
    private Boolean isDefault;
}
