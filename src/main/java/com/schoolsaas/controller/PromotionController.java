package com.schoolsaas.controller;

import com.schoolsaas.dto.promotion.PromotionDto;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/classes/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PromotionDto.StudentPromotionInfo>> getEligibleStudents(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            Pageable pageable) {
        UUID teacherUserId = SecurityUtils.getCurrentUserId();
        List<PromotionDto.StudentPromotionInfo> list = promotionService.getEligibleStudents(schoolId, classId, teacherUserId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @PostMapping("/students/{studentId}")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.read') or hasPermission(#schoolId, 'class.read')")
    public ResponseEntity<PromotionDto.PromotionResult> promoteStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "false") boolean force) {
        UUID teacherUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(promotionService.promoteStudent(schoolId, studentId, teacherUserId, force));
    }

    @PostMapping("/classes/{classId}/batch")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.read') or hasPermission(#schoolId, 'class.read')")
    public ResponseEntity<PromotionDto.BatchPromotionResult> promoteBatch(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @RequestBody PromotionDto.BatchPromotionRequest request) {
        UUID teacherUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(promotionService.promoteBatch(schoolId, classId, teacherUserId, request));
    }
}
