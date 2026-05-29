-- =====================================================
-- Seed Onboarding Steps for Smart Tooltip System
-- =====================================================

INSERT INTO onboarding_steps (step_key, target_page, target_selector, title, content, position, step_order, target_roles) VALUES
('dashboard_welcome', '/dashboard', '[data-tour="welcome"]', 'Welcome to School SaaS!', 'This is your dashboard. Here you can see an overview of everything happening in your school.', 'bottom', 1, ARRAY['ADMIN', 'TEACHER', 'STUDENT']),
('dashboard_stats', '/dashboard', '[data-tour="stats"]', 'Quick Stats', 'These cards show important numbers at a glance: students, teachers, classes, and revenue.', 'bottom', 2, ARRAY['ADMIN']),
('dashboard_actions', '/dashboard', '[data-tour="actions"]', 'Quick Actions', 'Use these buttons to quickly add students, teachers, create content, or view payments.', 'left', 3, ARRAY['ADMIN']),
('students_page', '/students', '[data-tour="students-table"]', 'Student Management', 'View, search, and manage all students. Click "Add Student" to register a new student.', 'right', 1, ARRAY['ADMIN', 'TEACHER']),
('teachers_page', '/teachers', '[data-tour="teachers-table"]', 'Teacher Management', 'Manage your teaching staff. Add new teachers and track their details.', 'right', 1, ARRAY['ADMIN']),
('cms_page', '/cms', '[data-tour="cms-list"]', 'Content Management', 'Create and manage lesson notes, assignments, videos, and files. Content goes through an approval workflow.', 'right', 1, ARRAY['ADMIN', 'TEACHER']),
('payments_page', '/payments', '[data-tour="payments-table"]', 'Fee & Payments', 'Track all fee payments. You can initiate new payments and verify Paystack transactions here.', 'right', 1, ARRAY['ADMIN', 'ACCOUNTANT']),
('analytics_page', '/analytics', '[data-tour="analytics-charts"]', 'Analytics Dashboard', 'Visualize school data with charts: revenue trends, enrollment, gender distribution, and class sizes.', 'top', 1, ARRAY['ADMIN']),
('roles_page', '/roles', '[data-tour="roles-list"]', 'Role Management', 'Create custom roles and assign specific permissions to control what each user can do.', 'right', 1, ARRAY['ADMIN']),
('student_results', '/student/results', '[data-tour="results-table"]', 'Your Results', 'View all your grades organized by term. See your scores, grade letters, and teacher remarks.', 'bottom', 1, ARRAY['STUDENT']),
('student_fees', '/student/fees', '[data-tour="fees-summary"]', 'Fee Payments', 'Track your fee payments. See what you have paid and what is still pending.', 'bottom', 1, ARRAY['STUDENT']),
('teacher_my_classes', '/teacher/my-classes', '[data-tour="classes-grid"]', 'My Classes', 'View all classes and subjects you teach. Click on a class to see students or enter grades.', 'bottom', 1, ARRAY['TEACHER']);
