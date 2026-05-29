package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "report_card_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCardTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_config", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> layoutConfig = Map.of();

    @Column(name = "header_text", columnDefinition = "TEXT")
    private String headerText;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @Column(name = "show_logo")
    @Builder.Default
    private Boolean showLogo = true;

    @Column(name = "show_qr_code")
    @Builder.Default
    private Boolean showQrCode = false;

    @Column(name = "show_attendance")
    @Builder.Default
    private Boolean showAttendance = true;

    @Column(name = "show_teacher_comments")
    @Builder.Default
    private Boolean showTeacherComments = true;

    @Column(name = "show_principal_comment")
    @Builder.Default
    private Boolean showPrincipalComment = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grading_scale", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> gradingScale = Map.of();

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
