package com.schoolsaas.config;

import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final AnnouncementRepository announcementRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BadgeRepository badgeRepository;
    private final IdCardTemplateRepository idCardTemplateRepository;
    private final ReportCardTemplateRepository reportCardTemplateRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final SubjectRepository subjectRepository;
    private final TermRepository termRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_PASSWORD = "password123";

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping...");
            return;
        }

        log.info("Seeding demo data...");
        String encodedPassword = passwordEncoder.encode(DEMO_PASSWORD);
        seedCoreData(encodedPassword);
        seedExtendedData(encodedPassword);
        log.info("Demo data seeded successfully!");
        log.info("===========================================");
        log.info("DEMO CREDENTIALS (password for all: {})", DEMO_PASSWORD);
        log.info("===========================================");
        log.info("Platform Admin: [EMAIL_REDACTED]");
        log.info("Greenfield Super Admin: [EMAIL_REDACTED]");
        log.info("Greenfield Admin: [EMAIL_REDACTED]");
        log.info("Greenfield Teacher: [EMAIL_REDACTED]");
        log.info("Greenfield Student: [EMAIL_REDACTED]");
        log.info("Sunrise Super Admin: [EMAIL_REDACTED]");
        log.info("===========================================");
    }

    private void seedCoreData(String encodedPassword) {
        // Platform Admin
        createUser("[EMAIL_REDACTED]", "Platform Admin", encodedPassword, true, "APP_ADMIN");
        createUser("[EMAIL_REDACTED]", "Platform Support", encodedPassword, true, "GENERAL_ADMIN");

        // Schools
        School greenfield = createSchool("Greenfield Academy", "greenfield", "GFA001", "[EMAIL_REDACTED]");
        School sunrise = createSchool("Sunrise International School", "sunrise", "SIS001", "[EMAIL_REDACTED]");

        // Roles
        Role gfSuperAdmin = createRole(greenfield.getId(), "SUPER_ADMIN", "Full access");
        Role gfAdmin = createRole(greenfield.getId(), "ADMIN", "School administration");
        Role gfTeacher = createRole(greenfield.getId(), "TEACHER", "Teaching staff");
        Role gfStudent = createRole(greenfield.getId(), "STUDENT", "Student access");
        Role gfAccountant = createRole(greenfield.getId(), "ACCOUNTANT", "Financial management");
        Role srSuperAdmin = createRole(sunrise.getId(), "SUPER_ADMIN", "Full access");
        Role srAdmin = createRole(sunrise.getId(), "ADMIN", "School administration");
        Role srTeacher = createRole(sunrise.getId(), "TEACHER", "Teaching staff");
        Role srStudent = createRole(sunrise.getId(), "STUDENT", "Student access");

        assignAllPermissions(gfSuperAdmin.getId());
        assignAllPermissions(srSuperAdmin.getId());
        assignAdminPermissions(gfAdmin.getId());
        assignAdminPermissions(srAdmin.getId());
        assignTeacherPermissions(gfTeacher.getId());
        assignTeacherPermissions(srTeacher.getId());
        assignStudentPermissions(gfStudent.getId());
        assignStudentPermissions(srStudent.getId());
        assignAccountantPermissions(gfAccountant.getId());

        // Greenfield Users
        User gfSuperAdminUser = createUser("[EMAIL_REDACTED]", "Chief Adebayo Okonkwo", encodedPassword, false, null);
        User gfAdminUser = createUser("[EMAIL_REDACTED]", "Mrs. Folake Adeleke", encodedPassword, false, null);
        User gfTeacher1 = createUser("[EMAIL_REDACTED]", "Mr. John Okafor", encodedPassword, false, null);
        User gfTeacher2 = createUser("[EMAIL_REDACTED]", "Mrs. Sarah Nwosu", encodedPassword, false, null);
        User gfAccountantUser = createUser("[EMAIL_REDACTED]", "Mr. Emeka Uzoma", encodedPassword, false, null);
        User gfStudent1 = createUser("[EMAIL_REDACTED]", "Ade Johnson", encodedPassword, false, null);
        User gfStudent2 = createUser("[EMAIL_REDACTED]", "Chioma Obi", encodedPassword, false, null);

        // Sunrise Users
        User srSuperAdminUser = createUser("[EMAIL_REDACTED]", "Dr. James Chen", encodedPassword, false, null);
        User srAdminUser = createUser("[EMAIL_REDACTED]", "Ms. Amara Williams", encodedPassword, false, null);
        User srTeacher1 = createUser("[EMAIL_REDACTED]", "Mrs. Mary Thompson", encodedPassword, false, null);
        User srStudent1 = createUser("[EMAIL_REDACTED]", "David Lee", encodedPassword, false, null);

        linkUserToSchool(gfSuperAdminUser, greenfield, gfSuperAdmin);
        linkUserToSchool(gfAdminUser, greenfield, gfAdmin);
        linkUserToSchool(gfTeacher1, greenfield, gfTeacher);
        linkUserToSchool(gfTeacher2, greenfield, gfTeacher);
        linkUserToSchool(gfAccountantUser, greenfield, gfAccountant);
        linkUserToSchool(gfStudent1, greenfield, gfStudent);
        linkUserToSchool(gfStudent2, greenfield, gfStudent);
        linkUserToSchool(srSuperAdminUser, sunrise, srSuperAdmin);
        linkUserToSchool(srAdminUser, sunrise, srAdmin);
        linkUserToSchool(srTeacher1, sunrise, srTeacher);
        linkUserToSchool(srStudent1, sunrise, srStudent);

        // Classes
        SchoolClass gfJss1 = createClass(greenfield.getId(), "JSS 1", 7, "A", 35);
        SchoolClass gfJss2 = createClass(greenfield.getId(), "JSS 2", 8, "A", 35);
        SchoolClass gfSs1 = createClass(greenfield.getId(), "SS 1", 10, "A", 30);
        SchoolClass srGrade9 = createClass(sunrise.getId(), "Grade 9", 9, null, 25);
        SchoolClass srGrade10 = createClass(sunrise.getId(), "Grade 10", 10, null, 25);

        // Teachers
        createTeacher(greenfield.getId(), gfTeacher1.getId(), "GFA-T001", "Mr. John Okafor", "Mathematics");
        createTeacher(greenfield.getId(), gfTeacher2.getId(), "GFA-T002", "Mrs. Sarah Nwosu", "English");
        createTeacher(sunrise.getId(), srTeacher1.getId(), "SIS-T001", "Mrs. Mary Thompson", "Science");

        // Students
        createStudent(greenfield.getId(), gfStudent1.getId(), "GFA/2023/001", "Ade Johnson", gfJss2.getId());
        createStudent(greenfield.getId(), gfStudent2.getId(), "GFA/2022/001", "Chioma Obi", gfSs1.getId());
        createStudent(greenfield.getId(), null, "GFA/2023/002", "Blessing Eze", gfJss2.getId());
        createStudent(greenfield.getId(), null, "GFA/2023/003", "Chinedu Okoro", gfJss2.getId());
        createStudent(greenfield.getId(), null, "GFA/2024/001", "Tunde Afolabi", gfJss1.getId());
        createStudent(sunrise.getId(), srStudent1.getId(), "SIS/2023/001", "David Lee", srGrade10.getId());
        createStudent(sunrise.getId(), null, "SIS/2023/002", "Sofia Martinez", srGrade10.getId());
    }

    private void seedExtendedData(String encodedPassword) {
        List<School> schools = schoolRepository.findAll();
        if (schools.isEmpty()) return;
        School greenfield = schools.stream().filter(s -> s.getName().contains("Greenfield")).findFirst().orElse(schools.get(0));
        School sunrise = schools.stream().filter(s -> s.getName().contains("Sunrise")).findFirst().orElse(schools.get(0));

        List<Student> students = studentRepository.findBySchoolId(greenfield.getId());
        if (students.isEmpty()) return;
        Student student1 = students.get(0);

        List<User> users = userRepository.findAll();
        User teacherUser = users.stream().filter(u -> u.getFullName().contains("John Okafor")).findFirst().orElse(users.get(0));
        User studentUser = users.stream().filter(u -> u.getFullName().contains("Ade Johnson")).findFirst().orElse(users.get(0));

        // Events
        createEvent(greenfield.getId(), "First Term Exam", "End of term examinations", "EXAM", LocalDateTime.now().plusDays(7), null, "School Hall", teacherUser.getId());
        createEvent(greenfield.getId(), "Sports Day", "Annual inter-house sports", "SPORTS", LocalDateTime.now().plusDays(14), null, "School Field", teacherUser.getId());
        createEvent(greenfield.getId(), "Parent Meeting", "Discuss student progress", "MEETING", LocalDateTime.now().plusDays(3), null, "Conference Room", teacherUser.getId());

        // Announcements
        createAnnouncement(greenfield.getId(), "Exam Timetable Released", "The first term examination timetable has been released.", "ALL", "HIGH", true, teacherUser.getId());
        createAnnouncement(greenfield.getId(), "Fee Payment Reminder", "Second term fees are due by end of month.", "PARENTS", "URGENT", true, teacherUser.getId());
        createAnnouncement(greenfield.getId(), "New Library Books", "Over 50 new books added to the digital library.", "STUDENTS", "NORMAL", false, teacherUser.getId());

        // Subjects
        Subject math = createSubject(greenfield.getId(), "Mathematics", "MATH");
        Subject english = createSubject(greenfield.getId(), "English", "ENG");
        Subject science = createSubject(greenfield.getId(), "Science", "SCI");

        // Terms
        Term term1 = createTerm(greenfield.getId(), null, "First Term 2025/2026", LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(2));

        // Grades
        createGrade(greenfield.getId(), student1.getId(), math.getId(), term1.getId(), new BigDecimal("73"), new BigDecimal("100"), "B", "Good");
        createGrade(greenfield.getId(), student1.getId(), english.getId(), term1.getId(), new BigDecimal("82"), new BigDecimal("100"), "A", "Excellent");
        createGrade(greenfield.getId(), student1.getId(), science.getId(), term1.getId(), new BigDecimal("65"), new BigDecimal("100"), "C", "Fair");

        // Attendance
        createAttendance(greenfield.getId(), student1.getId(), student1.getClassId(), LocalDate.now().minusDays(1), "PRESENT", teacherUser.getId());
        createAttendance(greenfield.getId(), student1.getId(), student1.getClassId(), LocalDate.now().minusDays(2), "PRESENT", teacherUser.getId());
        createAttendance(greenfield.getId(), student1.getId(), student1.getClassId(), LocalDate.now().minusDays(3), "ABSENT", teacherUser.getId());
        createAttendance(greenfield.getId(), student1.getId(), student1.getClassId(), LocalDate.now().minusDays(4), "LATE", teacherUser.getId());

        // Quizzes
        Quiz quiz1 = createQuiz(greenfield.getId(), "Mathematics - Algebra Basics", "Test your understanding of basic algebra", math.getId(), student1.getClassId(), 30, new BigDecimal("50.0"), new BigDecimal("25.0"), teacherUser.getId());
        createQuizQuestion(quiz1.getId(), "What is x in 2x + 5 = 15?", "MCQ", List.of(Map.of("label", "5", "value", "5"), Map.of("label", "10", "value", "10"), Map.of("label", "15", "value", "15")), "5", new BigDecimal("10.0"));
        createQuizQuestion(quiz1.getId(), "Solve: 3(x - 2) = 12", "MCQ", List.of(Map.of("label", "4", "value", "4"), Map.of("label", "6", "value", "6"), Map.of("label", "8", "value", "8")), "6", new BigDecimal("10.0"));
        createQuizQuestion(quiz1.getId(), "A sentence must always end with a full stop.", "TRUE_FALSE", List.of(), "false", new BigDecimal("10.0"));

        // Library Books
        BookCategory mathCat = createBookCategory(greenfield.getId(), "Mathematics");
        BookCategory engCat = createBookCategory(greenfield.getId(), "English");
        createLibraryBook(greenfield.getId(), mathCat.getId(), "Introduction to Algebra", "Dr. K. O. Adeyemi", "978-1234567890", "PDF", 5);
        createLibraryBook(greenfield.getId(), engCat.getId(), "English Grammar for Schools", "J. Smith & Co.", "978-0987654321", "PDF", 3);

        // Badges
        createBadge(greenfield.getId(), "Perfect Attendance", "Attended all classes for a full term", "#FFD700", "ATTENDANCE_STREAK", 50);
        createBadge(greenfield.getId(), "Math Whiz", "Scored 90+ in Mathematics", "#6366F1", "GRADE_AVERAGE", 30);
        createBadge(greenfield.getId(), "Quiz Master", "Completed 10 quizzes with 80%+", "#10B981", "QUIZ_SCORE", 40);

        // ID Card Template
        createIdCardTemplate(greenfield.getId(), "Standard Student ID", teacherUser.getId());

        // Report Card Template
        createReportCardTemplate(greenfield.getId(), "Standard Report Card", teacherUser.getId());

        // Parents
        User parentUser = createUser("parent1@example.com", "Mr. Robert Johnson", encodedPassword, false, null);
        Parent parent = createParent(greenfield.getId(), parentUser.getId(), "Mr. Robert Johnson", "parent1@example.com", "+2348012345678", "FATHER");
        linkParentToStudent(parent.getId(), student1.getId(), true);

        // Conversations and Messages
        Conversation conv = createConversation(greenfield.getId(), "Mr. John Okafor - Parent", "DIRECT", teacherUser.getId());
        addParticipant(conv.getId(), teacherUser.getId());
        addParticipant(conv.getId(), parentUser.getId());
        createMessage(conv.getId(), teacherUser.getId(), "Hello! I wanted to check on Ade's progress in Mathematics.");
        createMessage(conv.getId(), parentUser.getId(), "He is doing well. His last test score was 85%.");
        createMessage(conv.getId(), teacherUser.getId(), "That's great to hear! Please submit the assignment by Friday.");
    }

    private User createUser(String email, String fullName, String encodedPassword, boolean isPlatformAdmin, String platformRole) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(encodedPassword);
        user.setIsPlatformAdmin(isPlatformAdmin);
        user.setPlatformRole(platformRole);
        user.setEmailVerified(true);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private School createSchool(String name, String subdomain, String code, String email) {
        School school = new School();
        school.setName(name);
        school.setSubdomain(subdomain);
        school.setCode(code);
        school.setEmail(email);
        school.setStatus("ACTIVE");
        school.setConfig(new HashMap<>());
        return schoolRepository.save(school);
    }

    private Role createRole(UUID schoolId, String name, String description) {
        Role role = new Role();
        role.setSchoolId(schoolId);
        role.setName(name);
        role.setDescription(description);
        role.setIsSystemRole(true);
        role.setIsActive(true);
        return roleRepository.save(role);
    }

    private void linkUserToSchool(User user, School school, Role role) {
        UserSchool userSchool = new UserSchool();
        userSchool.setUserId(user.getId());
        userSchool.setSchoolId(school.getId());
        userSchool.setRoleId(role.getId());
        userSchool.setIsActive(true);
        userSchool.setJoinedAt(LocalDateTime.now());
        userSchoolRepository.save(userSchool);
    }

    private SchoolClass createClass(UUID schoolId, String name, int gradeLevel, String section, int capacity) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSchoolId(schoolId);
        schoolClass.setName(name);
        schoolClass.setGradeLevel(gradeLevel);
        schoolClass.setSection(section);
        schoolClass.setCapacity(capacity);
        schoolClass.setIsActive(true);
        return classRepository.save(schoolClass);
    }

    private Teacher createTeacher(UUID schoolId, UUID userId, String employeeId, String fullName, String specialization) {
        Teacher teacher = new Teacher();
        teacher.setSchoolId(schoolId);
        teacher.setUserId(userId);
        teacher.setEmployeeId(employeeId);
        teacher.setFullName(fullName);
        teacher.setSpecialization(specialization);
        teacher.setStatus("ACTIVE");
        teacher.setDateOfJoining(LocalDate.now().minusYears(2));
        return teacherRepository.save(teacher);
    }

    private Student createStudent(UUID schoolId, UUID userId, String admissionNumber, String fullName, UUID classId) {
        Student student = new Student();
        student.setSchoolId(schoolId);
        student.setUserId(userId);
        student.setAdmissionNumber(admissionNumber);
        student.setFullName(fullName);
        student.setClassId(classId);
        student.setStatus("ACTIVE");
        student.setAdmissionDate(LocalDate.now().minusMonths(6));
        return studentRepository.save(student);
    }

    private void assignAllPermissions(UUID roleId) {
        permissionRepository.findAll().forEach(permission -> {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionKey(permission.getKey());
            rolePermissionRepository.save(rp);
        });
    }

    private void assignAdminPermissions(UUID roleId) {
        List<String> adminKeys = List.of(
                "student.read", "student.create", "student.update", "student.delete", "student.bulk.enroll",
                "student.grades.read", "student.grades.manage", "student.attendance.read", "student.attendance.manage",
                "teacher.read", "teacher.create", "teacher.update", "teacher.delete", "teacher.assign.class",
                "class.read", "class.create", "class.update", "class.delete",
                "cms.folder.read", "cms.folder.create", "cms.content.read", "cms.content.approve", "cms.content.publish",
                "fee.read", "fee.create", "fee.update", "payment.read", "payment.create",
                "analytics.academic.view", "analytics.finance.view", "school.read", "school.update"
        );
        assignPermissions(roleId, adminKeys);
    }

    private void assignTeacherPermissions(UUID roleId) {
        List<String> teacherKeys = List.of(
                "student.read", "student.grades.read", "student.grades.manage",
                "student.attendance.read", "student.attendance.manage",
                "class.read", "cms.folder.read", "cms.content.read", "cms.content.create",
                "cms.content.edit", "cms.content.submit", "subject.read"
        );
        assignPermissions(roleId, teacherKeys);
    }

    private void assignStudentPermissions(UUID roleId) {
        List<String> studentKeys = List.of(
                "student.grades.read", "student.attendance.read",
                "cms.content.read", "fee.read", "payment.read"
        );
        assignPermissions(roleId, studentKeys);
    }

    private void assignAccountantPermissions(UUID roleId) {
        List<String> accountantKeys = List.of(
                "student.read", "fee.read", "fee.create", "fee.update",
                "payment.read", "payment.create", "payment.track", "payment.report",
                "payment.gateway.manage",
                "analytics.finance.view"
        );
        assignPermissions(roleId, accountantKeys);
    }

    private void assignPermissions(UUID roleId, List<String> permissionKeys) {
        permissionKeys.forEach(key -> {
            if (permissionRepository.existsByKey(key)) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionKey(key);
                rolePermissionRepository.save(rp);
            }
        });
    }

    // Extended data helpers
    private Event createEvent(UUID schoolId, String title, String description, String eventType, LocalDateTime startDate, LocalDateTime endDate, String location, UUID createdBy) {
        Event event = new Event();
        event.setSchoolId(schoolId);
        event.setTitle(title);
        event.setDescription(description);
        event.setEventType(eventType);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setLocation(location);
        event.setCreatedBy(createdBy);
        return eventRepository.save(event);
    }

    private Announcement createAnnouncement(UUID schoolId, String title, String content, String targetAudience, String priority, boolean isPinned, UUID createdBy) {
        Announcement announcement = new Announcement();
        announcement.setSchoolId(schoolId);
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setTargetAudience(targetAudience);
        announcement.setPriority(priority);
        announcement.setIsPinned(isPinned);
        announcement.setCreatedBy(createdBy);
        return announcementRepository.save(announcement);
    }

    private Subject createSubject(UUID schoolId, String name, String code) {
        Subject subject = new Subject();
        subject.setSchoolId(schoolId);
        subject.setName(name);
        subject.setCode(code);
        subject.setIsActive(true);
        return subjectRepository.save(subject);
    }

    private Term createTerm(UUID schoolId, UUID sessionId, String name, LocalDate startDate, LocalDate endDate) {
        Term term = new Term();
        term.setSchoolId(schoolId);
        term.setSessionId(sessionId);
        term.setName(name);
        term.setStartDate(startDate);
        term.setEndDate(endDate);
        term.setIsCurrent(true);
        return termRepository.save(term);
    }

    private Grade createGrade(UUID schoolId, UUID studentId, UUID subjectId, UUID termId, BigDecimal score, BigDecimal maxScore, String gradeLetter, String remarks) {
        Grade grade = new Grade();
        grade.setSchoolId(schoolId);
        grade.setStudentId(studentId);
        grade.setSubjectId(subjectId);
        grade.setTermId(termId);
        grade.setScore(score);
        grade.setMaxScore(maxScore);
        grade.setGradeLetter(gradeLetter);
        grade.setRemarks(remarks);
        grade.setAssessmentType("EXAM");
        return gradeRepository.save(grade);
    }

    private Attendance createAttendance(UUID schoolId, UUID studentId, UUID classId, LocalDate date, String status, UUID markedBy) {
        Attendance attendance = new Attendance();
        attendance.setSchoolId(schoolId);
        attendance.setStudentId(studentId);
        attendance.setClassId(classId);
        attendance.setDate(date);
        attendance.setStatus(status);
        attendance.setMarkedBy(markedBy);
        return attendanceRepository.save(attendance);
    }

    private Quiz createQuiz(UUID schoolId, String title, String description, UUID subjectId, UUID classId, int duration, BigDecimal totalMarks, BigDecimal passMark, UUID createdBy) {
        Quiz quiz = new Quiz();
        quiz.setSchoolId(schoolId);
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setSubjectId(subjectId);
        quiz.setClassId(classId);
        quiz.setDurationMinutes(duration);
        quiz.setTotalMarks(totalMarks);
        quiz.setPassMark(passMark);
        quiz.setCreatedBy(createdBy);
        quiz.setStatus("PUBLISHED");
        return quizRepository.save(quiz);
    }

    @SuppressWarnings("unchecked")
    private QuizQuestion createQuizQuestion(UUID quizId, String questionText, String questionType, List<Map<String, Object>> options, String correctAnswer, BigDecimal marks) {
        QuizQuestion question = new QuizQuestion();
        question.setQuizId(quizId);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setOptions(options);
        question.setCorrectAnswer(correctAnswer);
        question.setMarks(marks);
        question.setOrderIndex(0);
        return quizQuestionRepository.save(question);
    }

    private BookCategory createBookCategory(UUID schoolId, String name) {
        BookCategory category = new BookCategory();
        category.setSchoolId(schoolId);
        category.setName(name);
        return bookCategoryRepository.save(category);
    }

    private LibraryBook createLibraryBook(UUID schoolId, UUID categoryId, String title, String author, String isbn, String fileType, int copies) {
        LibraryBook book = new LibraryBook();
        book.setSchoolId(schoolId);
        book.setCategoryId(categoryId);
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setFileType(fileType);
        book.setTotalCopies(copies);
        book.setAvailableCopies(copies);
        book.setIsDigital(true);
        book.setIsActive(true);
        return libraryBookRepository.save(book);
    }

    private Badge createBadge(UUID schoolId, String name, String description, String color, String criteriaType, int pointsValue) {
        Badge badge = new Badge();
        badge.setSchoolId(schoolId);
        badge.setName(name);
        badge.setDescription(description);
        badge.setColor(color);
        badge.setCriteriaType(criteriaType);
        badge.setPointsValue(pointsValue);
        badge.setIsActive(true);
        return badgeRepository.save(badge);
    }

    private IdCardTemplate createIdCardTemplate(UUID schoolId, String name, UUID createdBy) {
        IdCardTemplate template = new IdCardTemplate();
        template.setSchoolId(schoolId);
        template.setName(name);
        template.setIsDefault(true);
        template.setIsActive(true);
        template.setCreatedBy(createdBy);
        return idCardTemplateRepository.save(template);
    }

    private ReportCardTemplate createReportCardTemplate(UUID schoolId, String name, UUID createdBy) {
        ReportCardTemplate template = new ReportCardTemplate();
        template.setSchoolId(schoolId);
        template.setName(name);
        template.setIsDefault(true);
        template.setIsActive(true);
        template.setCreatedBy(createdBy);
        return reportCardTemplateRepository.save(template);
    }

    private Parent createParent(UUID schoolId, UUID userId, String fullName, String email, String phone, String relationship) {
        Parent parent = new Parent();
        parent.setSchoolId(schoolId);
        parent.setUserId(userId);
        parent.setFullName(fullName);
        parent.setEmail(email);
        parent.setPhone(phone);
        parent.setRelationship(relationship);
        parent.setIsActive(true);
        return parentRepository.save(parent);
    }

    private void linkParentToStudent(UUID parentId, UUID studentId, boolean isPrimary) {
        ParentStudent ps = new ParentStudent();
        ps.setParentId(parentId);
        ps.setStudentId(studentId);
        ps.setIsPrimary(isPrimary);
        parentStudentRepository.save(ps);
    }

    private Conversation createConversation(UUID schoolId, String title, String type, UUID createdBy) {
        Conversation conv = new Conversation();
        conv.setSchoolId(schoolId);
        conv.setTitle(title);
        conv.setType(type);
        conv.setCreatedBy(createdBy);
        return conversationRepository.save(conv);
    }

    private void addParticipant(UUID conversationId, UUID userId) {
        ConversationParticipant cp = new ConversationParticipant();
        cp.setConversationId(conversationId);
        cp.setUserId(userId);
        conversationParticipantRepository.save(cp);
    }

    private Message createMessage(UUID conversationId, UUID senderId, String content) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setMessageType("TEXT");
        return messageRepository.save(msg);
    }
}
