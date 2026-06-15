package com.schoolsaas.controller;

import com.schoolsaas.dto.parent.ParentDto;
import com.schoolsaas.service.ParentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PostMapping
    public ResponseEntity<ParentDto> createParent(@PathVariable UUID schoolId, @RequestBody ParentDto dto) {
        return ResponseEntity.ok(parentService.createParent(schoolId, dto));
    }

    @GetMapping
    public ResponseEntity<Page<ParentDto>> listParents(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(parentService.listParents(schoolId, pageable));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<Page<ParentDto>> getParentsByStudent(@PathVariable UUID studentId, Pageable pageable) {
        List<ParentDto> list = parentService.getParentsByStudent(studentId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }
}
