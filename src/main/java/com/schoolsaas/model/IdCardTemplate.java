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
@Table(name = "id_card_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdCardTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> layoutConfig = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "front_design", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> frontDesign = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "back_design", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> backDesign = Map.of();

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
