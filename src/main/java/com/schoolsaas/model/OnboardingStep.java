package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "step_key", nullable = false, unique = true)
    private String stepKey;

    @Column(name = "target_page", nullable = false)
    private String targetPage;

    @Column(name = "target_selector", nullable = false)
    private String targetSelector;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private String position = "bottom";

    @Column(name = "step_order")
    @Builder.Default
    private Integer stepOrder = 0;

    @Column(name = "target_roles")
    private String[] targetRoles;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
