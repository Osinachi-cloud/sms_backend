package com.schoolsaas.dto.teacher;

import lombok.Data;

import java.util.UUID;

@Data
public class TeacherSubjectAssignmentRequest {
    private UUID teacherId;
    private UUID classId;
    private UUID subjectId;
    private Boolean isClassTeacher;
    private UUID sessionId;
}
