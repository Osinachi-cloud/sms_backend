package com.schoolsaas.service;

import com.schoolsaas.model.TeacherActivityLog;
import com.schoolsaas.repository.TeacherActivityLogRepository;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherActivityLogService {

    private final TeacherActivityLogRepository activityLogRepository;

    @Transactional
    public void logActivity(UUID schoolId, UUID teacherId, String activityType, String description, String entityType, UUID entityId, Map<String, Object> metadata) {
        TeacherActivityLog log = TeacherActivityLog.builder()
                .schoolId(schoolId)
                .teacherId(teacherId)
                .userId(SecurityUtils.getCurrentUserId())
                .activityType(activityType)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();
        activityLogRepository.save(log);
    }

    public Page<TeacherActivityLog> getTeacherActivities(UUID schoolId, UUID teacherId, Pageable pageable) {
        return activityLogRepository.findBySchoolIdAndTeacherIdOrderByCreatedAtDesc(schoolId, teacherId, pageable);
    }

    public Page<TeacherActivityLog> getSchoolActivities(UUID schoolId, Pageable pageable) {
        return activityLogRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable);
    }
}
