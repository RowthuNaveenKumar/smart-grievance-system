use sgdb;
-- Department Data-----------------
INSERT INTO department (department_id, name) VALUES
(1,'CSE'),
(2,'IT'),
(3,'AI/DS'),
(4,'Mechanical'),
(5,'Civil'),
(6,'EEE'),
(7,'ECE'),
(8,'Administration'),
(9,'Exam Cell'),
(10,'Library'),
(11,'Hostel'),
(12,'Transport'),
(13,'Sports'),
(14,'Medical');

-- Academic Division Data
INSERT INTO academic_division (division_id, name, department_id) VALUES
(1,'CSE-A',1),
(2,'CSE-B',1),
(3,'IT-A',2),
(4,'IT-B',2),
(5,'AI-DS-A',3),
(6,'MECH-A',4),
(7,'CIVIL-A',5),
(8,'EEE-A',6),
(9,'ECE-A',7),
(10,'ECE-B',7);

-- Hostel Data------------------
INSERT INTO hostel (hostel_id, name, type) VALUES
(1,'Boys Hostel A','BOYS'),
(2,'Boys Hostel B','BOYS'),
(3,'Girls Hostel A','GIRLS'),
(4,'Girls Hostel B','GIRLS');

-- hostel_floor Data------------------
INSERT INTO hostel_floor (floor_id, hostel_id, floor_number) VALUES
(1,1,'1'),
(2,1,'2'),
(3,1,'3'),
(4,2,'1'),
(5,2,'2'),
(6,2,'3'),
(7,3,'1'),
(8,3,'2'),
(9,3,'3'),
(10,4,'1'),
(11,4,'2'),
(12,4,'3');

-- Room Data------------------------
INSERT INTO room (room_id, floor_id, room_number, capacity, current_occupancy) VALUES
(1,1,'101',2,1),(2,1,'102',2,0),(3,1,'103',2,0),(4,1,'104',2,0),
(5,2,'201',3,0),(6,2,'202',3,0),(7,2,'203',3,0),(8,2,'204',3,0),
(9,3,'301',4,0),(10,3,'302',4,0),(11,3,'303',4,0),(12,3,'304',4,0),
(13,4,'101',2,0),(14,4,'102',2,0),(15,4,'103',2,0),(16,4,'104',2,0),
(17,5,'201',3,0),(18,5,'202',3,0),(19,5,'203',3,0),(20,5,'204',3,0),
(21,6,'301',4,0),(22,6,'302',4,0),(23,6,'303',4,0),(24,6,'304',4,0),
(25,7,'101',2,1),(26,7,'102',2,0),(27,7,'103',2,0),(28,7,'104',2,0),
(29,8,'201',3,0),(30,8,'202',3,0),(31,8,'203',3,0),(32,8,'204',3,0),
(33,9,'301',4,0),(34,9,'302',4,0),(35,9,'303',4,0),(36,9,'304',4,0),
(37,10,'101',2,0),(38,10,'102',2,0),(39,10,'103',2,0),(40,10,'104',2,0),
(41,11,'201',3,0),(42,11,'202',3,0),(43,11,'203',3,0),(44,11,'204',3,0),
(45,12,'301',4,0),(46,12,'302',4,0),(47,12,'303',4,0),(48,12,'304',4,0);

-- role Data------------------
INSERT INTO role (role_id, role_name) VALUES
(1,'ADMIN'),
(2,'MFT'),
(3,'STUDENT'),
(4,'HOD'),
(5,'DEAN'),
(6,'WARDEN'),
(7,'LIBRARIAN'),
(8,'TRANSPORT_MANAGER');

-- INSERT INTO role (role_name) VALUES
-- ('LIBRARIAN'),
-- ('TRANSPORT_MANAGER');

-- Staff Data---------------------------
INSERT INTO staff_info (staff_id, name, email, phone, division_id, floor_id) VALUES
(1,'Naveen Kumar','naveen@sgms.com','9876543210',NULL,NULL),
(2,'Priya Sharma','priya@sgms.com','9876543211',9,NULL),
(3,'Prof. Amit Shah','amit@sgmshod.com','839489283',NULL,NULL),
(4,'Dr.Rakesh Patel','rakesh@sgmsdean.com','672836384',NULL,NULL),
(5,'Patel Rajputh','patel@sgmswarden.com','839489234',NULL,1),
(6,'Geetha','sandeep@sgmswarden.com','8072382917',NULL,7),
(7,'Krishna','krishna78@sgmslibrarian.com','807287642',NULL,NULL),
(8,'Hansika','hansika@sgmstransport.com','992382917',NULL,NULL);


