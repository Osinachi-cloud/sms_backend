package com.schoolsaas.dto.library;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class LibraryBookDto {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String edition;
    private String description;
    private String coverImageUrl;
    private String fileUrl;
    private String fileType;
    private Integer totalCopies;
    private Integer availableCopies;
    private Boolean isDigital;
    private String[] tags;
    private String[] audienceRoles;
    private Map<String, Object> metadata;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
