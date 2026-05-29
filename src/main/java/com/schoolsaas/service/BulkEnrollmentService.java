package com.schoolsaas.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.schoolsaas.dto.student.CreateStudentRequest;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.BulkEnrollmentJob;
import com.schoolsaas.model.School;
import com.schoolsaas.model.Student;
import com.schoolsaas.repository.BulkEnrollmentJobRepository;
import com.schoolsaas.repository.ClassRepository;
import com.schoolsaas.repository.SchoolRepository;
import com.schoolsaas.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkEnrollmentService {

    private final BulkEnrollmentJobRepository jobRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;

    public record PreviewResult(
            List<String> headers,
            List<List<String>> rows,
            int totalRows
    ) {}

    public record ColumnMapping(
            String csvColumn,
            String dbField,
            boolean required
    ) {}

    public record ProcessingResult(
            UUID jobId,
            int totalRows,
            int successfulRows,
            int failedRows,
            List<String> errors
    ) {}

    @Transactional(readOnly = true)
    public PreviewResult previewFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BadRequestException("File name is required");
        }

        List<String> headers;
        List<List<String>> previewRows = new ArrayList<>();
        int totalRows = 0;

        if (filename.endsWith(".csv")) {
            try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                List<String[]> allRows = reader.readAll();
                if (allRows.isEmpty()) {
                    throw new BadRequestException("File is empty");
                }
                headers = Arrays.asList(allRows.get(0));
                totalRows = allRows.size() - 1;

                for (int i = 1; i <= Math.min(5, allRows.size() - 1); i++) {
                    previewRows.add(Arrays.asList(allRows.get(i)));
                }
            } catch (CsvException e) {
                throw new BadRequestException("Invalid CSV format: " + e.getMessage());
            }
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet.getPhysicalNumberOfRows() == 0) {
                    throw new BadRequestException("File is empty");
                }

                Row headerRow = sheet.getRow(0);
                headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(getCellValueAsString(cell));
                }

                totalRows = sheet.getPhysicalNumberOfRows() - 1;

                for (int i = 1; i <= Math.min(5, sheet.getPhysicalNumberOfRows() - 1); i++) {
                    Row row = sheet.getRow(i);
                    List<String> rowData = new ArrayList<>();
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.getCell(j);
                        rowData.add(cell != null ? getCellValueAsString(cell) : "");
                    }
                    previewRows.add(rowData);
                }
            }
        } else {
            throw new BadRequestException("Unsupported file format. Use CSV or Excel (.xlsx)");
        }

        return new PreviewResult(headers, previewRows, totalRows);
    }

    @Transactional
    public BulkEnrollmentJob createJob(UUID schoolId, UUID userId, String fileName, Map<String, String> columnMapping) {
        BulkEnrollmentJob job = BulkEnrollmentJob.builder()
                .schoolId(schoolId)
                .createdBy(userId)
                .fileName(fileName)
                .columnMapping(columnMapping)
                .status("PENDING")
                .build();

        return jobRepository.save(job);
    }

    @Async
    @Transactional
    public void processJobAsync(UUID jobId, MultipartFile file) {
        BulkEnrollmentJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        job.setStatus("PROCESSING");
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        List<String> errors = new ArrayList<>();
        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(0);

        try {
            String filename = file.getOriginalFilename();
            Map<String, String> mapping = job.getColumnMapping();

            if (filename != null && filename.endsWith(".csv")) {
                processCSV(file.getInputStream(), job.getSchoolId(), mapping, errors, successful, failed, total);
            } else {
                processExcel(file.getInputStream(), job.getSchoolId(), mapping, errors, successful, failed, total);
            }

            job.setStatus("COMPLETED");
        } catch (Exception e) {
            log.error("Bulk enrollment failed for job {}", jobId, e);
            job.setStatus("FAILED");
            errors.add("Processing error: " + e.getMessage());
        }

        job.setTotalRows(total.get());
        job.setSuccessfulRows(successful.get());
        job.setFailedRows(failed.get());
        job.setErrorLog(String.join("\n", errors));
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);

        log.info("Bulk enrollment job {} completed: {} successful, {} failed out of {} total",
                jobId, successful.get(), failed.get(), total.get());
    }

    private void processCSV(InputStream inputStream, UUID schoolId, Map<String, String> mapping,
                           List<String> errors, AtomicInteger successful, AtomicInteger failed, AtomicInteger total)
            throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.size() <= 1) return;

            String[] headers = allRows.get(0);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i], i);
            }

            for (int i = 1; i < allRows.size(); i++) {
                total.incrementAndGet();
                try {
                    String[] row = allRows.get(i);
                    processRow(schoolId, mapping, headerIndex, row, i + 1);
                    successful.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
    }

    private void processExcel(InputStream inputStream, UUID schoolId, Map<String, String> mapping,
                             List<String> errors, AtomicInteger successful, AtomicInteger failed, AtomicInteger total)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() <= 1) return;

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                headerIndex.put(getCellValueAsString(headerRow.getCell(i)), i);
            }

            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                total.incrementAndGet();
                try {
                    Row row = sheet.getRow(i);
                    String[] rowData = new String[headerIndex.size()];
                    for (int j = 0; j < headerIndex.size(); j++) {
                        Cell cell = row.getCell(j);
                        rowData[j] = cell != null ? getCellValueAsString(cell) : "";
                    }
                    processRow(schoolId, mapping, headerIndex, rowData, i + 1);
                    successful.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
    }

    private void processRow(UUID schoolId, Map<String, String> mapping, Map<String, Integer> headerIndex,
                           String[] row, int rowNumber) {
        String fullName = getMappedValue(mapping, headerIndex, row, "full_name");
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Full name is required");
        }

        String email = getMappedValue(mapping, headerIndex, row, "email");
        if (email != null && !email.isBlank() && studentRepository.existsBySchoolIdAndEmail(schoolId, email)) {
            throw new BadRequestException("Email already exists: " + email);
        }

        String admissionNumber = getMappedValue(mapping, headerIndex, row, "admission_number");
        if (admissionNumber == null || admissionNumber.isBlank()) {
            admissionNumber = generateAdmissionNumber(schoolId);
        } else if (studentRepository.existsBySchoolIdAndAdmissionNumber(schoolId, admissionNumber)) {
            throw new BadRequestException("Admission number already exists: " + admissionNumber);
        }

        String classIdStr = getMappedValue(mapping, headerIndex, row, "class_id");
        UUID classId = null;
        if (classIdStr != null && !classIdStr.isBlank()) {
            try {
                classId = UUID.fromString(classIdStr);
                if (!classRepository.existsById(classId)) {
                    throw new BadRequestException("Class not found: " + classIdStr);
                }
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid class ID: " + classIdStr);
            }
        }

        Student student = Student.builder()
                .schoolId(schoolId)
                .admissionNumber(admissionNumber)
                .fullName(fullName)
                .email(email)
                .phone(getMappedValue(mapping, headerIndex, row, "phone"))
                .gender(getMappedValue(mapping, headerIndex, row, "gender"))
                .address(getMappedValue(mapping, headerIndex, row, "address"))
                .classId(classId)
                .parentName(getMappedValue(mapping, headerIndex, row, "parent_name"))
                .parentEmail(getMappedValue(mapping, headerIndex, row, "parent_email"))
                .parentPhone(getMappedValue(mapping, headerIndex, row, "parent_phone"))
                .admissionDate(LocalDate.now())
                .status("ACTIVE")
                .build();

        studentRepository.save(student);
    }

    private String getMappedValue(Map<String, String> mapping, Map<String, Integer> headerIndex,
                                  String[] row, String dbField) {
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (entry.getValue().equals(dbField)) {
                Integer index = headerIndex.get(entry.getKey());
                if (index != null && index < row.length) {
                    return row[index];
                }
            }
        }
        return null;
    }

    private String generateAdmissionNumber(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        String prefix = school.getCode() + "/" + Year.now().getValue() + "/";
        Integer maxSeq = studentRepository.findMaxAdmissionNumberSequence(schoolId, prefix);
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;

        return prefix + String.format("%04d", nextSeq);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    @Transactional(readOnly = true)
    public Page<BulkEnrollmentJob> getJobs(UUID schoolId, Pageable pageable) {
        return jobRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable);
    }

    @Transactional(readOnly = true)
    public BulkEnrollmentJob getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));
    }

    public List<Map<String, Object>> getAvailableFields() {
        return List.of(
                Map.of("key", "full_name", "label", "Full Name", "required", true),
                Map.of("key", "email", "label", "Email", "required", false),
                Map.of("key", "phone", "label", "Phone", "required", false),
                Map.of("key", "gender", "label", "Gender", "required", false),
                Map.of("key", "address", "label", "Address", "required", false),
                Map.of("key", "admission_number", "label", "Admission Number", "required", false),
                Map.of("key", "class_id", "label", "Class ID", "required", false),
                Map.of("key", "parent_name", "label", "Parent Name", "required", false),
                Map.of("key", "parent_email", "label", "Parent Email", "required", false),
                Map.of("key", "parent_phone", "label", "Parent Phone", "required", false)
        );
    }
}
