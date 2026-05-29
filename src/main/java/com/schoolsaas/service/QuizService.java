package com.schoolsaas.service;

import com.schoolsaas.dto.quiz.*;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Quiz quiz = Quiz.builder()
                .schoolId(schoolId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .subjectId(dto.getSubjectId())
                .classId(dto.getClassId())
                .durationMinutes(dto.getDurationMinutes())
                .totalMarks(dto.getTotalMarks())
                .passMark(dto.getPassMark())
                .shuffleQuestions(dto.getShuffleQuestions())
                .showResultsImmediately(dto.getShowResultsImmediately())
                .maxAttempts(dto.getMaxAttempts())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        quiz = quizRepository.save(quiz);

        int orderIndex = 0;
        for (QuizQuestionDto qDto : dto.getQuestions()) {
            QuizQuestion q = QuizQuestion.builder()
                    .quizId(quiz.getId())
                    .questionText(qDto.getQuestionText())
                    .questionType(qDto.getQuestionType())
                    .options(qDto.getOptions())
                    .correctAnswer(qDto.getCorrectAnswer())
                    .correctAnswers(qDto.getCorrectAnswers())
                    .marks(qDto.getMarks())
                    .orderIndex(orderIndex++)
                    .explanation(qDto.getExplanation())
                    .build();
            questionRepository.save(q);
        }

        // Notify students in the class
        if (quiz.getClassId() != null) {
            List<Student> students = studentRepository.findBySchoolIdAndClassId(schoolId, quiz.getClassId());
            for (Student s : students) {
                if (s.getUserId() != null) {
                    notificationService.sendNotification(s.getUserId(), schoolId, "New Quiz Available",
                            "A new quiz has been published: " + quiz.getTitle(), "ASSIGNMENT", quiz.getId());
                }
            }
        }

        return mapToDto(quiz);
    }

    public Page<QuizDto> listQuizzes(UUID schoolId, Pageable pageable) {
        return quizRepository.findBySchoolId(schoolId, pageable).map(this::mapToDto);
    }

    public QuizDto getQuiz(UUID quizId) {
        return quizRepository.findById(quizId).map(this::mapToDto).orElseThrow();
    }

    public QuizDto startQuiz(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();

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

        double totalScore = 0;
        double maxMarks = 0;
        List<QuizAnswerDto> answerDtos = new ArrayList<>();

        for (QuizQuestion question : questions) {
            maxMarks += question.getMarks();
            Map<String, Object> answerData = request.getAnswers().stream()
                    .filter(a -> question.getId().equals(UUID.fromString(a.get("questionId").toString())))
                    .findFirst().orElse(null);

            double marksObtained = 0;
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

            totalScore += marksObtained;

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

        double percentage = maxMarks > 0 ? (totalScore / maxMarks) * 100 : 0;
        String gradeLetter = percentage >= 70 ? "A" : percentage >= 60 ? "B" : percentage >= 50 ? "C" : percentage >= 45 ? "D" : "F";

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

    private boolean gradeQuestion(QuizQuestion question, String userAnswer, List<String> selectedOptions) {
        return switch (question.getQuestionType()) {
            case "MCQ", "TRUE_FALSE" ->
                    userAnswer != null && userAnswer.equalsIgnoreCase(question.getCorrectAnswer());
            case "FILL_BLANK" ->
                    userAnswer != null && userAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
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
