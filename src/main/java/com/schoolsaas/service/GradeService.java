package com.schoolsaas.service;

import com.schoolsaas.dto.grade.GradeResponse;
import com.schoolsaas.model.Grade;
import com.schoolsaas.model.Subject;
import com.schoolsaas.model.Term;
import com.schoolsaas.repository.GradeRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.SubjectRepository;
import com.schoolsaas.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TermRepository termRepository;

    public List<GradeResponse> getStudentGrades(UUID schoolId, UUID studentId) {
        List<Grade> grades = gradeRepository.findBySchoolIdAndStudentId(schoolId, studentId);

        Map<UUID, Subject> subjectMap = subjectRepository.findBySchoolId(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));

        Map<UUID, Term> termMap = termRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                .stream().collect(Collectors.toMap(Term::getId, t -> t));

        return grades.stream().map(grade -> {
            Subject subject = grade.getSubjectId() != null ? subjectMap.get(grade.getSubjectId()) : null;
            Term term = grade.getTermId() != null ? termMap.get(grade.getTermId()) : null;

            return GradeResponse.builder()
                    .id(grade.getId())
                    .studentId(grade.getStudentId())
                    .subjectId(grade.getSubjectId())
                    .subjectName(subject != null ? subject.getName() : null)
                    .subjectCode(subject != null ? subject.getCode() : null)
                    .termId(grade.getTermId())
                    .termName(term != null ? term.getName() : null)
                    .assessmentType(grade.getAssessmentType())
                    .score(grade.getScore())
                    .maxScore(grade.getMaxScore())
                    .gradeLetter(grade.getGradeLetter())
                    .remarks(grade.getRemarks())
                    .createdAt(grade.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<GradeResponse> getGradesByTerm(UUID schoolId, UUID studentId, UUID termId) {
        List<Grade> grades = gradeRepository.findByStudentIdAndTermId(studentId, termId);

        Map<UUID, Subject> subjectMap = subjectRepository.findBySchoolId(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));

        Term term = termRepository.findById(termId).orElse(null);

        return grades.stream().map(grade -> {
            Subject subject = grade.getSubjectId() != null ? subjectMap.get(grade.getSubjectId()) : null;

            return GradeResponse.builder()
                    .id(grade.getId())
                    .studentId(grade.getStudentId())
                    .subjectId(grade.getSubjectId())
                    .subjectName(subject != null ? subject.getName() : null)
                    .subjectCode(subject != null ? subject.getCode() : null)
                    .termId(grade.getTermId())
                    .termName(term != null ? term.getName() : null)
                    .assessmentType(grade.getAssessmentType())
                    .score(grade.getScore())
                    .maxScore(grade.getMaxScore())
                    .gradeLetter(grade.getGradeLetter())
                    .remarks(grade.getRemarks())
                    .createdAt(grade.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}
