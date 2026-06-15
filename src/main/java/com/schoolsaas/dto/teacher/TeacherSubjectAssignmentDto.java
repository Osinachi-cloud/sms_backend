package com.schoolsaas.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSubjectAssignmentDto {
    private UUID id;
    private UUID teacherId;
    private String teacherName;
    private UUID classId;
    private String className;
    private UUID subjectId;
    private String subjectName;
    private Boolean isClassTeacher;
    private UUID sessionId;
    private String sessionName;
}
