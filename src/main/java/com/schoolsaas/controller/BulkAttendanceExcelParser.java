package com.schoolsaas.controller;

import com.schoolsaas.dto.attendance.BulkAttendanceRow;
import com.schoolsaas.exception.BadRequestException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulkAttendanceExcelParser {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
    };

    public static List<BulkAttendanceRow> parse(InputStream inputStream) throws Exception {
        List<BulkAttendanceRow> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new BadRequestException("Excel file is empty");
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                headerIndex.put(getCellString(headerRow.getCell(i)).toLowerCase().trim(), i);
            }

            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    BulkAttendanceRow record = new BulkAttendanceRow();
                    record.setDate(parseDate(getValue(headerIndex, row, "date")));
                    record.setStudentEmail(getValue(headerIndex, row, "student_email"));
                    record.setAdmissionNumber(getValue(headerIndex, row, "admission_number"));
                    record.setStatus(getValue(headerIndex, row, "status"));
                    record.setRemarks(getValue(headerIndex, row, "remarks"));
                    if (record.getDate() != null) {
                        rows.add(record);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return rows;
    }

    private static String getValue(Map<String, Integer> headerIndex, Row row, String column) {
        Integer index = headerIndex.get(column);
        if (index == null || index >= row.getPhysicalNumberOfCells()) return null;
        return getCellString(row.getCell(index));
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
