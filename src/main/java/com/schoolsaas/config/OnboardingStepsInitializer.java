package com.schoolsaas.config;

import com.schoolsaas.model.OnboardingStep;
import com.schoolsaas.repository.OnboardingStepRepository;
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
public class OnboardingStepsInitializer {

    private final OnboardingStepRepository onboardingStepRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedOnboardingStepsIfEmpty() {
        if (onboardingStepRepository.count() > 0) {
            return;
        }

        log.info("Onboarding steps table is empty. Seeding...");

        List<OnboardingStep> steps = List.of(
                OnboardingStep.builder().stepKey("dashboard_welcome").targetPage("/dashboard").targetSelector("[data-tour=\"welcome\"]").title("Welcome to School SaaS!").content("This is your dashboard. Here you can see an overview of everything happening in your school.").position("bottom").stepOrder(1).targetRoles(new String[]{"ADMIN", "TEACHER", "STUDENT"}).build(),
                OnboardingStep.builder().stepKey("dashboard_stats").targetPage("/dashboard").targetSelector("[data-tour=\"stats\"]").title("Quick Stats").content("These cards show important numbers at a glance: students, teachers, classes, and revenue.").position("bottom").stepOrder(2).targetRoles(new String[]{"ADMIN"}).build(),
                OnboardingStep.builder().stepKey("dashboard_actions").targetPage("/dashboard").targetSelector("[data-tour=\"actions\"]").title("Quick Actions").content("Use these buttons to quickly add students, teachers, create content, or view payments.").position("left").stepOrder(3).targetRoles(new String[]{"ADMIN"}).build(),
                OnboardingStep.builder().stepKey("students_page").targetPage("/students").targetSelector("[data-tour=\"students-table\"]").title("Student Management").content("View, search, and manage all students. Click \"Add Student\" to register a new student.").position("right").stepOrder(1).targetRoles(new String[]{"ADMIN", "TEACHER"}).build(),
                OnboardingStep.builder().stepKey("teachers_page").targetPage("/teachers").targetSelector("[data-tour=\"teachers-table\"]").title("Teacher Management").content("Manage your teaching staff. Add new teachers and track their details.").position("right").stepOrder(1).targetRoles(new String[]{"ADMIN"}).build(),
                OnboardingStep.builder().stepKey("cms_page").targetPage("/cms").targetSelector("[data-tour=\"cms-list\"]").title("Content Management").content("Create and manage lesson notes, assignments, videos, and files. Content goes through an approval workflow.").position("right").stepOrder(1).targetRoles(new String[]{"ADMIN", "TEACHER"}).build(),
                OnboardingStep.builder().stepKey("payments_page").targetPage("/payments").targetSelector("[data-tour=\"payments-table\"]").title("Fee & Payments").content("Track all fee payments. You can initiate new payments and verify transactions here.").position("right").stepOrder(1).targetRoles(new String[]{"ADMIN", "ACCOUNTANT"}).build(),
                OnboardingStep.builder().stepKey("analytics_page").targetPage("/analytics").targetSelector("[data-tour=\"analytics-charts\"]").title("Analytics Dashboard").content("Visualize school data with charts: revenue trends, enrollment, gender distribution, and class sizes.").position("top").stepOrder(1).targetRoles(new String[]{"ADMIN"}).build(),
                OnboardingStep.builder().stepKey("roles_page").targetPage("/roles").targetSelector("[data-tour=\"roles-list\"]").title("Role Management").content("Create custom roles and assign specific permissions to control what each user can do.").position("right").stepOrder(1).targetRoles(new String[]{"ADMIN"}).build(),
                OnboardingStep.builder().stepKey("student_results").targetPage("/student/results").targetSelector("[data-tour=\"results-table\"]").title("Your Results").content("View all your grades organized by term. See your scores, grade letters, and teacher remarks.").position("bottom").stepOrder(1).targetRoles(new String[]{"STUDENT"}).build(),
                OnboardingStep.builder().stepKey("student_fees").targetPage("/student/fees").targetSelector("[data-tour=\"fees-summary\"]").title("Fee Payments").content("Track your fee payments. See what you have paid and what is still pending.").position("bottom").stepOrder(1).targetRoles(new String[]{"STUDENT"}).build(),
                OnboardingStep.builder().stepKey("teacher_my_classes").targetPage("/teacher/my-classes").targetSelector("[data-tour=\"classes-grid\"]").title("My Classes").content("View all classes and subjects you teach. Click on a class to see students or enter grades.").position("bottom").stepOrder(1).targetRoles(new String[]{"TEACHER"}).build()
        );

        onboardingStepRepository.saveAll(steps);
        log.info("Seeded {} onboarding steps.", steps.size());
    }
}
