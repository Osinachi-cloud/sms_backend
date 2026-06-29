package com.schoolsaas.service;

import com.schoolsaas.dto.gradebook.GradebookEntryDto;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradebookService {

    private final GradebookQueryRepository gradebookQueryRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final TermRepository termRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;

    private final GradingSchemeService gradingSchemeService;
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> computeGradebook(UUID schoolId, UUID classId, UUID subjectId) {
        List<Map<String, Object>> allResults = new ArrayList<>();

        List<Subject> subjects;
        if (subjectId != null) {
            subjects = subjectRepository.findById(subjectId).map(List::of).orElse(List.of());
        } else {
            subjects = subjectRepository.findBySchoolIdAndIsActiveTrue(schoolId);
        }

        for (Subject subject : subjects) {
            GradingScheme scheme;
            try {
                scheme = gradingSchemeService.getEffectiveScheme(schoolId, subject.getId());
            } catch (RuntimeException ex) {
                continue; // no scheme for this subject, skip
            }

            List<UUID> targetClassIds;
            if (classId != null) {
                targetClassIds = List.of(classId);
            } else {
                targetClassIds = studentRepository.findBySchoolId(schoolId).stream()
                        .map(Student::getClassId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
            }

            Map<UUID, String> classNameMap = new HashMap<>();

            for (UUID clsId : targetClassIds) {
                List<Student> students = studentRepository.findBySchoolIdAndClassId(schoolId, clsId);
                if (students.isEmpty()) continue;

                String clsName = classNameMap.computeIfAbsent(clsId,
                        id -> classRepository.findById(id).map(SchoolClass::getName).orElse(""));

                for (Student student : students) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("student_id", student.getId());
                    row.put("student_name", student.getFullName());
                    row.put("class_id", clsId);
                    row.put("class_name", clsName);
                    row.put("subject_id", subject.getId());
                    row.put("subject_name", subject.getName());

                    double totalScore = 0;
                    boolean allResolved = true;
                    List<Map<String, Object>> componentResults = new ArrayList<>();

                    List<GradingComponent> components = scheme.getComponents() != null ? scheme.getComponents() : List.of();
                    for (GradingComponent component : components) {
                        if (component.getName() == null || component.getWeight() == null) continue;

                        Map<String, Object> compResult = new HashMap<>();
                        compResult.put("component_name", component.getName());
                        compResult.put("weight", component.getWeight());

                        String quizType = component.getName().toUpperCase();
                        Optional<Quiz> selectedQuiz = quizRepository.findBySchoolIdAndClassIdAndSubjectIdAndQuizTypeAndIsSelectedForGradeTrue(
                                schoolId, clsId, subject.getId(), quizType);

                        if (selectedQuiz.isEmpty()) {
                            selectedQuiz = quizRepository.findBySchoolIdAndSubjectIdAndQuizTypeAndIsSelectedForGradeTrue(
                                            schoolId, subject.getId(), quizType)
                                    .stream()
                                    .filter(q -> q.getTargetClassIds() != null && q.getTargetClassIds().contains(clsId))
                                    .findFirst();
                        }

                        if (selectedQuiz.isEmpty()) {
                            compResult.put("score", null);
                            allResolved = false;
                        } else {
                            Quiz quiz = selectedQuiz.get();
                            Double resolvedScore = resolveStudentScore(student.getId(), quiz);

                            if (resolvedScore == null) {
                                if (quiz.getEndTime() != null && quiz.getEndTime().isBefore(LocalDateTime.now())) {
                                    compResult.put("score", "-");
                                } else {
                                    compResult.put("score", null);
                                    allResolved = false;
                                }
                            } else if (quiz.getTotalMarks() == null || quiz.getTotalMarks().doubleValue() == 0) {
                                compResult.put("score", null);
                                allResolved = false;
                            } else {
                                double scaledScore = (resolvedScore / quiz.getTotalMarks().doubleValue()) * component.getWeight();
                                scaledScore = Math.round(scaledScore * 10.0) / 10.0;
                                compResult.put("score", scaledScore);
                                totalScore += scaledScore;
                            }
                        }
                        componentResults.add(compResult);
                    }

                    row.put("components", componentResults);
                    row.put("total", Math.round(totalScore * 10.0) / 10.0);
                    row.put("total_complete", allResolved);
                    allResults.add(row);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("grading_scheme", (classId != null && subjectId != null) ? "Single" : "All");
        result.put("students", allResults);
        return result;
    }

    private Double resolveStudentScore(UUID studentId, Quiz quiz) {
        List<QuizSubmission> submissions = quizSubmissionRepository.findByQuizIdAndStudentId(quiz.getId(), studentId);
        if (submissions.isEmpty()) return null;

        String strategy = quiz.getScoreAggregationStrategy() != null ? quiz.getScoreAggregationStrategy() : "HIGHEST";
        
        switch (strategy.toUpperCase()) {
            case "LOWEST":
                return submissions.stream()
                        .map(s -> s.getScore() != null ? s.getScore().doubleValue() : 0.0)
                        .min(Double::compare).orElse(0.0);
            case "AVERAGE":
                double avg = submissions.stream()
                        .mapToDouble(s -> s.getScore() != null ? s.getScore().doubleValue() : 0.0)
                        .average().orElse(0.0);
                return Math.round(avg * 100.0) / 100.0;
            case "HIGHEST":
            default:
                return submissions.stream()
                        .map(s -> s.getScore() != null ? s.getScore().doubleValue() : 0.0)
                        .max(Double::compare).orElse(0.0);
        }
    }

    @Transactional(readOnly = true)
    public Page<GradebookEntryDto> getGradebook(
            UUID schoolId,
            UUID classId,
            UUID subjectId,
            UUID studentId,
            UUID termId,
            UUID sessionId,
            String search,
            Pageable pageable) {

        // Resolve defaults
        UUID effectiveTermId = termId;
        UUID effectiveSessionId = sessionId;
        if (effectiveTermId == null) {
            effectiveTermId = termRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                    .map(Term::getId).orElse(null);
        }
        if (effectiveSessionId == null) {
            effectiveSessionId = sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                    .map(AcademicSession::getId).orElse(null);
        }

        // Determine teacher restriction
        List<UUID> allowedClassIds = resolveTeacherClassIds(schoolId);
        if (allowedClassIds != null && allowedClassIds.isEmpty()) {
            return Page.empty(pageable);
        }
        if (classId != null && allowedClassIds != null && !allowedClassIds.contains(classId)) {
            return Page.empty(pageable);
        }

        long total = gradebookQueryRepository.countEntries(
                schoolId, classId, subjectId, studentId,
                effectiveTermId, effectiveSessionId,
                search, allowedClassIds);

        if (total == 0) {
            return Page.empty(pageable);
        }

        List<GradebookEntryDto> entries = gradebookQueryRepository.findEntries(
                schoolId, classId, subjectId, studentId,
                effectiveTermId, effectiveSessionId,
                search, allowedClassIds, pageable);

        return new PageImpl<>(entries, pageable, total);
    }

    private List<UUID> resolveTeacherClassIds(UUID schoolId) {
        if (SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin()) {
            return null; // No restriction
        }
        var userOpt = SecurityUtils.getCurrentUser();
        if (userOpt.isEmpty()) return List.of();
        var user = userOpt.get();

        var teacherOpt = teacherRepository.findBySchoolIdAndUserId(schoolId, user.getId());
        if (teacherOpt.isEmpty()) {
            return null; // Not a teacher, no restriction
        }

        Teacher teacher = teacherOpt.get();
        List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacher.getId());
        return assignments.stream()
                .map(TeacherClass::getClassId)
                .distinct()
                .collect(Collectors.toList());
    }
}
