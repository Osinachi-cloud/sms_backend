package com.schoolsaas.controller;

import com.schoolsaas.dto.reportcard.ReportCardDto;
import com.schoolsaas.service.ReportCardPdfService;
import com.schoolsaas.service.ReportCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/report-cards")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final ReportCardPdfService pdfService;

    @PostMapping
    public ResponseEntity<ReportCardDto> generateReportCard(@PathVariable UUID schoolId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(reportCardService.generateReportCard(
                schoolId,
                UUID.fromString(body.get("studentId")),
                UUID.fromString(body.get("termId")),
                UUID.fromString(body.get("templateId"))));
    }

    @PostMapping("/{reportCardId}/publish")
    public ResponseEntity<ReportCardDto> publishReportCard(@PathVariable UUID reportCardId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(reportCardService.publishReportCard(reportCardId, body.get("teacherComment"), body.get("principalComment")));
    }

    @PostMapping("/{reportCardId}/email")
    public ResponseEntity<Map<String, String>> emailReportCard(
            @PathVariable UUID schoolId,
            @PathVariable UUID reportCardId) {
        reportCardService.emailReportCard(schoolId, reportCardId);
        return ResponseEntity.ok(Map.of("message", "Report card emailed to parent"));
    }

    @PostMapping("/bulk-email")
    public ResponseEntity<Map<String, String>> bulkEmailReportCards(
            @PathVariable UUID schoolId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> classIdStrings = (List<String>) body.get("classIds");
        String termIdStr = (String) body.get("termId");
        List<UUID> classIds = classIdStrings != null
                ? classIdStrings.stream().map(UUID::fromString).toList()
                : List.of();
        UUID termId = termIdStr != null ? UUID.fromString(termIdStr) : null;
        int count = reportCardService.bulkEmailReportCards(schoolId, classIds, termId);
        return ResponseEntity.ok(Map.of(
                "message", "Bulk email job started",
                "count", String.valueOf(count)
        ));
    }

    @GetMapping
    public ResponseEntity<Page<ReportCardDto>> listReportCards(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(reportCardService.listReportCards(schoolId, pageable));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<Page<ReportCardDto>> getStudentReportCards(@PathVariable UUID studentId, Pageable pageable) {
        List<ReportCardDto> list = reportCardService.getStudentReportCards(studentId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/student/{studentId}/report")
    public ResponseEntity<Map<String, Object>> getStudentReport(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID termId) {
        return ResponseEntity.ok(reportCardService.getStudentReport(schoolId, studentId, termId));
    }

    @GetMapping("/student/{studentId}/pdf")
    public ResponseEntity<byte[]> getStudentReportPdf(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID termId) throws Exception {
        Map<String, Object> report = reportCardService.getStudentReport(schoolId, studentId, termId);
        byte[] pdf = pdfService.generateReportCardPdf(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-card.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
