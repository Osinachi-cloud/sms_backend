package com.schoolsaas.config;

import com.schoolsaas.model.Permission;
import com.schoolsaas.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInitializer {

    private final PermissionRepository permissionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedPermissionsIfEmpty() {
        if (permissionRepository.count() > 0) {
            return;
        }

        log.info("Permissions table is empty. Seeding master permission list...");

        List<Permission> permissions = List.of(
                // Student
                Permission.builder().key("student.read").category("STUDENT").description("View student list and details").build(),
                Permission.builder().key("student.create").category("STUDENT").description("Create new student profile").build(),
                Permission.builder().key("student.update").category("STUDENT").description("Update student information").build(),
                Permission.builder().key("student.delete").category("STUDENT").description("Delete student (soft delete)").build(),
                Permission.builder().key("student.bulk.enroll").category("STUDENT").description("Bulk enrollment via Excel/CSV").build(),
                Permission.builder().key("student.grades.read").category("STUDENT").description("View student grades").build(),
                Permission.builder().key("student.grades.manage").category("STUDENT").description("Enter/edit student grades").build(),
                Permission.builder().key("student.attendance.read").category("STUDENT").description("View attendance records").build(),
                Permission.builder().key("student.attendance.manage").category("STUDENT").description("Mark/edit attendance").build(),
                Permission.builder().key("student.reports.generate").category("STUDENT").description("Generate student report cards").build(),
                // Teacher
                Permission.builder().key("teacher.read").category("TEACHER").description("View teachers").build(),
                Permission.builder().key("teacher.create").category("TEACHER").description("Create teacher profile").build(),
                Permission.builder().key("teacher.update").category("TEACHER").description("Update teacher information").build(),
                Permission.builder().key("teacher.delete").category("TEACHER").description("Delete teacher profile").build(),
                Permission.builder().key("teacher.assign.class").category("TEACHER").description("Assign teachers to classes").build(),
                Permission.builder().key("teacher.override").category("TEACHER").description("Temporarily grant class access").build(),
                Permission.builder().key("class.read").category("CLASS").description("View classes").build(),
                Permission.builder().key("class.create").category("CLASS").description("Create classes").build(),
                Permission.builder().key("class.update").category("CLASS").description("Update classes").build(),
                Permission.builder().key("class.delete").category("CLASS").description("Delete classes").build(),
                // CMS
                Permission.builder().key("cms.folder.read").category("CMS").description("View CMS folders").build(),
                Permission.builder().key("cms.folder.create").category("CMS").description("Create CMS folders").build(),
                Permission.builder().key("cms.folder.update").category("CMS").description("Update CMS folders").build(),
                Permission.builder().key("cms.folder.delete").category("CMS").description("Delete CMS folders").build(),
                Permission.builder().key("cms.content.read").category("CMS").description("View CMS content").build(),
                Permission.builder().key("cms.content.create").category("CMS").description("Create CMS content").build(),
                Permission.builder().key("cms.content.edit").category("CMS").description("Edit own content").build(),
                Permission.builder().key("cms.content.edit.any").category("CMS").description("Edit any content").build(),
                Permission.builder().key("cms.content.submit").category("CMS").description("Submit for approval").build(),
                Permission.builder().key("cms.content.approve").category("CMS").description("Approve content").build(),
                Permission.builder().key("cms.content.reject").category("CMS").description("Reject content").build(),
                Permission.builder().key("cms.content.delete").category("CMS").description("Delete content").build(),
                Permission.builder().key("cms.content.publish").category("CMS").description("Schedule/publish content").build(),
                // Finance
                Permission.builder().key("fee.read").category("FINANCE").description("View fee structure").build(),
                Permission.builder().key("fee.create").category("FINANCE").description("Create fees").build(),
                Permission.builder().key("fee.update").category("FINANCE").description("Update fees").build(),
                Permission.builder().key("fee.delete").category("FINANCE").description("Delete fees").build(),
                Permission.builder().key("payment.read").category("FINANCE").description("View payments").build(),
                Permission.builder().key("payment.create").category("FINANCE").description("Record payments").build(),
                Permission.builder().key("payment.track").category("FINANCE").description("Track payments").build(),
                Permission.builder().key("payment.report").category("FINANCE").description("Generate financial reports").build(),
                Permission.builder().key("payment.gateway.manage").category("FINANCE").description("Configure school payment gateways").build(),
                Permission.builder().key("payment.gateway.switch").category("FINANCE").description("Switch active payment gateway").build(),
                // Role
                Permission.builder().key("role.read").category("ROLE").description("View roles").build(),
                Permission.builder().key("role.create").category("ROLE").description("Create custom roles").build(),
                Permission.builder().key("role.update").category("ROLE").description("Update role permissions").build(),
                Permission.builder().key("role.delete").category("ROLE").description("Delete roles").build(),
                Permission.builder().key("permission.read").category("ROLE").description("View permissions").build(),
                Permission.builder().key("permission.assign").category("ROLE").description("Assign permissions to roles").build(),
                Permission.builder().key("user.role.assign").category("ROLE").description("Assign roles to users").build(),
                // Analytics
                Permission.builder().key("analytics.academic.view").category("ANALYTICS").description("View academic analytics").build(),
                Permission.builder().key("analytics.finance.view").category("ANALYTICS").description("View financial analytics").build(),
                Permission.builder().key("analytics.operations.view").category("ANALYTICS").description("View operational analytics").build(),
                Permission.builder().key("analytics.export").category("ANALYTICS").description("Export analytics data").build(),
                // School
                Permission.builder().key("school.read").category("SCHOOL").description("View school details").build(),
                Permission.builder().key("school.update").category("SCHOOL").description("Update school settings").build(),
                Permission.builder().key("school.users.read").category("SCHOOL").description("View school users").build(),
                Permission.builder().key("school.users.manage").category("SCHOOL").description("Manage school users").build(),
                Permission.builder().key("school.deletion.request").category("SCHOOL").description("Request school deletion").build(),
                // User Management
                Permission.builder().key("user.read").category("USER").description("View school users").build(),
                Permission.builder().key("user.create").category("USER").description("Create school users").build(),
                Permission.builder().key("user.delete").category("USER").description("Delete school users").build(),
                // Timetable
                Permission.builder().key("timetable.read").category("TIMETABLE").description("View timetable").build(),
                Permission.builder().key("timetable.create").category("TIMETABLE").description("Create timetable entries").build(),
                Permission.builder().key("timetable.update").category("TIMETABLE").description("Update timetable entries").build(),
                Permission.builder().key("timetable.delete").category("TIMETABLE").description("Delete timetable entries").build(),
                // Academic
                Permission.builder().key("session.read").category("ACADEMIC").description("View academic sessions").build(),
                Permission.builder().key("session.create").category("ACADEMIC").description("Create academic sessions").build(),
                Permission.builder().key("session.update").category("ACADEMIC").description("Update academic sessions").build(),
                Permission.builder().key("term.read").category("ACADEMIC").description("View terms").build(),
                Permission.builder().key("term.create").category("ACADEMIC").description("Create terms").build(),
                Permission.builder().key("term.update").category("ACADEMIC").description("Update terms").build(),
                Permission.builder().key("subject.read").category("ACADEMIC").description("View subjects").build(),
                Permission.builder().key("subject.create").category("ACADEMIC").description("Create subjects").build(),
                Permission.builder().key("subject.update").category("ACADEMIC").description("Update subjects").build()
        );

        permissionRepository.saveAll(permissions);
        log.info("Seeded {} permissions.", permissions.size());
    }
}
