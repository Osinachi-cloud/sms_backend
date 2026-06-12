package com.schoolsaas.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.schoolsaas.dto.parent.ParentDto;
import com.schoolsaas.dto.student.CreateStudentRequest;
import com.schoolsaas.dto.teacher.CreateTeacherRequest;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.BulkEnrollmentJob;
import com.schoolsaas.model.School;
import com.schoolsaas.repository.BulkEnrollmentJobRepository;
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
import java.math.BigDecimal;
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
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final ParentService parentService;

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
    public void processJobAsync(UUID jobId, byte[] fileBytes, String fileName) {
        BulkEnrollmentJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        // Save status immediately (no outer @Transactional, so this commits now)
        job.setStatus("PROCESSING");
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        List<String> errors = new ArrayList<>();
        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(0);

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            Map<String, String> mapping = job.getColumnMapping();
            String entityType = mapping.getOrDefault("__entityType", "students");

            if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                processCSV(inputStream, job.getSchoolId(), mapping, entityType, errors, successful, failed, total);
            } else {
                processExcel(inputStream, job.getSchoolId(), mapping, entityType, errors, successful, failed, total);
            }

            if (failed.get() > 0 && successful.get() == 0) {
                job.setStatus("FAILED");
            } else {
                job.setStatus("COMPLETED");
            }
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

    private void processCSV(InputStream inputStream, UUID schoolId, Map<String, String> mapping, String entityType,
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
                    processRow(schoolId, entityType, mapping, headerIndex, row, i + 1);
                    successful.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
    }

    private void processExcel(InputStream inputStream, UUID schoolId, Map<String, String> mapping, String entityType,
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
                    processRow(schoolId, entityType, mapping, headerIndex, rowData, i + 1);
                    successful.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
    }

    private void processRow(UUID schoolId, String entityType, Map<String, String> mapping, Map<String, Integer> headerIndex,
                           String[] row, int rowNumber) {
        switch (entityType) {
            case "teachers" -> processTeacherRow(schoolId, mapping, headerIndex, row);
            case "parents" -> processParentRow(schoolId, mapping, headerIndex, row);
            default -> processStudentRow(schoolId, mapping, headerIndex, row);
        }
    }

    private void processStudentRow(UUID schoolId, Map<String, String> mapping, Map<String, Integer> headerIndex, String[] row) {
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
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid class ID: " + classIdStr);
            }
        }

        String gender = getMappedValue(mapping, headerIndex, row, "gender");
        if (gender != null && !gender.isBlank()) {
            gender = gender.toUpperCase();
        }

        CreateStudentRequest request = new CreateStudentRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPhone(normalizePhone(getMappedValue(mapping, headerIndex, row, "phone")));
        request.setGender(gender);
        request.setAddress(getMappedValue(mapping, headerIndex, row, "address"));
        request.setClassId(classId);
        request.setAdmissionNumber(admissionNumber);
        request.setPassword(getMappedValue(mapping, headerIndex, row, "password"));

        // Build nested parent payload from bulk columns (supports both old and new key names)
        String parentName = getMappedValueWithAliases(mapping, headerIndex, row, "parent_full_name", "parent_name");
        String parentEmail = getMappedValue(mapping, headerIndex, row, "parent_email");
        String parentPhone = normalizePhone(getMappedValue(mapping, headerIndex, row, "parent_phone"));
        String parentRelationship = getMappedValue(mapping, headerIndex, row, "parent_relationship");
        String parentAddress = getMappedValue(mapping, headerIndex, row, "parent_address");
        String parentOccupation = getMappedValue(mapping, headerIndex, row, "parent_occupation");
        String parentPassword = getMappedValue(mapping, headerIndex, row, "parent_password");

        if (parentName != null && !parentName.isBlank()) {
            CreateStudentRequest.ParentPayload parentPayload = new CreateStudentRequest.ParentPayload();
            parentPayload.setFullName(parentName);
            parentPayload.setEmail(parentEmail);
            parentPayload.setPhone(parentPhone);
            parentPayload.setRelationship(parentRelationship);
            parentPayload.setAddress(parentAddress);
            parentPayload.setOccupation(parentOccupation);
            parentPayload.setPassword(parentPassword);
            request.setParent(parentPayload);
        } else {
            // Fallback to flat parent fields for backward compatibility
            request.setParentName(parentName);
            request.setParentEmail(parentEmail);
            request.setParentPhone(parentPhone);
        }

        studentService.createStudent(schoolId, request);
    }

    private void processTeacherRow(UUID schoolId, Map<String, String> mapping, Map<String, Integer> headerIndex, String[] row) {
        String fullName = getMappedValue(mapping, headerIndex, row, "full_name");
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Full name is required");
        }

        CreateTeacherRequest request = new CreateTeacherRequest();
        request.setFullName(fullName);
        request.setEmail(getMappedValue(mapping, headerIndex, row, "email"));
        request.setPhone(normalizePhone(getMappedValue(mapping, headerIndex, row, "phone")));
        request.setEmployeeId(getMappedValue(mapping, headerIndex, row, "employee_id"));
        request.setSpecialization(getMappedValue(mapping, headerIndex, row, "specialization"));
        request.setQualification(getMappedValue(mapping, headerIndex, row, "qualification"));
        request.setPassword(getMappedValue(mapping, headerIndex, row, "password"));

        String dateOfJoining = getMappedValue(mapping, headerIndex, row, "date_of_joining");
        if (dateOfJoining != null && !dateOfJoining.isBlank()) {
            request.setDateOfJoining(LocalDate.parse(dateOfJoining));
        }

        teacherService.createTeacher(schoolId, request);
    }

    private void processParentRow(UUID schoolId, Map<String, String> mapping, Map<String, Integer> headerIndex, String[] row) {
        String fullName = getMappedValue(mapping, headerIndex, row, "full_name");
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Full name is required");
        }

        ParentDto dto = new ParentDto();
        dto.setFullName(fullName);
        dto.setEmail(getMappedValue(mapping, headerIndex, row, "email"));
        dto.setPhone(getMappedValue(mapping, headerIndex, row, "phone"));
        dto.setAddress(getMappedValue(mapping, headerIndex, row, "address"));
        dto.setOccupation(getMappedValue(mapping, headerIndex, row, "occupation"));
        dto.setRelationship(getMappedValue(mapping, headerIndex, row, "relationship"));

        parentService.createParent(schoolId, dto);
    }

    private String getMappedValueWithAliases(Map<String, String> mapping, Map<String, Integer> headerIndex,
                                              String[] row, String... dbFields) {
        for (String dbField : dbFields) {
            String value = getMappedValue(mapping, headerIndex, row, dbField);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizePhone(String value) {
        if (value == null || value.isBlank()) return null;
        // Excel exports large numbers (e.g. phone numbers) in scientific notation.
        // Convert 2.34809E+12 -> 2348090000000
        String upper = value.toUpperCase().replace(" ", "");
        if (upper.contains("E+") || upper.contains("E-")) {
            try {
                return new BigDecimal(value).toPlainString();
            } catch (NumberFormatException e) {
                return value.trim();
            }
        }
        return value.trim();
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
                Map.of("key", "date_of_birth", "label", "Date of Birth", "required", false),
                Map.of("key", "address", "label", "Address", "required", false),
                Map.of("key", "admission_number", "label", "Admission Number", "required", false),
                Map.of("key", "class_id", "label", "Class ID", "required", false),
                Map.of("key", "password", "label", "Password", "required", false),
                Map.of("key", "parent_full_name", "label", "Parent Full Name", "required", false),
                Map.of("key", "parent_email", "label", "Parent Email", "required", false),
                Map.of("key", "parent_phone", "label", "Parent Phone", "required", false),
                Map.of("key", "parent_relationship", "label", "Parent Relationship", "required", false),
                Map.of("key", "parent_address", "label", "Parent Address", "required", false),
                Map.of("key", "parent_occupation", "label", "Parent Occupation", "required", false),
                Map.of("key", "parent_password", "label", "Parent Password", "required", false)
        );
    }
}
