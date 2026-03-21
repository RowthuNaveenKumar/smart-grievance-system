-- =====================================
-- INDEXES FOR COMPLAINT SEARCHING
-- =====================================
use sgdb;

CREATE INDEX idx_complaint_student
ON complaints(student_id);

CREATE INDEX idx_complaint_assigned
ON complaints(assigned_staff_id);

CREATE INDEX idx_complaint_status
ON complaints(status);

CREATE INDEX idx_complaint_priority
ON complaints(priority);

CREATE INDEX idx_complaint_status_priority
ON complaints(status, priority);

CREATE INDEX idx_complaint_created ON complaints(created_at);
CREATE INDEX idx_complaint_workflow ON complaints(workflow_id);
CREATE INDEX idx_complaint_department ON complaints(department_id);
