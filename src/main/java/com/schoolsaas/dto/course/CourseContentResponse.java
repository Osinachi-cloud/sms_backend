package com.schoolsaas.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseContentResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID subjectId;
    private String subjectName;
    private UUID classId;
    private String className;
    private List<UUID> targetClassIds;
    private Integer weekNumber;
    private String contentType;
    private List<String> fileUrls;
    private List<String> videoLinks;
    private String thumbnailUrl;
    private String richText;
    private String status;
    private UUID teacherId;
    private String teacherName;
    private LocalDateTime createdAt;
}
