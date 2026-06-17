package com.schoolsaas.service;

import com.schoolsaas.dto.quiz.*;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final QuizAnswerRepository answerRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final NotificationService notificationService;
    private final GamificationService gamificationService;

    @Transactional
    public QuizDto createQuiz(UUID schoolId, QuizDto dto) {
        UUID currentUserId = SecurityUtils.getCurrentUser().map(UserPrincipal::getId).orElse(null);
        Quiz quiz = Quiz.builder()
                .schoolId(schoolId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .subjectId(dto.getSubjectId())
                .classId(dto.getClassId())
                .durationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 30)
                .totalMarks(dto.getTotalMarks() != null ? dto.getTotalMarks() : new BigDecimal("100.00"))
                .passMark(dto.getPassMark() != null ? dto.getPassMark() : new BigDecimal("40.00"))
                .shuffleQuestions(Boolean.TRUE.equals(dto.getShuffleQuestions()))
                .showResultsImmediately(dto.getShowResultsImmediately() != null ? dto.getShowResultsImmediately() : true)
                .maxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 1)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .targetClassIds(dto.getTargetClassIds() != null ? dto.getTargetClassIds() : (dto.getClassId() != null ? java.util.List.of(dto.getClassId()) : java.util.List.of()))
                .createdBy(currentUserId)
                .build();
        quiz = quizRepository.save(quiz);

        if (dto.getQuestions() != null) {
            int orderIndex = 0;
            for (QuizQuestionDto qDto : dto.getQuestions()) {
                if (qDto == null) continue;
                QuizQuestion q = QuizQuestion.builder()
                        .quizId(quiz.getId())
                        .questionText(qDto.getQuestionText())
                        .questionType(qDto.getQuestionType() != null ? qDto.getQuestionType() : "MCQ")
                        .options(qDto.getOptions() != null ? qDto.getOptions() : java.util.List.of())
                        .correctAnswer(qDto.getCorrectAnswer())
                        .correctAnswers(qDto.getCorrectAnswers() != null ? qDto.getCorrectAnswers() : java.util.List.of())
                        .marks(qDto.getMarks() != null ? qDto.getMarks() : java.math.BigDecimal.ONE)
                        .orderIndex(orderIndex++)
                        .explanation(qDto.getExplanation())
                        .build();
                questionRepository.save(q);
            }
        }

        // Notify students in target classes
        notifyStudentsOfNewQuiz(schoolId, quiz);

        return mapToDto(quiz);
    }

    public Page<QuizDto> listQuizzes(UUID schoolId, UUID studentId, Pageable pageable) {
        Page<Quiz> quizzes = quizRepository.findBySchoolId(schoolId, pageable);
        if (studentId != null) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student != null && student.getSchoolId().equals(schoolId)) {
                List<QuizDto> filtered = quizzes.getContent().stream()
                        .filter(q -> isQuizVisibleToStudent(q, student))
                        .map(this::mapToDto)
                        .collect(java.util.stream.Collectors.toList());
                return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
        }
        return quizzes.map(this::mapToDto);
    }

    public QuizDto getQuiz(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();
        if (studentId != null) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student != null && !isQuizVisibleToStudent(quiz, student)) {
                throw new com.schoolsaas.exception.BadRequestException("You do not have access to this quiz");
            }
        }
        return mapToDto(quiz);
    }

    @Transactional
    public QuizDto updateQuiz(UUID schoolId, UUID quizId, QuizDto dto, boolean isAdmin) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId));
        if (!quiz.getSchoolId().equals(schoolId)) {
            throw new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId);
        }
        if (!isAdmin && quiz.getCreatedBy() != null) {
            UUID userId = SecurityUtils.getCurrentUserId();
            if (!userId.equals(quiz.getCreatedBy())) {
                throw new com.schoolsaas.exception.BadRequestException("You can only edit quizzes you created");
            }
        }

        quiz.setTitle(dto.getTitle());
        quiz.setDescription(dto.getDescription());
        quiz.setSubjectId(dto.getSubjectId());
        quiz.setClassId(dto.getClassId());
        if (dto.getTargetClassIds() != null) {
            quiz.setTargetClassIds(dto.getTargetClassIds());
        } else if (dto.getClassId() != null) {
            quiz.setTargetClassIds(java.util.List.of(dto.getClassId()));
        }
        quiz.setDurationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : quiz.getDurationMinutes());
        quiz.setTotalMarks(dto.getTotalMarks() != null ? dto.getTotalMarks() : quiz.getTotalMarks());
        quiz.setPassMark(dto.getPassMark() != null ? dto.getPassMark() : quiz.getPassMark());
        quiz.setShuffleQuestions(Boolean.TRUE.equals(dto.getShuffleQuestions()));
        quiz.setShowResultsImmediately(dto.getShowResultsImmediately() != null ? dto.getShowResultsImmediately() : quiz.getShowResultsImmediately());
        quiz.setMaxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : quiz.getMaxAttempts());
        quiz.setStartTime(dto.getStartTime());
        quiz.setEndTime(dto.getEndTime());
        quiz.setStatus(dto.getStatus() != null ? dto.getStatus() : quiz.getStatus());
        quiz = quizRepository.save(quiz);

        // Replace questions if provided
        if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            questionRepository.deleteByQuizId(quiz.getId());
            int orderIndex = 0;
            for (QuizQuestionDto qDto : dto.getQuestions()) {
                if (qDto == null) continue;
                QuizQuestion q = QuizQuestion.builder()
                        .quizId(quiz.getId())
                        .questionText(qDto.getQuestionText())
                        .questionType(qDto.getQuestionType() != null ? qDto.getQuestionType() : "MCQ")
                        .options(qDto.getOptions() != null ? qDto.getOptions() : java.util.List.of())
                        .correctAnswer(qDto.getCorrectAnswer())
                        .correctAnswers(qDto.getCorrectAnswers() != null ? qDto.getCorrectAnswers() : java.util.List.of())
                        .marks(qDto.getMarks() != null ? qDto.getMarks() : java.math.BigDecimal.ONE)
                        .orderIndex(orderIndex++)
                        .explanation(qDto.getExplanation())
                        .build();
                questionRepository.save(q);
            }
        }

        return mapToDto(quiz);
    }

    @Transactional
    public void deleteQuiz(UUID schoolId, UUID quizId, boolean isAdmin) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId));
        if (!quiz.getSchoolId().equals(schoolId)) {
            throw new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId);
        }
        if (!isAdmin && quiz.getCreatedBy() != null) {
            UUID userId = SecurityUtils.getCurrentUserId();
            if (!userId.equals(quiz.getCreatedBy())) {
                throw new com.schoolsaas.exception.BadRequestException("You can only delete quizzes you created");
            }
        }
        questionRepository.deleteByQuizId(quiz.getId());
        quizRepository.delete(quiz);
    }

    public QuizDto startQuiz(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student != null && !isQuizVisibleToStudent(quiz, student)) {
            throw new com.schoolsaas.exception.BadRequestException("You do not have access to this quiz");
        }

        long attempts = submissionRepository.countByQuizIdAndStudentId(quizId, studentId);
        if (attempts >= quiz.getMaxAttempts()) {
            throw new RuntimeException("Maximum attempts reached");
        }

        QuizSubmission submission = QuizSubmission.builder()
                .quizId(quizId)
                .studentId(studentId)
                .attemptNumber((int) attempts + 1)
                .build();
        submissionRepository.save(submission);

        QuizDto dto = mapToDto(quiz);
        // Don't send correct answers when starting!
        dto.getQuestions().forEach(q -> {
            q.setCorrectAnswer(null);
            q.setCorrectAnswers(null);
        });
        return dto;
    }

    @Transactional
    public QuizResultDto submitQuiz(UUID quizId, UUID studentId, SubmitQuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();
        List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);

        QuizSubmission submission = submissionRepository
                .findByQuizIdAndStudentIdAndStatus(quizId, studentId, "IN_PROGRESS")
                .orElseThrow(() -> new RuntimeException("No active submission found"));

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxMarks = BigDecimal.ZERO;
        List<QuizAnswerDto> answerDtos = new ArrayList<>();

        for (QuizQuestion question : questions) {
            maxMarks = maxMarks.add(question.getMarks());
            Map<String, Object> answerData = request.getAnswers().stream()
                    .filter(a -> question.getId().equals(UUID.fromString(a.get("questionId").toString())))
                    .findFirst().orElse(null);

            BigDecimal marksObtained = BigDecimal.ZERO;
            Boolean isCorrect = false;
            String userAnswer = "";
            List<String> selectedOptions = new ArrayList<>();

            if (answerData != null) {
                userAnswer = answerData.get("answer") != null ? answerData.get("answer").toString() : "";
                if (answerData.get("selectedOptions") != null) {
                    selectedOptions = ((List<?>) answerData.get("selectedOptions")).stream()
                            .map(Object::toString).collect(Collectors.toList());
                }

                isCorrect = gradeQuestion(question, userAnswer, selectedOptions);
                if (isCorrect) marksObtained = question.getMarks();
            }

            QuizAnswer answer = QuizAnswer.builder()
                    .submissionId(submission.getId())
                    .questionId(question.getId())
                    .answer(userAnswer)
                    .selectedOptions(selectedOptions)
                    .isCorrect(isCorrect)
                    .marksObtained(marksObtained)
                    .build();
            answerRepository.save(answer);

            totalScore = totalScore.add(marksObtained);

            QuizAnswerDto ad = new QuizAnswerDto();
            ad.setQuestionId(question.getId());
            ad.setQuestionText(question.getQuestionText());
            ad.setUserAnswer(userAnswer);
            ad.setCorrectAnswer(question.getCorrectAnswer());
            ad.setIsCorrect(isCorrect);
            ad.setMarksObtained(marksObtained);
            ad.setTotalMarks(question.getMarks());
            ad.setExplanation(question.getExplanation());
            answerDtos.add(ad);
        }

        BigDecimal percentage = maxMarks.compareTo(BigDecimal.ZERO) > 0 
                ? totalScore.multiply(new BigDecimal("100")).divide(maxMarks, 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        double percentageDouble = percentage.doubleValue();
        String gradeLetter = percentageDouble >= 70 ? "A" : percentageDouble >= 60 ? "B" : percentageDouble >= 50 ? "C" : percentageDouble >= 45 ? "D" : "F";

        submission.setSubmittedAt(LocalDateTime.now());
        submission.setScore(totalScore);
        submission.setTotalMarks(maxMarks);
        submission.setPercentage(percentage);
        submission.setGradeLetter(gradeLetter);
        submission.setStatus("SUBMITTED");
        submissionRepository.save(submission);

        // Award points for quiz completion
        gamificationService.awardPoints(studentId, quiz.getSchoolId(), 10, "EARNED", "Completed quiz: " + quiz.getTitle(), "QUIZ", quiz.getId());

        QuizResultDto result = new QuizResultDto();
        result.setSubmissionId(submission.getId());
        result.setQuizId(quizId);
        result.setQuizTitle(quiz.getTitle());
        result.setScore(totalScore);
        result.setTotalMarks(maxMarks);
        result.setPercentage(percentage);
        result.setGradeLetter(gradeLetter);
        result.setStatus(submission.getStatus());
        result.setAnswers(answerDtos);
        return result;
    }

    private boolean isQuizVisibleToStudent(Quiz quiz, Student student) {
        if (!quiz.getSchoolId().equals(student.getSchoolId())) {
            return false;
        }
        List<UUID> targets = quiz.getTargetClassIds();
        if (targets != null && !targets.isEmpty()) {
            return targets.contains(student.getClassId());
        }
        if (quiz.getClassId() != null) {
            return quiz.getClassId().equals(student.getClassId());
        }
        return true;
    }

    private void notifyStudentsOfNewQuiz(UUID schoolId, Quiz quiz) {
        List<UUID> targets = quiz.getTargetClassIds();
        if (targets != null && !targets.isEmpty()) {
            for (UUID classId : targets) {
                List<Student> students = studentRepository.findBySchoolIdAndClassId(schoolId, classId);
                for (Student s : students) {
                    if (s.getUserId() != null) {
                        notificationService.sendNotification(s.getUserId(), schoolId, "New Quiz Available",
                                "A new quiz has been published: " + quiz.getTitle(), "ASSIGNMENT", quiz.getId());
                    }
                }
            }
        } else if (quiz.getClassId() != null) {
            List<Student> students = studentRepository.findBySchoolIdAndClassId(schoolId, quiz.getClassId());
            for (Student s : students) {
                if (s.getUserId() != null) {
                    notificationService.sendNotification(s.getUserId(), schoolId, "New Quiz Available",
                            "A new quiz has been published: " + quiz.getTitle(), "ASSIGNMENT", quiz.getId());
                }
            }
        }
    }

    private boolean gradeQuestion(QuizQuestion question, String userAnswer, List<String> selectedOptions) {
        return switch (question.getQuestionType()) {
            case "MCQ", "TRUE_FALSE" ->
                    userAnswer != null && userAnswer.equalsIgnoreCase(question.getCorrectAnswer());
            case "FILL_BLANK" ->
                    userAnswer != null && question.getCorrectAnswer() != null &&
                    userAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
            case "MATCHING" -> {
                if (selectedOptions == null || question.getCorrectAnswers() == null) yield false;
                yield new HashSet<>(selectedOptions).containsAll(question.getCorrectAnswers()) &&
                        selectedOptions.size() == question.getCorrectAnswers().size();
            }
            default -> false;
        };
    }

    private QuizDto mapToDto(Quiz quiz) {
        QuizDto dto = new QuizDto();
        dto.setId(quiz.getId());
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setSubjectId(quiz.getSubjectId());
        if (quiz.getSubjectId() != null) {
            subjectRepository.findById(quiz.getSubjectId()).ifPresent(s -> dto.setSubjectName(s.getName()));
        }
        dto.setClassId(quiz.getClassId());
        if (quiz.getClassId() != null) {
            classRepository.findById(quiz.getClassId()).ifPresent(c -> dto.setClassName(c.getName()));
        }
        dto.setTargetClassIds(quiz.getTargetClassIds());
        dto.setDurationMinutes(quiz.getDurationMinutes());
        dto.setTotalMarks(quiz.getTotalMarks());
        dto.setPassMark(quiz.getPassMark());
        dto.setShuffleQuestions(quiz.getShuffleQuestions());
        dto.setShowResultsImmediately(quiz.getShowResultsImmediately());
        dto.setMaxAttempts(quiz.getMaxAttempts());
        dto.setStartTime(quiz.getStartTime());
        dto.setEndTime(quiz.getEndTime());
        dto.setStatus(quiz.getStatus());
        List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        dto.setQuestionCount(questions.size());
        dto.setQuestions(questions.stream().map(q -> {
            QuizQuestionDto qd = new QuizQuestionDto();
            qd.setId(q.getId());
            qd.setQuestionText(q.getQuestionText());
            qd.setQuestionType(q.getQuestionType());
            qd.setOptions(q.getOptions());
            qd.setMarks(q.getMarks());
            qd.setOrderIndex(q.getOrderIndex());
            qd.setExplanation(q.getExplanation());
            qd.setCorrectAnswer(q.getCorrectAnswer());
            qd.setCorrectAnswers(q.getCorrectAnswers());
            return qd;
        }).collect(Collectors.toList()));
        return dto;
    }
}
