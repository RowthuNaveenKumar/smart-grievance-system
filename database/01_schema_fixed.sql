CREATE DATABASE IF NOT EXISTS sgdb;
USE sgdb;
-- users-----------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT NOT NULL AUTO_INCREMENT,
    email VARCHAR(130) NOT NULL,
    password VARCHAR(225) NOT NULL,
    account_type ENUM('STUDENT','STAFF') NOT NULL,
    is_temp_password TINYINT(1) DEFAULT 1,
    enabled TINYINT(1) DEFAULT 1,
    last_login TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE (email)
);

-- department-------------------------
CREATE TABLE IF NOT EXISTS department (
    department_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,

    PRIMARY KEY (department_id),
    UNIQUE (name)
);
-- hostel---------------------------------------------------------
CREATE TABLE IF NOT EXISTS hostel (
    hostel_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    type ENUM('BOYS','GIRLS') NOT NULL,
    PRIMARY KEY (hostel_id)
);
-- role--------------------------------------
CREATE TABLE IF NOT EXISTS role (
    role_id INT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(100) NOT NULL,
    assignment_scope ENUM('DIVISION','DEPARTMENT','FLOOR','GLOBAL'),

    PRIMARY KEY (role_id),
    UNIQUE (role_name)
);
-- academic_division------------
CREATE TABLE IF NOT EXISTS academic_division (
    division_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,

    PRIMARY KEY (division_id),
    UNIQUE (department_id, name),

    CONSTRAINT fk_division_department
        FOREIGN KEY (department_id)
        REFERENCES department(department_id)
        ON DELETE RESTRICT
);
-- hostel_floor-------------
CREATE TABLE IF NOT EXISTS hostel_floor (
    floor_id INT NOT NULL AUTO_INCREMENT,
    hostel_id INT NOT NULL,
    floor_number VARCHAR(20) DEFAULT NULL,

    PRIMARY KEY (floor_id),
    KEY fk_floor_hostel (hostel_id),

    CONSTRAINT fk_floor_hostel
        FOREIGN KEY (hostel_id)
        REFERENCES hostel(hostel_id)
        ON DELETE CASCADE
);

-- room-----------------------------------
CREATE TABLE IF NOT EXISTS room (
    room_id INT NOT NULL AUTO_INCREMENT,
    floor_id INT NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    capacity INT NOT NULL DEFAULT 1,
    current_occupancy INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id),
    UNIQUE (floor_id, room_number),
    CONSTRAINT fk_room_floor
        FOREIGN KEY (floor_id)
        REFERENCES hostel_floor(floor_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_room_capacity
        CHECK (current_occupancy <= capacity)
);

