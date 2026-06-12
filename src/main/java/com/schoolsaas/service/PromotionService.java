package com.schoolsaas.service;

import com.schoolsaas.dto.promotion.PromotionDto;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final GradeRepository gradeRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    private static final BigDecimal PASSING_THRESHOLD = new BigDecimal("40");

    @Transactional(readOnly = true)
    public List<PromotionDto.StudentPromotionInfo> getEligibleStudents(UUID schoolId, UUID classId, UUID teacherUserId) {
        // Verify teacher has access to this class
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> new BadRequestException("Teacher profile not found"));

        SchoolClass currentClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        if (!currentClass.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Class", "id", classId);
        }

        List<Student> students = studentRepository.findActiveBySchoolIdAndClassId(schoolId, classId);
        if (students.isEmpty()) {
            return List.of();
        }

        // Find next class
        UUID nextClassId = findNextClassId(schoolId, currentClass);
        String nextClassName = null;
        if (nextClassId != null) {
            nextClassName = classRepository.findById(nextClassId)
                    .map(SchoolClass::getName)
                    .orElse(null);
        }

        // Get current term
        Optional<Term> currentTermOpt = termRepository.findBySchoolIdAndIsCurrentTrue(schoolId);
        UUID currentTermId = currentTermOpt.map(Term::getId).orElse(null);

        final UUID effectiveNextClassId = nextClassId;
        final String effectiveNextClassName = nextClassName;

        return students.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .map(student -> {
                    BigDecimal avg = calculateAverageScore(student.getId(), currentTermId);
                    boolean eligible = avg != null && avg.compareTo(PASSING_THRESHOLD) >= 0;

                    return PromotionDto.StudentPromotionInfo.builder()
                            .studentId(student.getId())
                            .studentName(student.getFullName())
                            .admissionNumber(student.getAdmissionNumber())
                            .currentClassId(classId)
                            .currentClassName(currentClass.getName())
                            .averageScore(avg)
                            .gradeLetter(calculateGradeLetter(avg))
                            .eligible(eligible)
                            .nextClassId(effectiveNextClassId)
                            .nextClassName(effectiveNextClassName)
                            .promotionStatus(effectiveNextClassId == null ? "GRADUATED" : "PENDING")
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public PromotionDto.PromotionResult promoteStudent(UUID schoolId, UUID studentId, UUID teacherUserId, boolean force) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        if (student.getClassId() == null) {
            throw new BadRequestException("Student is not assigned to any class");
        }

        SchoolClass currentClass = classRepository.findById(student.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", student.getClassId()));

        UUID nextClassId = findNextClassId(schoolId, currentClass);
        if (nextClassId == null) {
            return PromotionDto.PromotionResult.builder()
                    .studentId(studentId)
                    .studentName(student.getFullName())
                    .promoted(false)
                    .message("Student is in the highest class. No next class available for promotion.")
                    .build();
        }

        // Check eligibility unless forced
        if (!force) {
            Optional<Term> currentTermOpt = termRepository.findBySchoolIdAndIsCurrentTrue(schoolId);
            UUID currentTermId = currentTermOpt.map(Term::getId).orElse(null);
            BigDecimal avg = calculateAverageScore(studentId, currentTermId);
            if (avg == null || avg.compareTo(PASSING_THRESHOLD) < 0) {
                return PromotionDto.PromotionResult.builder()
                        .studentId(studentId)
                        .studentName(student.getFullName())
                        .promoted(false)
                        .message("Student average (" + (avg != null ? avg : "N/A") + ") is below passing threshold. Use force=true to override.")
                        .build();
            }
        }

        SchoolClass nextClass = classRepository.findById(nextClassId).orElse(null);
        String nextClassName = nextClass != null ? nextClass.getName() : "Unknown";

        // Perform promotion
        student.setClassId(nextClassId);
        studentRepository.save(student);

        log.info("Student {} promoted from {} to {} by teacher {}",
                studentId, currentClass.getName(), nextClassName, teacherUserId);

        return PromotionDto.PromotionResult.builder()
                .studentId(studentId)
                .studentName(student.getFullName())
                .promoted(true)
                .message("Successfully promoted to " + nextClassName)
                .newClassId(nextClassId)
                .newClassName(nextClassName)
                .build();
    }

    @Transactional
    public PromotionDto.BatchPromotionResult promoteBatch(UUID schoolId, UUID classId, UUID teacherUserId, PromotionDto.BatchPromotionRequest request) {
        List<PromotionDto.PromotionResult> results = new ArrayList<>();
        int promoted = 0;
        int failed = 0;

        for (UUID studentId : request.getStudentIds()) {
            try {
                PromotionDto.PromotionResult result = promoteStudent(schoolId, studentId, teacherUserId, false);
                results.add(result);
                if (result.isPromoted()) promoted++;
                else failed++;
            } catch (Exception e) {
                failed++;
                results.add(PromotionDto.PromotionResult.builder()
                        .studentId(studentId)
                        .promoted(false)
                        .message("Error: " + e.getMessage())
                        .build());
            }
        }

        return PromotionDto.BatchPromotionResult.builder()
                .totalRequested(request.getStudentIds().size())
                .promoted(promoted)
                .failed(failed)
                .results(results)
                .build();
    }

    private UUID findNextClassId(UUID schoolId, SchoolClass currentClass) {
        if (currentClass.getGradeLevel() == null) {
            return null;
        }
        int nextGrade = currentClass.getGradeLevel() + 1;
        List<SchoolClass> candidates = classRepository.findBySchoolIdAndGradeLevel(schoolId, nextGrade);
        if (candidates.isEmpty()) {
            return null;
        }
        // Prefer same section
        return candidates.stream()
                .filter(c -> currentClass.getSection() != null && currentClass.getSection().equals(c.getSection()))
                .findFirst()
                .orElse(candidates.get(0))
                .getId();
    }

    private BigDecimal calculateAverageScore(UUID studentId, UUID termId) {
        List<Grade> grades;
        if (termId != null) {
            grades = gradeRepository.findByStudentIdAndTermId(studentId, termId);
        } else {
            grades = gradeRepository.findByStudentIdOrderByTermIdDescSubjectIdAsc(studentId);
        }
        if (grades == null || grades.isEmpty()) {
            return null;
        }
        // Average only the most recent grade per subject (in case of multiple assessments)
        Map<UUID, Grade> latestBySubject = new LinkedHashMap<>();
        for (Grade g : grades) {
            latestBySubject.putIfAbsent(g.getSubjectId(), g);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Grade g : latestBySubject.values()) {
            sum = sum.add(g.getScore() != null ? g.getScore() : BigDecimal.ZERO);
        }
        return sum.divide(new BigDecimal(latestBySubject.size()), 2, RoundingMode.HALF_UP);
    }

    private String calculateGradeLetter(BigDecimal average) {
        if (average == null) return "N/A";
        int score = average.intValue();
        if (score >= 70) return "A";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        if (score >= 40) return "D";
        return "F";
    }
}
