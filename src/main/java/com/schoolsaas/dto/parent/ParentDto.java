package com.schoolsaas.dto.parent;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ParentDto {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String occupation;
    private String relationship;
    private Boolean isActive;
    private List<ParentStudentInfo> students;
}
