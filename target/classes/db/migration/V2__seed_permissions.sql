-- =====================================================
-- Seed Data: Master Permission List
-- =====================================================

-- Student Management Permissions
INSERT INTO permissions (key, category, description) VALUES
('student.read', 'STUDENT', 'View student list and details'),
('student.create', 'STUDENT', 'Create new student profile'),
('student.update', 'STUDENT', 'Update student information'),
('student.delete', 'STUDENT', 'Delete student (soft delete)'),
('student.bulk.enroll', 'STUDENT', 'Bulk enrollment via Excel/CSV'),
('student.grades.read', 'STUDENT', 'View student grades'),
('student.grades.manage', 'STUDENT', 'Enter/edit student grades'),
('student.attendance.read', 'STUDENT', 'View attendance records'),
('student.attendance.manage', 'STUDENT', 'Mark/edit attendance'),
('student.reports.generate', 'STUDENT', 'Generate student report cards')
ON CONFLICT (key) DO NOTHING;

-- Teacher & Class Management Permissions
INSERT INTO permissions (key, category, description) VALUES
('teacher.read', 'TEACHER', 'View teachers'),
('teacher.create', 'TEACHER', 'Create teacher profile'),
('teacher.update', 'TEACHER', 'Update teacher information'),
('teacher.delete', 'TEACHER', 'Delete teacher profile'),
('teacher.assign.class', 'TEACHER', 'Assign teachers to classes'),
('teacher.override', 'TEACHER', 'Temporarily grant class access'),
('class.read', 'CLASS', 'View classes'),
('class.create', 'CLASS', 'Create classes'),
('class.update', 'CLASS', 'Update classes'),
('class.delete', 'CLASS', 'Delete classes')
ON CONFLICT (key) DO NOTHING;

-- CMS Permissions
INSERT INTO permissions (key, category, description) VALUES
('cms.folder.read', 'CMS', 'View CMS folders'),
('cms.folder.create', 'CMS', 'Create CMS folders'),
('cms.folder.update', 'CMS', 'Update CMS folders'),
('cms.folder.delete', 'CMS', 'Delete CMS folders'),
('cms.content.read', 'CMS', 'View CMS content'),
('cms.content.create', 'CMS', 'Create CMS content'),
('cms.content.edit', 'CMS', 'Edit own content'),
('cms.content.edit.any', 'CMS', 'Edit any content'),
('cms.content.submit', 'CMS', 'Submit for approval'),
('cms.content.approve', 'CMS', 'Approve content'),
('cms.content.reject', 'CMS', 'Reject content'),
('cms.content.delete', 'CMS', 'Delete content'),
('cms.content.publish', 'CMS', 'Schedule/publish content')
ON CONFLICT (key) DO NOTHING;

-- Finance Permissions
INSERT INTO permissions (key, category, description) VALUES
('fee.read', 'FINANCE', 'View fee structure'),
('fee.create', 'FINANCE', 'Create fees'),
('fee.update', 'FINANCE', 'Update fees'),
('fee.delete', 'FINANCE', 'Delete fees'),
('payment.read', 'FINANCE', 'View payments'),
('payment.create', 'FINANCE', 'Record payments'),
('payment.track', 'FINANCE', 'Track payments'),
('payment.report', 'FINANCE', 'Generate financial reports')
ON CONFLICT (key) DO NOTHING;

-- Role & Permission Management
INSERT INTO permissions (key, category, description) VALUES
('role.read', 'ROLE', 'View roles'),
('role.create', 'ROLE', 'Create custom roles'),
('role.update', 'ROLE', 'Update role permissions'),
('role.delete', 'ROLE', 'Delete roles'),
('permission.read', 'ROLE', 'View permissions'),
('permission.assign', 'ROLE', 'Assign permissions to roles'),
('user.role.assign', 'ROLE', 'Assign roles to users')
ON CONFLICT (key) DO NOTHING;

-- Analytics Permissions
INSERT INTO permissions (key, category, description) VALUES
('analytics.academic.view', 'ANALYTICS', 'View academic analytics'),
('analytics.finance.view', 'ANALYTICS', 'View financial analytics'),
('analytics.operations.view', 'ANALYTICS', 'View operational analytics'),
('analytics.export', 'ANALYTICS', 'Export analytics data')
ON CONFLICT (key) DO NOTHING;

-- School Management Permissions
INSERT INTO permissions (key, category, description) VALUES
('school.read', 'SCHOOL', 'View school details'),
('school.update', 'SCHOOL', 'Update school settings'),
('school.users.read', 'SCHOOL', 'View school users'),
('school.users.manage', 'SCHOOL', 'Manage school users'),
('school.deletion.request', 'SCHOOL', 'Request school deletion')
ON CONFLICT (key) DO NOTHING;

-- Academic Session Permissions
INSERT INTO permissions (key, category, description) VALUES
('session.read', 'ACADEMIC', 'View academic sessions'),
('session.create', 'ACADEMIC', 'Create academic sessions'),
('session.update', 'ACADEMIC', 'Update academic sessions'),
('term.read', 'ACADEMIC', 'View terms'),
('term.create', 'ACADEMIC', 'Create terms'),
('term.update', 'ACADEMIC', 'Update terms'),
('subject.read', 'ACADEMIC', 'View subjects'),
('subject.create', 'ACADEMIC', 'Create subjects'),
('subject.update', 'ACADEMIC', 'Update subjects')
ON CONFLICT (key) DO NOTHING;
