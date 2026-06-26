package com.schoolsaas.service;

import com.schoolsaas.model.GradingComponent;
import com.schoolsaas.model.GradingScheme;
import com.schoolsaas.model.Subject;
import com.schoolsaas.repository.GradingSchemeRepository;
import com.schoolsaas.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GradingSchemeService {

    private final GradingSchemeRepository gradingSchemeRepository;
    private final SubjectRepository subjectRepository;

    public List<GradingScheme> getAllSchemes(UUID schoolId) {
        return gradingSchemeRepository.findAllBySchoolId(schoolId);
    }

    @Transactional
    public GradingScheme createScheme(UUID schoolId, GradingScheme scheme) {
        validateWeight(scheme.getComponents());
        scheme.setSchoolId(schoolId);
        
        if (scheme.getComponents() != null) {
            for (GradingComponent component : scheme.getComponents()) {
                component.setScheme(scheme);
            }
        }

        if (Boolean.TRUE.equals(scheme.getIsDefault())) {
            resetOthersDefault(schoolId);
        } else if (!gradingSchemeRepository.existsBySchoolIdAndIsDefaultTrue(schoolId)) {
            scheme.setIsDefault(true);
        }

        return gradingSchemeRepository.save(scheme);
    }

    @Transactional
    public GradingScheme updateScheme(UUID schoolId, UUID schemeId, GradingScheme updatedScheme) {
        GradingScheme existing = gradingSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("Grading scheme not found"));
        
        if (!existing.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }

        validateWeight(updatedScheme.getComponents());

        existing.setName(updatedScheme.getName());
        
        // Update components
        existing.getComponents().clear();
        if (updatedScheme.getComponents() != null) {
            for (GradingComponent comp : updatedScheme.getComponents()) {
                comp.setScheme(existing);
                existing.getComponents().add(comp);
            }
        }

        if (Boolean.TRUE.equals(updatedScheme.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            resetOthersDefault(schoolId);
            existing.setIsDefault(true);
        }

        return gradingSchemeRepository.save(existing);
    }

    @Transactional
    public void deleteScheme(UUID schoolId, UUID schemeId) {
        GradingScheme scheme = gradingSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("Grading scheme not found"));

        if (!scheme.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (subjectRepository.existsByGradingSchemeId(schemeId)) {
            throw new RuntimeException("Cannot delete scheme: It is currently assigned to one or more subjects.");
        }

        if (Boolean.TRUE.equals(scheme.getIsDefault())) {
            throw new RuntimeException("Cannot delete the default scheme. Please mark another scheme as default first.");
        }

        gradingSchemeRepository.delete(scheme);
    }

    @Transactional
    public void setDefault(UUID schoolId, UUID schemeId) {
        GradingScheme scheme = gradingSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("Grading scheme not found"));

        if (!scheme.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }

        resetOthersDefault(schoolId);
        scheme.setIsDefault(true);
        gradingSchemeRepository.save(scheme);
    }

    @Transactional
    public void assignToSubject(UUID schoolId, UUID subjectId, UUID schemeId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        if (!subject.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (schemeId != null) {
            GradingScheme scheme = gradingSchemeRepository.findById(schemeId)
                    .orElseThrow(() -> new RuntimeException("Grading scheme not found"));
            if (!scheme.getSchoolId().equals(schoolId)) {
                throw new RuntimeException("Unauthorized");
            }
            subject.setGradingSchemeId(schemeId);
        } else {
            subject.setGradingSchemeId(null);
        }

        subjectRepository.save(subject);
    }

    public GradingScheme getEffectiveScheme(UUID schoolId, UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        if (subject.getGradingSchemeId() != null) {
            return gradingSchemeRepository.findById(subject.getGradingSchemeId()).orElseGet(() -> getDefaultScheme(schoolId));
        }

        return getDefaultScheme(schoolId);
    }

    private GradingScheme getDefaultScheme(UUID schoolId) {
        return gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(schoolId)
                .orElseThrow(() -> new RuntimeException("No default grading scheme found for the school."));
    }

    private void validateWeight(List<GradingComponent> components) {
        if (components == null || components.isEmpty()) {
            throw new RuntimeException("Grading scheme must have at least one component.");
        }
        int total = components.stream().mapToInt(GradingComponent::getWeight).sum();
        if (total != 100) {
            throw new RuntimeException("Component weights must sum to 100. Current total: " + total);
        }
    }

    private void resetOthersDefault(UUID schoolId) {
        gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(schoolId).ifPresent(s -> {
            s.setIsDefault(false);
            gradingSchemeRepository.save(s);
        });
    }
}
