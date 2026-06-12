package com.schoolsaas.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ClassRequest {
    @NotBlank(message = "Class name is required")
    private String name;

    private Integer gradeLevel;
    private String section;
    private Integer capacity;
    private UUID classTeacherId;
}
