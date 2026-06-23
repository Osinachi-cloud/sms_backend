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
    private final TermRepository termRepository;
    private final AcademicSessionRepository sessionRepository;
    private final GradeRepository gradeRepository;
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
                .quizType(dto.getQuizType() != null ? dto.getQuizType() : "QUIZ")
                .isEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true)
                .showCorrectAnswers(dto.getShowCorrectAnswers() != null ? dto.getShowCorrectAnswers() : false)
                .resultVisibilityType(dto.getResultVisibilityType() != null ? dto.getResultVisibilityType() : "NEVER")
                .resultsReleased(dto.getResultsReleased() != null ? dto.getResultsReleased() : false)
                .termId(dto.getTermId())
                .sessionId(dto.getSessionId())
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
                        .map(q -> mapToDtoWithStudentInfo(q, studentId))
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
        quiz.setQuizType(dto.getQuizType() != null ? dto.getQuizType() : quiz.getQuizType());
        quiz.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : quiz.getIsEnabled());
        quiz.setShowCorrectAnswers(dto.getShowCorrectAnswers() != null ? dto.getShowCorrectAnswers() : quiz.getShowCorrectAnswers());
        quiz.setResultVisibilityType(dto.getResultVisibilityType() != null ? dto.getResultVisibilityType() : quiz.getResultVisibilityType());
        quiz.setResultsReleased(dto.getResultsReleased() != null ? dto.getResultsReleased() : quiz.getResultsReleased());
        quiz.setTermId(dto.getTermId());
        quiz.setSessionId(dto.getSessionId());
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

        if (Boolean.FALSE.equals(quiz.getIsEnabled())) {
            throw new com.schoolsaas.exception.BadRequestException("This quiz is currently disabled");
        }

        LocalDateTime now = LocalDateTime.now();
        if (quiz.getStartTime() != null && now.isBefore(quiz.getStartTime())) {
            throw new com.schoolsaas.exception.BadRequestException("This quiz has not started yet");
        }
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            throw new com.schoolsaas.exception.BadRequestException("This quiz has expired");
        }

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student != null && !isQuizVisibleToStudent(quiz, student)) {
            throw new com.schoolsaas.exception.BadRequestException("You do not have access to this quiz");
        }

        long attempts = submissionRepository.countByQuizIdAndStudentId(quizId, studentId);
        if (attempts >= quiz.getMaxAttempts()) {
            throw new com.schoolsaas.exception.BadRequestException("Maximum attempts reached");
        }

        // Auto-submit ALL stale IN_PROGRESS submissions
        List<QuizSubmission> inProgressSubs = submissionRepository.findByQuizIdAndStudentIdAndStatus(quizId, studentId, "IN_PROGRESS");
        for (QuizSubmission sub : inProgressSubs) {
            if (sub.getStartedAt() != null && quiz.getDurationMinutes() != null) {
                LocalDateTime deadline = sub.getStartedAt().plusMinutes(quiz.getDurationMinutes());
                if (now.isAfter(deadline)) {
                    sub.setStatus("TIMED_OUT");
                    sub.setSubmittedAt(deadline);
                    submissionRepository.save(sub);
                }
            }
        }

        // Check remaining valid attempts after cleanup
        long validAttempts = submissionRepository.countByQuizIdAndStudentIdAndStatusNot(quizId, studentId, "IN_PROGRESS");
        if (validAttempts >= quiz.getMaxAttempts()) {
            throw new com.schoolsaas.exception.BadRequestException("Maximum attempts reached");
        }

        // Check if there's still a fresh IN_PROGRESS — reuse it instead of creating a duplicate
        Optional<QuizSubmission> freshSub = submissionRepository
                .findFirstByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(quizId, studentId, "IN_PROGRESS");
        if (freshSub.isPresent()) {
            QuizDto dto = mapToDto(quiz);
            if (quiz.getDurationMinutes() != null && freshSub.get().getStartedAt() != null) {
                dto.setEndTime(freshSub.get().getStartedAt().plusMinutes(quiz.getDurationMinutes()));
            }
            dto.getQuestions().forEach(q -> {
                q.setCorrectAnswer(null);
                q.setCorrectAnswers(null);
            });
            return dto;
        }

        QuizSubmission submission = QuizSubmission.builder()
                .quizId(quizId)
                .studentId(studentId)
                .startedAt(now)
                .attemptNumber((int) validAttempts + 1)
                .build();
        submissionRepository.save(submission);

        QuizDto dto = mapToDto(quiz);
        if (quiz.getDurationMinutes() != null) {
            dto.setEndTime(now.plusMinutes(quiz.getDurationMinutes()));
        }
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
                .findFirstByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(quizId, studentId, "IN_PROGRESS")
                .orElseThrow(() -> new com.schoolsaas.exception.BadRequestException("No active submission found. Please start the quiz first."));

        // Enforce duration limit
        LocalDateTime now = LocalDateTime.now();
        if (quiz.getDurationMinutes() != null && submission.getStartedAt() != null) {
            LocalDateTime deadline = submission.getStartedAt().plusMinutes(quiz.getDurationMinutes());
            if (now.isAfter(deadline)) {
                submission.setStatus("TIMED_OUT");
                submission.setSubmittedAt(deadline);
                submissionRepository.save(submission);
                throw new com.schoolsaas.exception.BadRequestException("Time limit exceeded. Quiz has been auto-submitted.");
            }
        }

        // Also check expiry
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            throw new com.schoolsaas.exception.BadRequestException("This quiz has expired");
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxMarks = BigDecimal.ZERO;
        List<QuizAnswerDto> answerDtos = new ArrayList<>();

        for (QuizQuestion question : questions) {
            maxMarks = maxMarks.add(question.getMarks());
            Map<String, Object> answerData = request.getAnswers() != null ? request.getAnswers().stream()
                    .filter(a -> a != null && a.get("questionId") != null && question.getId().equals(UUID.fromString(a.get("questionId").toString())))
                    .findFirst().orElse(null) : null;

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
            ad.setSelectedOptions(selectedOptions);
            ad.setCorrectAnswer(question.getCorrectAnswer());
            ad.setCorrectAnswers(question.getCorrectAnswers());
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

        submission.setSubmittedAt(now);
        submission.setScore(totalScore);
        submission.setTotalMarks(maxMarks);
        submission.setPercentage(percentage);
        submission.setGradeLetter(gradeLetter);
        submission.setStatus("SUBMITTED");
        submissionRepository.save(submission);

        // Award points for quiz completion (use user's UUID, not student's UUID)
        Student submittingStudent = studentRepository.findById(studentId).orElse(null);
        if (submittingStudent != null && submittingStudent.getUserId() != null) {
            try {
                gamificationService.awardPoints(submittingStudent.getUserId(), quiz.getSchoolId(), 10, "EARNED", "Completed quiz: " + quiz.getTitle(), "QUIZ", quiz.getId());
            } catch (Exception e) {
                // Gamification failure should not break quiz submission
            }
        }

        boolean resultsVisible = determineIfResultsVisible(quiz);
        String resultsAvailableText = buildResultsAvailableText(quiz);

        QuizResultDto result = new QuizResultDto();
        result.setSubmissionId(submission.getId());
        result.setQuizId(quizId);
        result.setQuizTitle(quiz.getTitle());
        result.setStatus(submission.getStatus());
        result.setResultsVisible(resultsVisible);
        result.setResultsAvailableText(resultsAvailableText);

        if (resultsVisible) {
            result.setScore(totalScore);
            result.setTotalMarks(maxMarks);
            result.setPercentage(percentage);
            result.setGradeLetter(gradeLetter);
            result.setShowCorrectAnswers(quiz.getShowCorrectAnswers());
            result.setAnswers(answerDtos);
        } else {
            result.setShowCorrectAnswers(false);
            result.setAnswers(java.util.List.of());
        }
        return result;
    }

    private boolean determineIfResultsVisible(Quiz quiz) {
        String type = quiz.getResultVisibilityType();
        if (type == null) type = "NEVER";
        return switch (type) {
            case "IMMEDIATELY" -> true;
            case "AFTER_ALL_SUBMITTED" -> allStudentsSubmitted(quiz);
            case "AFTER_DEADLINE" -> quiz.getEndTime() != null && LocalDateTime.now().isAfter(quiz.getEndTime());
            case "MANUAL" -> Boolean.TRUE.equals(quiz.getResultsReleased());
            case "NEVER" -> false;
            default -> false;
        };
    }

    private String buildResultsAvailableText(Quiz quiz) {
        String type = quiz.getResultVisibilityType();
        if (type == null) type = "NEVER";
        return switch (type) {
            case "IMMEDIATELY" -> "Results are available immediately.";
            case "AFTER_ALL_SUBMITTED" -> allStudentsSubmitted(quiz)
                    ? "Results are now available."
                    : "Results will be available after all students have submitted.";
            case "AFTER_DEADLINE" -> quiz.getEndTime() != null
                    ? "Results will be available after the deadline on " + quiz.getEndTime().toLocalDate() + "."
                    : "Results will be available after the deadline.";
            case "MANUAL" -> Boolean.TRUE.equals(quiz.getResultsReleased())
                    ? "Results have been released by your teacher."
                    : "Your teacher will release results when ready.";
            case "NEVER" -> "Results are not available for this assessment.";
            default -> "Results are not available.";
        };
    }

    private boolean allStudentsSubmitted(Quiz quiz) {
        List<UUID> targetClassIds = quiz.getTargetClassIds();
        if (targetClassIds == null || targetClassIds.isEmpty()) {
            if (quiz.getClassId() != null) {
                targetClassIds = java.util.List.of(quiz.getClassId());
            } else {
                return false; // Cannot determine eligible students
            }
        }

        long totalEligible = 0;
        for (UUID classId : targetClassIds) {
            totalEligible += studentRepository.countBySchoolIdAndClassId(quiz.getSchoolId(), classId);
        }

        List<QuizSubmission> subs = submissionRepository.findByQuizId(quiz.getId());
        long uniqueSubmitted = subs.stream()
                .filter(s -> !"IN_PROGRESS".equals(s.getStatus()))
                .map(QuizSubmission::getStudentId)
                .distinct()
                .count();

        return totalEligible > 0 && uniqueSubmitted >= totalEligible;
    }

    @Transactional
    public QuizDto releaseQuizResults(UUID schoolId, UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId));
        if (!quiz.getSchoolId().equals(schoolId)) {
            throw new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId);
        }
        quiz.setResultsReleased(true);
        return mapToDto(quizRepository.save(quiz));
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
            case "FILL_BLANK", "SHORT_ANSWER" ->
                    userAnswer != null && question.getCorrectAnswer() != null &&
                    userAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
            case "CHECKBOX" -> {
                if (selectedOptions == null || question.getCorrectAnswers() == null) yield false;
                yield new HashSet<>(selectedOptions).containsAll(question.getCorrectAnswers()) &&
                        selectedOptions.size() == question.getCorrectAnswers().size();
            }
            case "MATCHING" -> {
                if (selectedOptions == null || question.getCorrectAnswers() == null) yield false;
                yield new HashSet<>(selectedOptions).containsAll(question.getCorrectAnswers()) &&
                        selectedOptions.size() == question.getCorrectAnswers().size();
            }
            case "PARAGRAPH" -> {
                // Paragraph requires manual grading; default to false until teacher grades
                yield false;
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
        dto.setQuizType(quiz.getQuizType());
        dto.setIsEnabled(quiz.getIsEnabled());
        dto.setShowCorrectAnswers(quiz.getShowCorrectAnswers());
        dto.setResultVisibilityType(quiz.getResultVisibilityType());
        dto.setResultsReleased(quiz.getResultsReleased());
        dto.setTermId(quiz.getTermId());
        if (quiz.getTermId() != null) {
            termRepository.findById(quiz.getTermId()).ifPresent(t -> dto.setTermName(t.getName()));
        }
        dto.setSessionId(quiz.getSessionId());
        if (quiz.getSessionId() != null) {
            sessionRepository.findById(quiz.getSessionId()).ifPresent(s -> dto.setSessionName(s.getName()));
        }
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

    private QuizDto mapToDtoWithStudentInfo(Quiz quiz, UUID studentId) {
        QuizDto dto = mapToDto(quiz);
        List<QuizSubmission> subs = submissionRepository.findByQuizIdAndStudentId(quiz.getId(), studentId);
        dto.setHasAttempted(!subs.isEmpty());
        dto.setAttemptsUsed((int) subs.stream().filter(s -> !"IN_PROGRESS".equals(s.getStatus())).count());
        dto.setBestScore(subs.stream()
                .filter(s -> s.getScore() != null)
                .map(QuizSubmission::getScore)
                .max(BigDecimal::compareTo)
                .orElse(null));
        return dto;
    }

    public List<QuizSubmission> getQuizSubmissions(UUID quizId) {
        return submissionRepository.findByQuizId(quizId);
    }

    public List<QuizParticipantDto> getQuizParticipants(UUID quizId, String search, String statusFilter, BigDecimal minScore, BigDecimal maxScore) {
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        BigDecimal passMark = quiz != null ? quiz.getPassMark() : BigDecimal.ZERO;

        List<QuizSubmission> subs = submissionRepository.findByQuizId(quizId);
        Set<UUID> studentIds = subs.stream().map(QuizSubmission::getStudentId).collect(Collectors.toSet());
        if (studentIds.isEmpty()) {
            return List.of();
        }

        List<Student> students = studentRepository.findAllById(studentIds);
        Map<UUID, Student> studentMap = students.stream().collect(Collectors.toMap(Student::getId, s -> s));

        Map<UUID, List<QuizSubmission>> grouped = subs.stream().collect(Collectors.groupingBy(QuizSubmission::getStudentId));

        List<QuizParticipantDto> result = new ArrayList<>();
        for (Map.Entry<UUID, List<QuizSubmission>> entry : grouped.entrySet()) {
            UUID sid = entry.getKey();
            Student student = studentMap.get(sid);
            if (student == null) continue;

            List<QuizSubmission> attempts = entry.getValue().stream()
                    .sorted(Comparator.comparing(QuizSubmission::getAttemptNumber, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            QuizSubmission best = attempts.stream()
                    .filter(a -> a.getScore() != null)
                    .max(Comparator.comparing(QuizSubmission::getScore))
                    .orElse(null);

            boolean passed = best != null && best.getPercentage() != null
                    && best.getPercentage().compareTo(passMark) >= 0;

            QuizParticipantDto dto = new QuizParticipantDto();
            dto.setStudentId(sid);
            dto.setStudentName(student.getFullName());
            dto.setAdmissionNumber(student.getAdmissionNumber());
            if (student.getClassId() != null) {
                classRepository.findById(student.getClassId()).ifPresent(c -> dto.setClassName(c.getName()));
            }
            dto.setAttemptCount(attempts.size());
            dto.setBestScore(best != null ? best.getScore() : null);
            dto.setBestPercentage(best != null ? best.getPercentage() : null);
            dto.setBestGradeLetter(best != null ? best.getGradeLetter() : null);
            dto.setPassed(passed);
            dto.setAttempts(attempts.stream().map(a -> {
                QuizParticipantDto.AttemptInfo info = new QuizParticipantDto.AttemptInfo();
                info.setSubmissionId(a.getId());
                info.setAttemptNumber(a.getAttemptNumber());
                info.setScore(a.getScore());
                info.setTotalMarks(a.getTotalMarks());
                info.setPercentage(a.getPercentage());
                info.setGradeLetter(a.getGradeLetter());
                info.setStatus(a.getStatus());
                info.setStartedAt(a.getStartedAt());
                info.setSubmittedAt(a.getSubmittedAt());
                return info;
            }).collect(Collectors.toList()));

            result.add(dto);
        }

        // Apply filters
        return result.stream().filter(dto -> {
            if (search != null && !search.isBlank()) {
                String term = search.toLowerCase();
                String text = String.join(" ",
                        String.valueOf(dto.getStudentName()),
                        String.valueOf(dto.getAdmissionNumber()),
                        String.valueOf(dto.getClassName())
                ).toLowerCase();
                if (!text.contains(term)) return false;
            }
            if (statusFilter != null && !statusFilter.isBlank()) {
                if ("PASSED".equalsIgnoreCase(statusFilter) && !dto.isPassed()) return false;
                if ("FAILED".equalsIgnoreCase(statusFilter) && dto.isPassed()) return false;
            }
            if (minScore != null) {
                if (dto.getBestScore() == null || dto.getBestScore().compareTo(minScore) < 0) return false;
            }
            if (maxScore != null) {
                if (dto.getBestScore() == null || dto.getBestScore().compareTo(maxScore) > 0) return false;
            }
            return true;
        }).sorted(Comparator.comparing(QuizParticipantDto::getStudentName, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());
    }

    public List<QuizResultDto> getStudentQuizHistory(UUID studentId) {
        List<QuizSubmission> subs = submissionRepository.findByStudentId(studentId);
        return subs.stream().map(sub -> {
            Quiz quiz = quizRepository.findById(sub.getQuizId()).orElse(null);
            boolean resultsVisible = quiz != null && determineIfResultsVisible(quiz);
            String resultsAvailableText = quiz != null ? buildResultsAvailableText(quiz) : null;

            QuizResultDto dto = new QuizResultDto();
            dto.setSubmissionId(sub.getId());
            dto.setQuizId(sub.getQuizId());
            dto.setQuizTitle(quiz != null ? quiz.getTitle() : "Unknown");
            dto.setStatus(sub.getStatus());
            dto.setResultsVisible(resultsVisible);
            dto.setResultsAvailableText(resultsAvailableText);

            if (resultsVisible) {
                List<QuizAnswer> answers = answerRepository.findBySubmissionId(sub.getId());
                dto.setScore(sub.getScore());
                dto.setTotalMarks(sub.getTotalMarks());
                dto.setPercentage(sub.getPercentage());
                dto.setGradeLetter(sub.getGradeLetter());
                dto.setShowCorrectAnswers(quiz != null ? quiz.getShowCorrectAnswers() : false);
                dto.setAnswers(answers.stream().map(a -> {
                    QuizAnswerDto ad = new QuizAnswerDto();
                    ad.setQuestionId(a.getQuestionId());
                    QuizQuestion q = questionRepository.findById(a.getQuestionId()).orElse(null);
                    ad.setQuestionText(q != null ? q.getQuestionText() : "");
                    ad.setUserAnswer(a.getAnswer());
                    ad.setSelectedOptions(a.getSelectedOptions());
                    ad.setCorrectAnswer(q != null ? q.getCorrectAnswer() : null);
                    ad.setCorrectAnswers(q != null ? q.getCorrectAnswers() : null);
                    ad.setIsCorrect(a.getIsCorrect());
                    ad.setMarksObtained(a.getMarksObtained());
                    ad.setTotalMarks(q != null ? q.getMarks() : BigDecimal.ZERO);
                    ad.setExplanation(q != null ? q.getExplanation() : null);
                    return ad;
                }).collect(Collectors.toList()));
            } else {
                dto.setShowCorrectAnswers(false);
                dto.setAnswers(java.util.List.of());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public QuizDto toggleQuizEnabled(UUID schoolId, UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId));
        if (!quiz.getSchoolId().equals(schoolId)) {
            throw new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId);
        }
        quiz.setIsEnabled(!Boolean.FALSE.equals(quiz.getIsEnabled()));
        return mapToDto(quizRepository.save(quiz));
    }

    @Transactional
    public void addQuizScoreToGrade(UUID schoolId, UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId));
        if (!quiz.getSchoolId().equals(schoolId)) {
            throw new com.schoolsaas.exception.ResourceNotFoundException("Quiz", "id", quizId);
        }
        if (quiz.getSubjectId() == null || quiz.getTermId() == null) {
            throw new com.schoolsaas.exception.BadRequestException("Quiz must have subject and term to add to grades");
        }
        List<QuizSubmission> subs = submissionRepository.findByQuizId(quizId);
        for (QuizSubmission sub : subs) {
            if (sub.getScore() == null || sub.getTotalMarks() == null) continue;
            Optional<Grade> existing = gradeRepository.findByStudentIdAndSubjectIdAndTermIdAndAssessmentType(
                    sub.getStudentId(), quiz.getSubjectId(), quiz.getTermId(), "QUIZ_" + quizId);
            if (existing.isPresent()) continue;
            Grade grade = new Grade();
            grade.setSchoolId(schoolId);
            grade.setStudentId(sub.getStudentId());
            grade.setSubjectId(quiz.getSubjectId());
            grade.setTermId(quiz.getTermId());
            grade.setAssessmentType("QUIZ_" + quizId);
            grade.setScore(sub.getScore());
            grade.setMaxScore(sub.getTotalMarks());
            BigDecimal pct = sub.getTotalMarks().compareTo(BigDecimal.ZERO) > 0
                    ? sub.getScore().multiply(new BigDecimal("100")).divide(sub.getTotalMarks(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            double p = pct.doubleValue();
            String gl = p >= 70 ? "A" : p >= 60 ? "B" : p >= 50 ? "C" : p >= 45 ? "D" : "F";
            grade.setGradeLetter(gl);
            grade.setRemarks("Auto-graded from " + (quiz.getQuizType() != null ? quiz.getQuizType() : "quiz") + ": " + quiz.getTitle());
            gradeRepository.save(grade);
        }
    }
}
