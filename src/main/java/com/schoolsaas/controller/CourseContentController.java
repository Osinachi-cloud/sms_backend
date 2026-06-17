package com.schoolsaas.controller;

import com.schoolsaas.dto.course.CourseContentRequest;
import com.schoolsaas.dto.course.CourseContentResponse;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.repository.TeacherRepository;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.CourseContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/course-contents")
@RequiredArgsConstructor
public class CourseContentController {

    private final CourseContentService courseContentService;
    private final TeacherRepository teacherRepository;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<CourseContentResponse>> getAllContents(@PathVariable UUID schoolId, Pageable pageable) {
        List<CourseContentResponse> list = courseContentService.getAllContents(schoolId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<CourseContentResponse>> getContentsBySubject(
            @PathVariable UUID schoolId,
            @PathVariable UUID subjectId,
            @RequestParam(required = false) UUID studentId,
            Pageable pageable) {
        List<CourseContentResponse> list = courseContentService.getContentsBySubject(schoolId, subjectId, studentId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<CourseContentResponse>> getContentsByClass(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @RequestParam(required = false) UUID studentId,
            Pageable pageable) {
        List<CourseContentResponse> list = courseContentService.getContentsByClass(schoolId, classId, studentId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<CourseContentResponse>> getContentsByTeacher(
            @PathVariable UUID schoolId,
            @PathVariable UUID teacherId,
            Pageable pageable) {
        List<CourseContentResponse> list = courseContentService.getContentsByTeacher(schoolId, teacherId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/{contentId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<CourseContentResponse> getContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId,
            @RequestParam(required = false) UUID studentId) {
        return ResponseEntity.ok(courseContentService.getContent(schoolId, contentId, studentId));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<CourseContentResponse> createContent(
            @PathVariable UUID schoolId,
            @RequestBody CourseContentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Optional<Teacher> teacher = teacherRepository.findBySchoolIdAndUserId(schoolId, userId);
        UUID teacherId = teacher.map(Teacher::getId).orElse(null);
        return ResponseEntity.ok(courseContentService.createContent(schoolId, teacherId, request));
    }

    @PutMapping("/{contentId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<CourseContentResponse> updateContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId,
            @RequestBody CourseContentRequest request) {
        boolean isAdmin = SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("cms.content.edit.any");
        return ResponseEntity.ok(courseContentService.updateContent(schoolId, contentId, request, isAdmin));
    }

    @DeleteMapping("/{contentId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.delete') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId) {
        boolean isAdmin = SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("cms.content.delete.any");
        courseContentService.deleteContent(schoolId, contentId, isAdmin);
        return ResponseEntity.ok().build();
    }
}
