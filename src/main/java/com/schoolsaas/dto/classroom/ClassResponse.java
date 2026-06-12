package com.schoolsaas.dto.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ClassResponse {
    private UUID id;
    private String name;
    private Integer gradeLevel;
    private String section;
    private Integer capacity;
    private Integer studentCount;
    private UUID classTeacherId;
    private String classTeacherName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
