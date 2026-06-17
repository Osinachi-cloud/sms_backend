package com.schoolsaas.service;

import com.schoolsaas.dto.course.CourseContentRequest;
import com.schoolsaas.dto.course.CourseContentResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseContentService {

    private final CourseContentRepository courseContentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CourseContentResponse> getContentsBySubject(UUID schoolId, UUID subjectId, UUID studentId) {
        List<CourseContent> contents = courseContentRepository.findPublishedBySubject(schoolId, subjectId);
        if (studentId != null) {
            contents = filterVisibleToStudent(schoolId, contents, studentId);
        }
        return contents.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseContentResponse> getContentsByClass(UUID schoolId, UUID classId, UUID studentId) {
        List<CourseContent> contents = courseContentRepository.findPublishedByClass(schoolId, classId);
        if (studentId != null) {
            contents = filterVisibleToStudent(schoolId, contents, studentId);
        }
        return contents.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseContentResponse> getContentsByTeacher(UUID schoolId, UUID teacherId) {
        return courseContentRepository.findBySchoolIdAndTeacherIdOrderByCreatedAtDesc(schoolId, teacherId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseContentResponse> getAllContents(UUID schoolId) {
        return courseContentRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseContentResponse getContent(UUID schoolId, UUID contentId, UUID studentId) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseContent", "id", contentId));
        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("CourseContent", "id", contentId);
        }
        if (studentId != null && !isContentVisibleToStudent(schoolId, content, studentId)) {
            throw new BadRequestException("You do not have access to this content");
        }
        return mapToResponse(content);
    }

    @Transactional
    public CourseContentResponse createContent(UUID schoolId, UUID teacherId, CourseContentRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Title is required");
        }
        CourseContent content = CourseContent.builder()
                .schoolId(schoolId)
                .teacherId(teacherId)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .subjectId(request.getSubjectId())
                .classId(request.getClassId())
                .targetClassIds(request.getTargetClassIds() != null ? request.getTargetClassIds() : (request.getClassId() != null ? java.util.List.of(request.getClassId()) : java.util.List.of()))
                .weekNumber(request.getWeekNumber())
                .contentType(request.getContentType())
                .fileUrls(request.getFileUrls())
                .videoLinks(request.getVideoLinks())
                .thumbnailUrl(request.getThumbnailUrl())
                .richText(request.getRichText())
                .status(request.getStatus() != null ? request.getStatus() : "PUBLISHED")
                .build();
        content = courseContentRepository.save(content);

        // Send notification to enrolled students
        if ("PUBLISHED".equals(content.getStatus()) && request.getSubjectId() != null) {
            notifyEnrolledStudents(schoolId, content);
        }

        return mapToResponse(content);
    }

    @Transactional
    public CourseContentResponse updateContent(UUID schoolId, UUID contentId, CourseContentRequest request, boolean isAdmin) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseContent", "id", contentId));
        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("CourseContent", "id", contentId);
        }
        // Non-admins can only edit their own content
        if (!isAdmin && content.getTeacherId() != null) {
            UUID userId = SecurityUtils.getCurrentUserId();
            Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
            UUID teacherId = teacher != null ? teacher.getId() : null;
            if (teacherId == null || !teacherId.equals(content.getTeacherId())) {
                throw new com.schoolsaas.exception.BadRequestException("You can only edit content you created");
            }
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            content.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            content.setDescription(request.getDescription());
        }
        if (request.getSubjectId() != null) {
            content.setSubjectId(request.getSubjectId());
        }
        if (request.getClassId() != null) {
            content.setClassId(request.getClassId());
        }
        if (request.getTargetClassIds() != null) {
            content.setTargetClassIds(request.getTargetClassIds());
        }
        if (request.getWeekNumber() != null) {
            content.setWeekNumber(request.getWeekNumber());
        }
        if (request.getContentType() != null) {
            content.setContentType(request.getContentType());
        }
        if (request.getFileUrls() != null) {
            content.setFileUrls(request.getFileUrls());
        }
        if (request.getVideoLinks() != null) {
            content.setVideoLinks(request.getVideoLinks());
        }
        if (request.getThumbnailUrl() != null) {
            content.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getRichText() != null) {
            content.setRichText(request.getRichText());
        }
        if (request.getStatus() != null) {
            content.setStatus(request.getStatus());
        }
        content = courseContentRepository.save(content);
        return mapToResponse(content);
    }

    @Transactional
    public void deleteContent(UUID schoolId, UUID contentId, boolean isAdmin) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseContent", "id", contentId));
        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("CourseContent", "id", contentId);
        }
        if (!isAdmin && content.getTeacherId() != null) {
            UUID userId = SecurityUtils.getCurrentUserId();
            Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
            UUID teacherId = teacher != null ? teacher.getId() : null;
            if (teacherId == null || !teacherId.equals(content.getTeacherId())) {
                throw new com.schoolsaas.exception.BadRequestException("You can only delete content you created");
            }
        }
        courseContentRepository.delete(content);
    }

    private List<CourseContent> filterVisibleToStudent(UUID schoolId, List<CourseContent> contents, UUID studentId) {
        return contents.stream()
                .filter(c -> isContentVisibleToStudent(schoolId, c, studentId))
                .collect(Collectors.toList());
    }

    private boolean isContentVisibleToStudent(UUID schoolId, CourseContent content, UUID studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null || !student.getSchoolId().equals(schoolId)) {
            return false;
        }
        // If targetClassIds is specified, student must be in one of them
        List<UUID> targets = content.getTargetClassIds();
        if (targets != null && !targets.isEmpty()) {
            if (!targets.contains(student.getClassId())) {
                return false;
            }
        } else {
            // Fallback to legacy classId
            if (content.getClassId() != null && !content.getClassId().equals(student.getClassId())) {
                return false;
            }
        }
        // Student must be enrolled in the subject
        if (content.getSubjectId() != null) {
            return enrollmentRepository.existsBySchoolIdAndStudentIdAndSubjectId(schoolId, studentId, content.getSubjectId());
        }
        return true;
    }

    private void notifyEnrolledStudents(UUID schoolId, CourseContent content) {
        try {
            Subject subject = content.getSubjectId() != null ? subjectRepository.findById(content.getSubjectId()).orElse(null) : null;
            String subjectName = subject != null ? subject.getName() : "a subject";
            notificationService.notifySubjectContentUploaded(schoolId, content.getSubjectId(), content.getTitle(), subjectName);
        } catch (Exception e) {
            log.warn("Failed to send content upload notification: {}", e.getMessage());
        }
    }

    private CourseContentResponse mapToResponse(CourseContent content) {
        String subjectName = null;
        if (content.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(content.getSubjectId()).orElse(null);
            subjectName = subject != null ? subject.getName() : null;
        }
        String className = null;
        if (content.getClassId() != null) {
            SchoolClass sc = classRepository.findById(content.getClassId()).orElse(null);
            className = sc != null ? sc.getName() : null;
        }
        String teacherName = null;
        if (content.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(content.getTeacherId()).orElse(null);
            teacherName = teacher != null ? teacher.getFullName() : null;
        }
        return CourseContentResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .description(content.getDescription())
                .subjectId(content.getSubjectId())
                .subjectName(subjectName)
                .classId(content.getClassId())
                .className(className)
                .targetClassIds(content.getTargetClassIds())
                .weekNumber(content.getWeekNumber())
                .contentType(content.getContentType())
                .fileUrls(content.getFileUrls())
                .videoLinks(content.getVideoLinks())
                .thumbnailUrl(content.getThumbnailUrl())
                .richText(content.getRichText())
                .status(content.getStatus())
                .teacherId(content.getTeacherId())
                .teacherName(teacherName)
                .createdAt(content.getCreatedAt())
                .build();
    }
}