-- INSERT INTO staff_info (staff_id, name, email, phone, division_id, floor_id) VALUES
-- (7,'Krishna','krishna78@sgmslibrarian.com','807287642',NULL,NULL),
-- (8,'Hansika','hansika@sgmstransport.com','992382917',NULL,NULL);

-- staff_role Data-------------------
INSERT INTO staff_role (staff_id, role_id) VALUES
(1,1),
(2,2),
(3,4),
(4,5),
(5,6),
(6,6),
(7,7),
(8,8);

-- INSERT INTO staff_role (staff_id, role_id)
-- VALUES
-- (7,7),  -- assign someone as librarian
-- (8,8);  -- assign transport manager

-- Student_info data--------------------------
INSERT INTO student_info (student_id,name,email,year,enrollment_no,division_id,room_id) VALUES
(1,'Rithish Chowdary','rithish@sgms.com','THIRD_YEAR','2303031240505',1,1),
(2,'Mounika Patel','mouni@sgms.com','THIRD_YEAR','2303031240506',1,25);

INSERT INTO workflow (workflow_id, department_id, name) VALUES
(1,1,'CSE Workflow'),
(2,2,'IT Workflow'),
(3,3,'AI/DS Workflow'),
(4,4,'Mechanical Workflow'),
(5,5,'Civil Workflow'),
(6,6,'EEE Workflow'),
(7,7,'ECE Workflow'),
(8,8,'Administration Workflow'),
(9,9,'Exam Cell Workflow'),
(10,10,'Library Workflow'),
(11,11,'Hostel Workflow'),
(12,12,'Transport Workflow'),
(13,13,'Sports Workflow'),
(14,14,'Medical Workflow');

INSERT INTO workflow_steps (workflow_id, level, role_id, resolution_time_hours)
VALUES
-- MFT → HOD → DEAN → ADMIN
-- CSE (1)
(1,1,2,24),(1,2,4,48),(1,3,5,72),(1,4,1,96),
-- IT (2)
(2,1,2,24),(2,2,4,48),(2,3,5,72),(2,4,1,96),
-- AI/DS (3)
(3,1,2,24),(3,2,4,48),(3,3,5,72),(3,4,1,96),
-- Mechanical (4)
(4,1,2,24),(4,2,4,48),(4,3,5,72),(4,4,1,96),
-- Civil (5)
(5,1,2,24),(5,2,4,48),(5,3,5,72),(5,4,1,96),
-- EEE (6)
(6,1,2,24),(6,2,4,48),(6,3,5,72),(6,4,1,96),
-- ECE (7)
(7,1,2,24),(7,2,4,48),(7,3,5,72),(7,4,1,96);


INSERT INTO workflow_steps (workflow_id, level, role_id, resolution_time_hours)
VALUES
(8,1,1,24), -- Administration + Exam Cell -> Only Admin
(9,1,1,24),  

(10,1,7,24), -- Library ->LIBRARIAN → ADMIN
(10,2,1,48),

(11,1,6,24), -- Hostel ->WARDEN → ADMIN
(11,2,1,48),

(12,1,8,24), -- Transport ->TRANSPORT_MANAGER → ADMIN
(12,2,1,48),

(13,1,1,24), -- Sports-> ADMIN 
(14,1,1,24); -- Medical-> ADMIN 

INSERT INTO complaint_category(name, department_id) VALUES
('ACADEMIC',1),     -- CSE
('ACADEMIC',2),     -- IT
('ACADEMIC',3),     -- AI/DS
('ACADEMIC',4),     -- Mechanical
('ACADEMIC',5),     -- Civil
('ACADEMIC',6),     -- EEE
('ACADEMIC',7),     -- ECE
('ADMIN',8),        -- Administration
('EXAM',9),         -- Exam Cell
('LIBRARY',10),     -- Library
('HOSTEL',11),      -- Hostel
('TRANSPORT',12),   -- Transport
('SPORTS',13),      -- Sports
('MEDICAL',14);     -- Medical

UPDATE role SET assignment_scope='GLOBAL' WHERE role_name='ADMIN';
UPDATE role SET assignment_scope='DIVISION' WHERE role_name='MFT';
UPDATE role SET assignment_scope='DEPARTMENT' WHERE role_name='HOD';
UPDATE role SET assignment_scope='DEPARTMENT' WHERE role_name='DEAN';
UPDATE role SET assignment_scope='FLOOR' WHERE role_name='WARDEN';
UPDATE role SET assignment_scope='DEPARTMENT' WHERE role_name='LIBRARIAN';
UPDATE role SET assignment_scope='DEPARTMENT' WHERE role_name='TRANSPORT_MANAGER';
UPDATE role SET assignment_scope='GLOBAL' WHERE role_name='STUDENT';