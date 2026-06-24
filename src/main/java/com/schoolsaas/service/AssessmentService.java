package com.schoolsaas.service;

import com.schoolsaas.dto.assessment.*;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final TeacherAssessmentRepository assessmentRepo;
    private final AssessmentScoreRepository scoreRepo;
    private final GradingSchemeEntryRepository gradingRepo;
    private final StudentRepository studentRepo;
    private final ClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final TermRepository termRepo;
    private final TeacherRepository teacherRepo;
    private final QuizRepository quizRepo;

    private UUID currentTeacherId(UUID schoolId) {
        UserPrincipal user = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        if (SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin()) return null;
        Teacher teacher = teacherRepo.findBySchoolIdAndUserId(schoolId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", user.getId()));
        return teacher.getId();
    }

    private boolean isAdmin() {
        return SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin();
    }

    @Transactional
    public AssessmentDto createAssessment(UUID schoolId, AssessmentDto dto) {
        UUID teacherId = currentTeacherId(schoolId);
        TeacherAssessment a = TeacherAssessment.builder()
                .schoolId(schoolId)
                .teacherId(teacherId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .subjectId(dto.getSubjectId())
                .classId(dto.getClassId())
                .termId(dto.getTermId())
                .sessionId(dto.getSessionId())
                .assessmentType(dto.getAssessmentType())
                .maxScore(dto.getMaxScore() != null ? dto.getMaxScore() : new BigDecimal("100"))
                .dateConducted(dto.getDateConducted())
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .build();
        return mapToDto(assessmentRepo.save(a));
    }

    @Transactional
    public AssessmentDto updateAssessment(UUID schoolId, UUID assessmentId, AssessmentDto dto) {
        TeacherAssessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id", assessmentId));
        if (!a.getSchoolId().equals(schoolId)) throw new ResourceNotFoundException("Assessment", "id", assessmentId);
        if (!isAdmin() && !a.getTeacherId().equals(currentTeacherId(schoolId))) {
            throw new BadRequestException("You can only edit your own assessments");
        }
        if (dto.getTitle() != null) a.setTitle(dto.getTitle());
        if (dto.getDescription() != null) a.setDescription(dto.getDescription());
        if (dto.getSubjectId() != null) a.setSubjectId(dto.getSubjectId());
        if (dto.getClassId() != null) a.setClassId(dto.getClassId());
        if (dto.getTermId() != null) a.setTermId(dto.getTermId());
        if (dto.getSessionId() != null) a.setSessionId(dto.getSessionId());
        if (dto.getAssessmentType() != null) a.setAssessmentType(dto.getAssessmentType());
        if (dto.getMaxScore() != null) a.setMaxScore(dto.getMaxScore());
        if (dto.getDateConducted() != null) a.setDateConducted(dto.getDateConducted());
        if (dto.getStatus() != null) a.setStatus(dto.getStatus());
        return mapToDto(assessmentRepo.save(a));
    }

    @Transactional
    public void deleteAssessment(UUID schoolId, UUID assessmentId) {
        TeacherAssessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id", assessmentId));
        if (!a.getSchoolId().equals(schoolId)) throw new ResourceNotFoundException("Assessment", "id", assessmentId);
        if (!isAdmin() && !a.getTeacherId().equals(currentTeacherId(schoolId))) {
            throw new BadRequestException("You can only delete your own assessments");
        }
        scoreRepo.deleteAll(scoreRepo.findByAssessmentId(assessmentId));
        assessmentRepo.delete(a);
    }

    public Page<AssessmentDto> listAssessments(UUID schoolId, UUID teacherId, String search, Pageable pageable) {
        UUID currentTid = currentTeacherId(schoolId);
        UUID targetTeacherId = isAdmin() && teacherId != null ? teacherId : currentTid;
        Page<TeacherAssessment> page;
        if (search != null && !search.isBlank()) {
            page = assessmentRepo.searchByTeacher(schoolId, targetTeacherId, search, pageable);
        } else {
            page = assessmentRepo.findBySchoolIdAndTeacherId(schoolId, targetTeacherId, pageable);
        }
        return page.map(this::mapToDto);
    }

    public List<AssessmentDto> listAllForAdmin(UUID schoolId, UUID classId, UUID subjectId, UUID termId) {
        List<TeacherAssessment> all;
        if (classId != null && subjectId != null && termId != null) {
            all = assessmentRepo.findBySchoolIdAndClassIdAndSubjectIdAndTermId(
                    schoolId, classId, subjectId, termId);
        } else {
            all = assessmentRepo.findBySchoolId(schoolId);
        }
        return all.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public AssessmentDto getAssessment(UUID assessmentId) {
        TeacherAssessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id", assessmentId));
        return mapToDto(a);
    }

    @Transactional
    public void saveScores(UUID schoolId, UUID assessmentId, SaveScoresRequest request) {
        TeacherAssessment a = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id", assessmentId));
        if (!a.getSchoolId().equals(schoolId)) throw new ResourceNotFoundException("Assessment", "id", assessmentId);
        if (!isAdmin() && !a.getTeacherId().equals(currentTeacherId(schoolId))) {
            throw new BadRequestException("You can only score your own assessments");
        }
        for (SaveScoresRequest.ScoreEntry entry : request.getScores()) {
            Optional<AssessmentScore> existing = scoreRepo.findByAssessmentIdAndStudentId(assessmentId, entry.getStudentId());
            if (existing.isPresent()) {
                AssessmentScore s = existing.get();
                s.setScore(entry.getScore());
                s.setRemarks(entry.getRemarks());
                scoreRepo.save(s);
            } else {
                scoreRepo.save(AssessmentScore.builder()
                        .assessmentId(assessmentId)
                        .studentId(entry.getStudentId())
                        .score(entry.getScore())
                        .remarks(entry.getRemarks())
                        .build());
            }
        }
    }

    public List<AssessmentScoreDto> getScores(UUID assessmentId) {
        return scoreRepo.findByAssessmentIdWithStudent(assessmentId).stream()
                .map(this::mapScoreToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveGradingScheme(UUID schoolId, SaveGradingSchemeRequest request) {
        gradingRepo.deleteByFilters(schoolId, request.getClassId(), request.getSubjectId(), request.getTermId());
        int totalWeight = request.getEntries().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .mapToInt(SaveGradingSchemeRequest.Entry::getWeight)
                .sum();
        if (totalWeight != 100) {
            throw new BadRequestException("Total grading weight must equal exactly 100%. Current sum: " + totalWeight + "%");
        }
        for (SaveGradingSchemeRequest.Entry entry : request.getEntries()) {
            GradingSchemeEntry g = GradingSchemeEntry.builder()
                    .schoolId(schoolId)
                    .teacherId(currentTeacherId(schoolId))
                    .classId(request.getClassId())
                    .subjectId(request.getSubjectId())
                    .termId(request.getTermId())
                    .sourceType(entry.getSourceType())
                    .sourceId(entry.getSourceId())
                    .weight(entry.getWeight())
                    .active(Boolean.TRUE.equals(entry.getActive()))
                    .build();
            gradingRepo.save(g);
        }
    }

    public List<GradingSchemeEntryDto> getGradingScheme(UUID schoolId, UUID classId, UUID subjectId, UUID termId) {
        List<GradingSchemeEntry> entries = gradingRepo.findBySchoolIdAndClassIdAndSubjectIdAndTermIdAndActiveTrue(schoolId, classId, subjectId, termId);
        return entries.stream().map(this::mapGradingToDto).collect(Collectors.toList());
    }

    public List<AssessmentDto> getAvailableAssessments(UUID schoolId, UUID classId, UUID subjectId, UUID termId) {
        return assessmentRepo.findPublishedByFilters(schoolId, classId, subjectId, termId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> computeGradedScores(UUID schoolId, UUID classId, UUID subjectId, UUID termId) {
        List<GradingSchemeEntry> scheme = gradingRepo.findBySchoolIdAndClassIdAndSubjectIdAndTermIdAndActiveTrue(schoolId, classId, subjectId, termId);
        if (scheme.isEmpty()) return List.of();

        List<Student> students = studentRepo.findBySchoolIdAndClassId(schoolId, classId);
        List<Map<String, Object>> results = new ArrayList<>();

        for (Student student : students) {
            BigDecimal totalWeighted = BigDecimal.ZERO;
            for (GradingSchemeEntry entry : scheme) {
                BigDecimal pct = getPercentageForSource(student.getId(), entry);
                if (pct != null) {
                    BigDecimal weighted = pct.multiply(new BigDecimal(entry.getWeight())).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    totalWeighted = totalWeighted.add(weighted);
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("studentName", student.getFullName());
            row.put("admissionNumber", student.getAdmissionNumber());
            row.put("aggregateScore", totalWeighted.setScale(1, RoundingMode.HALF_UP));
            results.add(row);
        }
        results.sort((a, b) -> ((BigDecimal) b.get("aggregateScore")).compareTo((BigDecimal) a.get("aggregateScore")));
        return results;
    }

    private BigDecimal getPercentageForSource(UUID studentId, GradingSchemeEntry entry) {
        if ("ASSESSMENT".equalsIgnoreCase(entry.getSourceType())) {
            Optional<AssessmentScore> sc = scoreRepo.findByAssessmentIdAndStudentId(entry.getSourceId(), studentId);
            if (sc.isEmpty() || sc.get().getScore() == null) return null;
            TeacherAssessment a = assessmentRepo.findById(entry.getSourceId()).orElse(null);
            if (a == null || a.getMaxScore() == null || a.getMaxScore().compareTo(BigDecimal.ZERO) == 0) return null;
            return sc.get().getScore().multiply(new BigDecimal("100")).divide(a.getMaxScore(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private AssessmentDto mapToDto(TeacherAssessment a) {
        AssessmentDto dto = new AssessmentDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setSubjectId(a.getSubjectId());
        dto.setSubjectName(a.getSubjectId() != null ? subjectRepo.findById(a.getSubjectId()).map(Subject::getName).orElse(null) : null);
        dto.setClassId(a.getClassId());
        dto.setClassName(a.getClassId() != null ? classRepo.findById(a.getClassId()).map(SchoolClass::getName).orElse(null) : null);
        dto.setTermId(a.getTermId());
        dto.setTermName(a.getTermId() != null ? termRepo.findById(a.getTermId()).map(Term::getName).orElse(null) : null);
        dto.setSessionId(a.getSessionId());
        dto.setAssessmentType(a.getAssessmentType());
        dto.setMaxScore(a.getMaxScore());
        dto.setDateConducted(a.getDateConducted());
        dto.setStatus(a.getStatus());
        dto.setTotalStudents(scoreRepo.countByAssessmentId(a.getId()));
        dto.setScoredCount(scoreRepo.countScoredByAssessmentId(a.getId()));
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    private AssessmentScoreDto mapScoreToDto(AssessmentScore s) {
        AssessmentScoreDto dto = new AssessmentScoreDto();
        dto.setId(s.getId());
        dto.setAssessmentId(s.getAssessmentId());
        dto.setStudentId(s.getStudentId());
        dto.setStudentName(s.getStudent() != null ? s.getStudent().getFullName() : null);
        dto.setAdmissionNumber(s.getStudent() != null ? s.getStudent().getAdmissionNumber() : null);
        dto.setScore(s.getScore());
        dto.setRemarks(s.getRemarks());
        return dto;
    }

    private GradingSchemeEntryDto mapGradingToDto(GradingSchemeEntry g) {
        GradingSchemeEntryDto dto = new GradingSchemeEntryDto();
        dto.setId(g.getId());
        dto.setClassId(g.getClassId());
        dto.setClassName(classRepo.findById(g.getClassId()).map(SchoolClass::getName).orElse(null));
        dto.setSubjectId(g.getSubjectId());
        dto.setSubjectName(subjectRepo.findById(g.getSubjectId()).map(Subject::getName).orElse(null));
        dto.setTermId(g.getTermId());
        dto.setTermName(termRepo.findById(g.getTermId()).map(Term::getName).orElse(null));
        dto.setSourceType(g.getSourceType());
        dto.setSourceId(g.getSourceId());
        dto.setWeight(g.getWeight());
        dto.setActive(g.getActive());

        if ("ASSESSMENT".equalsIgnoreCase(g.getSourceType())) {
            assessmentRepo.findById(g.getSourceId()).ifPresent(a -> dto.setSourceTitle(a.getTitle()));
        } else if ("QUIZ".equalsIgnoreCase(g.getSourceType())) {
            quizRepo.findById(g.getSourceId()).ifPresent(q -> dto.setSourceTitle(q.getTitle()));
        }
        return dto;
    }
}
