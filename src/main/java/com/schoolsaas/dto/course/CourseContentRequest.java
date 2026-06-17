package com.schoolsaas.dto.course;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CourseContentRequest {
    private String title;
    private String description;
    private UUID subjectId;
    private UUID classId;
    private List<UUID> targetClassIds;
    private Integer weekNumber;
    private String contentType;
    private List<String> fileUrls;
    private List<String> videoLinks;
    private String thumbnailUrl;
    private String richText;
    private String status;
}
