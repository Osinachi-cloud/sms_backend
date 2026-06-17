package com.schoolsaas.controller;

import com.schoolsaas.dto.holiday.HolidayBulkPreviewDto;
import com.schoolsaas.model.Holiday;
import com.schoolsaas.repository.HolidayRepository;
import com.schoolsaas.service.HolidayBulkUploadService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayRepository holidayRepository;
    private final HolidayBulkUploadService holidayBulkUploadService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Holiday>> getHolidays(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(holidayRepository.findBySchoolId(schoolId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Holiday> createHoliday(@PathVariable UUID schoolId, @RequestBody Holiday holiday) {
        holiday.setSchoolId(schoolId);
        return ResponseEntity.ok(holidayRepository.save(holiday));
    }

    @PutMapping("/{holidayId}")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Holiday> updateHoliday(
            @PathVariable UUID schoolId,
            @PathVariable UUID holidayId,
            @RequestBody Holiday holiday) {
        Holiday existing = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new RuntimeException("Holiday not found"));
        if (!existing.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Holiday not found");
        }
        existing.setName(holiday.getName());
        existing.setDate(holiday.getDate());
        existing.setHolidayType(holiday.getHolidayType());
        existing.setDescription(holiday.getDescription());
        return ResponseEntity.ok(holidayRepository.save(existing));
    }

    @DeleteMapping("/{holidayId}")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable UUID schoolId, @PathVariable UUID holidayId) {
        Holiday existing = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new RuntimeException("Holiday not found"));
        if (!existing.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Holiday not found");
        }
        holidayRepository.delete(existing);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-upload/preview")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<?> previewBulkUpload(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file) {
        try {
            List<HolidayBulkPreviewDto> preview = holidayBulkUploadService.previewFromFile(file);
            int validCount = (int) preview.stream().filter(HolidayBulkPreviewDto::isValid).count();
            int invalidCount = preview.size() - validCount;
            return ResponseEntity.ok(Map.of(
                    "preview", preview,
                    "totalCount", preview.size(),
                    "validCount", validCount,
                    "invalidCount", invalidCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk-upload")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<?> bulkUploadHolidays(
            @PathVariable UUID schoolId,
            @RequestBody List<Holiday> holidays) {
        try {
            List<Holiday> saved = holidayBulkUploadService.saveHolidays(schoolId, holidays);
            return ResponseEntity.ok(Map.of(
                    "message", "Upload successful",
                    "savedCount", saved.size(),
                    "holidays", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/template")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable UUID schoolId) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Holidays");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] columns = {"Name", "Date (YYYY-MM-DD)", "Type", "Description"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("New Year");
            row1.createCell(1).setCellValue("2026-01-01");
            row1.createCell(2).setCellValue("PUBLIC_HOLIDAY");
            row1.createCell(3).setCellValue("New Year celebration");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Mid-term Break");
            row2.createCell(1).setCellValue("2026-03-15");
            row2.createCell(2).setCellValue("SCHOOL_EVENT");
            row2.createCell(3).setCellValue("Mid-term break start");

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=holiday_template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}
