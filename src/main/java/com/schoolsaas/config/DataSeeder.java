package com.schoolsaas.config;

import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds demo data for development/testing.
 * Only runs when "dev" profile is active.
 * DELETE THIS CLASS before going to production.
 */
@Slf4j
@Component
@Profile("dev")
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
    private final PasswordEncoder passwordEncoder;

    // Single password for all demo users
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

        // Create Platform Admin
        User platformAdmin = createUser("admin@schoolsaas.com", "Platform Admin", encodedPassword, true, "APP_ADMIN");
        User platformSupport = createUser("support@schoolsaas.com", "Platform Support", encodedPassword, true, "GENERAL_ADMIN");

        // Create Schools
        School greenfield = createSchool("Greenfield Academy", "greenfield", "GFA001", "info@greenfield.edu");
        School sunrise = createSchool("Sunrise International School", "sunrise", "SIS001", "info@sunrise.edu");

        // Create Roles for Greenfield
        Role gfSuperAdmin = createRole(greenfield.getId(), "SUPER_ADMIN", "Full access to all school features");
        Role gfAdmin = createRole(greenfield.getId(), "ADMIN", "School administration");
        Role gfTeacher = createRole(greenfield.getId(), "TEACHER", "Teaching staff");
        Role gfStudent = createRole(greenfield.getId(), "STUDENT", "Student access");
        Role gfAccountant = createRole(greenfield.getId(), "ACCOUNTANT", "Financial management");

        // Create Roles for Sunrise
        Role srSuperAdmin = createRole(sunrise.getId(), "SUPER_ADMIN", "Full access to all school features");
        Role srAdmin = createRole(sunrise.getId(), "ADMIN", "School administration");
        Role srTeacher = createRole(sunrise.getId(), "TEACHER", "Teaching staff");
        Role srStudent = createRole(sunrise.getId(), "STUDENT", "Student access");

        // Assign all permissions to Super Admin roles
        assignAllPermissions(gfSuperAdmin.getId());
        assignAllPermissions(srSuperAdmin.getId());

        // Assign limited permissions to other roles
        assignAdminPermissions(gfAdmin.getId());
        assignAdminPermissions(srAdmin.getId());
        assignTeacherPermissions(gfTeacher.getId());
        assignTeacherPermissions(srTeacher.getId());
        assignStudentPermissions(gfStudent.getId());
        assignStudentPermissions(srStudent.getId());
        assignAccountantPermissions(gfAccountant.getId());

        // Create Greenfield Users
        User gfSuperAdminUser = createUser("superadmin@greenfield.edu", "Chief Adebayo Okonkwo", encodedPassword, false, null);
        User gfAdminUser = createUser("admin@greenfield.edu", "Mrs. Folake Adeleke", encodedPassword, false, null);
        User gfTeacher1 = createUser("john.math@greenfield.edu", "Mr. John Okafor", encodedPassword, false, null);
        User gfTeacher2 = createUser("sarah.english@greenfield.edu", "Mrs. Sarah Nwosu", encodedPassword, false, null);
        User gfAccountantUser = createUser("finance@greenfield.edu", "Mr. Emeka Uzoma", encodedPassword, false, null);
        User gfStudent1 = createUser("ade.johnson@greenfield.edu", "Ade Johnson", encodedPassword, false, null);
        User gfStudent2 = createUser("chioma.obi@greenfield.edu", "Chioma Obi", encodedPassword, false, null);

        // Create Sunrise Users
        User srSuperAdminUser = createUser("superadmin@sunrise.edu", "Dr. James Chen", encodedPassword, false, null);
        User srAdminUser = createUser("admin@sunrise.edu", "Ms. Amara Williams", encodedPassword, false, null);
        User srTeacher1 = createUser("mary.science@sunrise.edu", "Mrs. Mary Thompson", encodedPassword, false, null);
        User srStudent1 = createUser("david.lee@sunrise.edu", "David Lee", encodedPassword, false, null);

        // Link users to schools with roles
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

        // Create Classes for Greenfield
        SchoolClass gfJss1 = createClass(greenfield.getId(), "JSS 1", 7, "A", 35);
        SchoolClass gfJss2 = createClass(greenfield.getId(), "JSS 2", 8, "A", 35);
        SchoolClass gfSs1 = createClass(greenfield.getId(), "SS 1", 10, "A", 30);

        // Create Classes for Sunrise
        SchoolClass srGrade9 = createClass(sunrise.getId(), "Grade 9", 9, null, 25);
        SchoolClass srGrade10 = createClass(sunrise.getId(), "Grade 10", 10, null, 25);

        // Create Teachers
        Teacher teacher1 = createTeacher(greenfield.getId(), gfTeacher1.getId(), "GFA-T001", "Mr. John Okafor", "Mathematics");
        Teacher teacher2 = createTeacher(greenfield.getId(), gfTeacher2.getId(), "GFA-T002", "Mrs. Sarah Nwosu", "English");
        Teacher teacher3 = createTeacher(sunrise.getId(), srTeacher1.getId(), "SIS-T001", "Mrs. Mary Thompson", "Science");

        // Create Students
        createStudent(greenfield.getId(), gfStudent1.getId(), "GFA/2023/001", "Ade Johnson", gfJss2.getId());
        createStudent(greenfield.getId(), gfStudent2.getId(), "GFA/2022/001", "Chioma Obi", gfSs1.getId());
        createStudent(greenfield.getId(), null, "GFA/2023/002", "Blessing Eze", gfJss2.getId());
        createStudent(greenfield.getId(), null, "GFA/2023/003", "Chinedu Okoro", gfJss2.getId());
        createStudent(greenfield.getId(), null, "GFA/2024/001", "Tunde Afolabi", gfJss1.getId());

        createStudent(sunrise.getId(), srStudent1.getId(), "SIS/2023/001", "David Lee", srGrade10.getId());
        createStudent(sunrise.getId(), null, "SIS/2023/002", "Sofia Martinez", srGrade10.getId());

        log.info("Demo data seeded successfully!");
        log.info("===========================================");
        log.info("DEMO CREDENTIALS (password for all: {})", DEMO_PASSWORD);
        log.info("===========================================");
        log.info("Platform Admin: admin@schoolsaas.com");
        log.info("Greenfield Super Admin: superadmin@greenfield.edu");
        log.info("Greenfield Admin: admin@greenfield.edu");
        log.info("Greenfield Teacher: john.math@greenfield.edu");
        log.info("Greenfield Student: ade.johnson@greenfield.edu");
        log.info("Sunrise Super Admin: superadmin@sunrise.edu");
        log.info("===========================================");
    }

    private User createUser(String email, String fullName, String encodedPassword, boolean isPlatformAdmin, String platformRole) {
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
}