-- staff_info------------------------
CREATE TABLE IF NOT EXISTS staff_info (
    staff_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    user_id INT DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    division_id INT DEFAULT NULL,
    floor_id INT DEFAULT NULL,
    department_id INT,

    PRIMARY KEY (staff_id),
    UNIQUE (email),
    UNIQUE (user_id),

    KEY fk_staff_division (division_id),
    KEY fk_staff_floor (floor_id),

    CONSTRAINT fk_staff_department
        FOREIGN KEY (department_id)
        REFERENCES department(department_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_staff_division
        FOREIGN KEY (division_id)
        REFERENCES academic_division(division_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_staff_floor
        FOREIGN KEY (floor_id)
        REFERENCES hostel_floor(floor_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_staff_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
);

-- student_info----------------------------
CREATE TABLE IF NOT EXISTS student_info (
    student_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    user_id INT DEFAULT NULL,
    year ENUM('FIRST_YEAR','SECOND_YEAR','THIRD_YEAR','FOURTH_YEAR') DEFAULT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    enrollment_no VARCHAR(100) NOT NULL,
    division_id INT NOT NULL,
    room_id INT DEFAULT NULL,
    PRIMARY KEY (student_id),
    UNIQUE (email),
    UNIQUE (enrollment_no),
    UNIQUE (user_id),

    KEY fk_student_division (division_id),
    KEY fk_student_room (room_id),

    CONSTRAINT fk_student_division
        FOREIGN KEY (division_id)
        REFERENCES academic_division(division_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_student_room
        FOREIGN KEY (room_id)
        REFERENCES room(room_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_student_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
);

-- staff_role----------------------
CREATE TABLE IF NOT EXISTS staff_role (
    staff_id INT NOT NULL,
    role_id INT NOT NULL,

    PRIMARY KEY (staff_id, role_id),
    KEY role_id (role_id),

    CONSTRAINT staff_role_ibfk_1
        FOREIGN KEY (staff_id)
        REFERENCES staff_info(staff_id)
        ON DELETE CASCADE,

    CONSTRAINT staff_role_ibfk_2
        FOREIGN KEY (role_id)
        REFERENCES role(role_id)
        ON DELETE RESTRICT
);
-- escalation_matrix-----------------------------------
-- CREATE TABLE IF NOT EXISTS escalation_matrix (
--     id INT NOT NULL AUTO_INCREMENT,
--     category ENUM(
-- 'HOSTEL',
-- 'ACADEMIC',
-- 'LIBRARY',
-- 'MEDICAL',
-- 'TRANSPORT',
-- 'IT',
-- 'FACILITY',
-- 'SPORTS',
-- 'ADMIN',
-- 'GENERAL'
-- ) NOT NULL,
--     level INT NOT NULL,
--     role_id INT NOT NULL,
--     resolution_time_hours INT NOT NULL,

--     PRIMARY KEY (id),
--     KEY role_id (role_id),

--     CONSTRAINT escalation_matrix_ibfk_1
--         FOREIGN KEY (role_id)
--         REFERENCES role(role_id)
--         ON DELETE RESTRICT
-- );
-- ALTER TABLE escalation_matrix
-- DROP FOREIGN KEY escalation_matrix_ibfk_1;

-- Workflow-------------------------------
CREATE TABLE IF NOT EXISTS workflow (
    workflow_id INT AUTO_INCREMENT PRIMARY KEY,
    department_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,

    UNIQUE(department_id),

    CONSTRAINT fk_workflow_department
        FOREIGN KEY (department_id)
        REFERENCES department(department_id)
        ON DELETE CASCADE
);
-- Workfow steps---------------
CREATE TABLE IF NOT EXISTS workflow_steps (
    step_id INT AUTO_INCREMENT PRIMARY KEY,
    workflow_id INT NOT NULL,
    level INT NOT NULL,
    role_id INT NOT NULL,
    resolution_time_hours INT,

    CONSTRAINT fk_step_workflow
        FOREIGN KEY (workflow_id)
        REFERENCES workflow(workflow_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_step_role
        FOREIGN KEY (role_id)
        REFERENCES role(role_id)
        ON DELETE RESTRICT,

    UNIQUE (workflow_id, level) -- important
);

-- ALTER TABLE complaints
-- ADD current_level INT DEFAULT 1 AFTER escalation_level,
-- ADD workflow_id INT AFTER escalation_level;

-- ALTER TABLE complaints
-- ADD CONSTRAINT fk_complaint_workflow
-- FOREIGN KEY (workflow_id)
-- REFERENCES workflow(workflow_id)
-- ON DELETE SET NULL;

-- ALTER TABLE complaints
-- DROP COLUMN category;

-- CREATE CATEGORY TABLE (REPLACE ENUM) in complaints
CREATE TABLE IF NOT EXISTS complaint_category (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,

    UNIQUE(name, department_id),
    FOREIGN KEY (department_id)
    REFERENCES department(department_id)
    ON DELETE CASCADE
);

--  complaints-----------------------------------------
CREATE TABLE IF NOT EXISTS complaints (
    complaint_id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    assigned_staff_id INT DEFAULT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category_id INT,
    department_id INT,
    -- category ENUM('HOSTEL','ACADEMIC','LIBRARY','MEDICAL','TRANSPORT','IT','FACILITY','SPORTS','ADMIN','GENERAL') NOT NULL,
    ml_predicted_category VARCHAR(100) DEFAULT NULL,
    priority ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'LOW',
    ml_predicted_priority VARCHAR(50) DEFAULT NULL,
    ml_confidence DECIMAL(5,4) DEFAULT NULL,
    escalation_level INT DEFAULT 1,
    workflow_id INT,
    current_level INT DEFAULT 1,
    status ENUM('OPEN','IN_PROGRESS','ESCALATED','RESOLVED','REJECTED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL DEFAULT NULL,

    PRIMARY KEY (complaint_id),

    KEY idx_complaint_student (student_id),
    KEY idx_complaint_assigned (assigned_staff_id),
    KEY idx_complaint_status (status),
    KEY idx_complaint_priority (priority),
    KEY idx_complaint_status_priority (status, priority),

    KEY idx_complaint_created (created_at),
    KEY idx_complaints_workflow(workflow_id),
    KEY idx_complaint_department(department_id),

    CONSTRAINT fk_complaint_staff
        FOREIGN KEY (assigned_staff_id)
        REFERENCES staff_info(staff_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_complaint_workflow
        FOREIGN KEY (workflow_id)
        REFERENCES workflow(workflow_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_complaint_category
        FOREIGN KEY (category_id)
        REFERENCES complaint_category(category_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_complaint_department
        FOREIGN KEY (department_id)
        REFERENCES department(department_id),

    CONSTRAINT fk_complaint_student
        FOREIGN KEY (student_id)
        REFERENCES student_info(student_id)
        ON DELETE CASCADE
);
-- complaint_files-------------------------------
CREATE TABLE IF NOT EXISTS complaint_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id INT NOT NULL,
    file_url TEXT NOT NULL,
    uploaded_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY fk_file_complaint (complaint_id),

    CONSTRAINT fk_file_complaint
        FOREIGN KEY (complaint_id)
        REFERENCES complaints(complaint_id)
        ON DELETE CASCADE
);
-- complaints_updates------------------------------------------
CREATE TABLE IF NOT EXISTS complaint_updates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id INT NOT NULL,
    performed_by INT DEFAULT NULL,
    action VARCHAR(100) NOT NULL,
    note TEXT,
    from_status ENUM('OPEN','IN_PROGRESS','ESCALATED','RESOLVED','REJECTED') DEFAULT NULL,
    to_status ENUM('OPEN','IN_PROGRESS','ESCALATED','RESOLVED','REJECTED') DEFAULT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY fk_update_complaint (complaint_id),
    KEY fk_update_staff (performed_by),

    CONSTRAINT fk_update_complaint
        FOREIGN KEY (complaint_id)
        REFERENCES complaints(complaint_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_update_staff
        FOREIGN KEY (performed_by)
        REFERENCES staff_info(staff_id)
        ON DELETE SET NULL
);

-- ALTER TABLE complaints
-- ADD category_id INT,
-- ADD department_id INT
-- AFTER description;

-- ALTER TABLE complaints
-- ADD CONSTRAINT fk_complaint_category
-- FOREIGN KEY (category_id)
-- REFERENCES complaint_category(category_id);

-- ALTER TABLE complaints
-- ADD CONSTRAINT fk_complaint_department
-- FOREIGN KEY (department_id)
-- REFERENCES department(department_id);

-- ALTER TABLE staff_info
-- ADD department_id INT;

-- ALTER TABLE staff_info
-- ADD CONSTRAINT fk_staff_department
-- FOREIGN KEY (department_id)
-- REFERENCES department(department_id)
-- ON DELETE SET NULL;

-- ALTER TABLE workflow
-- ADD UNIQUE(department_id);

-- ALTER TABLE complaint_category
-- ADD UNIQUE(name, department_id); 

-- ALTER TABLE role
-- ADD assignment_scope ENUM(
-- 'DIVISION',
-- 'DEPARTMENT',
-- 'FLOOR',
-- 'GLOBAL'
-- );


