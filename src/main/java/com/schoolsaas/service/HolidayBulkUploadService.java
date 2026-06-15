package com.schoolsaas.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.schoolsaas.dto.holiday.HolidayBulkPreviewDto;
import com.schoolsaas.model.Holiday;
import com.schoolsaas.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayBulkUploadService {

    private final HolidayRepository holidayRepository;

    /**
     * Parse a file (CSV or Excel) and return a preview of holidays without saving.
     */
    public List<HolidayBulkPreviewDto> previewFromFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (fileName.endsWith(".csv")) {
            return parseCsv(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return parseExcel(file);
        }
        throw new IllegalArgumentException("Unsupported file format. Please upload a CSV or Excel file.");
    }

    @Transactional
    public List<Holiday> saveHolidays(UUID schoolId, List<Holiday> holidays) {
        List<Holiday> saved = new ArrayList<>();
        for (Holiday holiday : holidays) {
            if (holiday.getName() == null || holiday.getName().isBlank() || holiday.getDate() == null) {
                log.warn("Skipping holiday with missing name or date");
                continue;
            }
            if (holidayRepository.existsBySchoolIdAndDate(schoolId, holiday.getDate())) {
                log.info("Holiday already exists for date {}, skipping", holiday.getDate());
                continue;
            }
            holiday.setSchoolId(schoolId);
            saved.add(holidayRepository.save(holiday));
        }
        return saved;
    }

    private List<HolidayBulkPreviewDto> parseExcel(MultipartFile file) throws Exception {
        List<HolidayBulkPreviewDto> results = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;
            for (Row row : sheet) {
                if (rowCount == 0) {
                    rowCount++;
                    continue; // skip header
                }
                if (isRowEmpty(row)) {
                    continue;
                }
                String name = getCellStringValue(row.getCell(0));
                String dateStr = getCellDateStringValue(row.getCell(1));
                String holidayType = getCellStringValue(row.getCell(2));
                String description = getCellStringValue(row.getCell(3));
                results.add(validateRow(rowCount, name, dateStr, holidayType, description));
                rowCount++;
            }
        }
        return results;
    }

    private List<HolidayBulkPreviewDto> parseCsv(MultipartFile file) throws Exception {
        List<HolidayBulkPreviewDto> results = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {

            String[] nextLine;
            int rowCount = 0;
            while ((nextLine = csvReader.readNext()) != null) {
                if (rowCount == 0) {
                    rowCount++;
                    continue; // skip header
                }
                if (nextLine.length == 0 || (nextLine.length == 1 && nextLine[0].trim().isEmpty())) {
                    continue; // skip empty rows
                }
                String name = nextLine.length > 0 ? nextLine[0].trim() : null;
                String dateStr = nextLine.length > 1 ? nextLine[1].trim() : null;
                String holidayType = nextLine.length > 2 ? nextLine[2].trim() : null;
                String description = nextLine.length > 3 ? nextLine[3].trim() : null;
                results.add(validateRow(rowCount, name, dateStr, holidayType, description));
                rowCount++;
            }
        }
        return results;
    }

    private HolidayBulkPreviewDto validateRow(int rowNumber, String name, String dateStr, String holidayType, String description) {
        List<String> errors = new ArrayList<>();
        if (name == null || name.isBlank()) {
            errors.add("Name is required");
        }
        if (dateStr == null || dateStr.isBlank()) {
            errors.add("Date is required");
        } else {
            try {
                LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                errors.add("Invalid date format. Use YYYY-MM-DD");
            }
        }
        if (holidayType != null && !holidayType.isBlank()) {
            String normalized = holidayType.trim().toUpperCase();
            if (!normalized.equals("PUBLIC_HOLIDAY") && !normalized.equals("SCHOOL_EVENT") && !normalized.equals("OTHER")) {
                errors.add("Type must be PUBLIC_HOLIDAY, SCHOOL_EVENT, or OTHER");
            }
        }
        String errorStr = errors.isEmpty() ? null : String.join("; ", errors);
        return HolidayBulkPreviewDto.builder()
                .rowNumber(rowNumber)
                .name(name)
                .date(dateStr)
                .holidayType(holidayType != null && !holidayType.isBlank() ? holidayType.trim().toUpperCase() : "PUBLIC_HOLIDAY")
                .description(description)
                .valid(errors.isEmpty())
                .error(errorStr)
                .build();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 4; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private String getCellDateStringValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        return null;
    }
}
