package com.schoolsaas.controller;

import com.opencsv.CSVReader;
import com.schoolsaas.dto.attendance.BulkAttendanceRow;
import com.schoolsaas.exception.BadRequestException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BulkAttendanceCsvParser {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,        // 2026-06-15
        DateTimeFormatter.ofPattern("dd/MM/yyyy"), // 15/06/2026
        DateTimeFormatter.ofPattern("MM/dd/yyyy"), // 06/15/2026
        DateTimeFormatter.ofPattern("dd-MM-yyyy"), // 15-06-2026
        DateTimeFormatter.ofPattern("dd.MM.yyyy"), // 15.06.2026
    };

    public static List<BulkAttendanceRow> parse(InputStream inputStream) throws Exception {
        List<BulkAttendanceRow> rows = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new BadRequestException("CSV file is empty");
            }

            Map<String, Integer> headerIndex = new java.util.HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            String[] line;
            int rowNum = 2;
            while ((line = reader.readNext()) != null) {
                try {
                    BulkAttendanceRow row = new BulkAttendanceRow();
                    row.setDate(parseDate(getValue(headerIndex, line, "date")));
                    row.setStudentEmail(getValue(headerIndex, line, "student_email"));
                    row.setAdmissionNumber(getValue(headerIndex, line, "admission_number"));
                    row.setStatus(getValue(headerIndex, line, "status"));
                    row.setRemarks(getValue(headerIndex, line, "remarks"));
                    if (row.getDate() != null) {
                        rows.add(row);
                    }
                } catch (Exception e) {
                    // Log and skip invalid rows
                    rowNum++;
                    continue;
                }
                rowNum++;
            }
        }

        return rows;
    }

    private static String getValue(Map<String, Integer> headerIndex, String[] row, String column) {
        Integer index = headerIndex.get(column);
        if (index == null || index >= row.length) return null;
        String value = row[index];
        return value != null ? value.trim() : null;
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
