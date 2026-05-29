package com.schoolsaas.controller;

import com.schoolsaas.dto.idcard.IdCardTemplateDto;
import com.schoolsaas.dto.idcard.StudentIdCardDto;
import com.schoolsaas.service.IdCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/id-cards")
@RequiredArgsConstructor
public class IdCardController {

    private final IdCardService idCardService;

    @PostMapping("/templates")
    public ResponseEntity<IdCardTemplateDto> createTemplate(@PathVariable UUID schoolId, @RequestBody IdCardTemplateDto dto) {
        return ResponseEntity.ok(idCardService.createTemplate(schoolId, dto));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<IdCardTemplateDto>> listTemplates(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(idCardService.listTemplates(schoolId));
    }

    @PostMapping("/generate")
    public ResponseEntity<StudentIdCardDto> generateIdCard(@PathVariable UUID schoolId, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(idCardService.generateIdCard(
                schoolId,
                UUID.fromString(body.get("studentId")),
                UUID.fromString(body.get("templateId"))));
    }

    @GetMapping
    public ResponseEntity<Page<StudentIdCardDto>> listIdCards(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(idCardService.listIdCards(schoolId, pageable));
    }
}
