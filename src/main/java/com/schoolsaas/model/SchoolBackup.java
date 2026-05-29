package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "school_backups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "school_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> schoolData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "students_data", columnDefinition = "jsonb")
    private Map<String, Object> studentsData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teachers_data", columnDefinition = "jsonb")
    private Map<String, Object> teachersData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classes_data", columnDefinition = "jsonb")
    private Map<String, Object> classesData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_data", columnDefinition = "jsonb")
    private Map<String, Object> contentData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payments_data", columnDefinition = "jsonb")
    private Map<String, Object> paymentsData;

    @Column(name = "deletion_request_id")
    private UUID deletionRequestId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
