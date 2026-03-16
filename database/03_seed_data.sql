use sgdb;
-- Department Data-----------------
INSERT INTO department (department_id, name) VALUES
('CSE'),
('IT'),
('AI/DS'),
('Mechanical'),
('Civil'),
('EEE'),
('ECE'),
('Administration'),
('Exam Cell'),
('Library'),
('Hostel'),
('Transport'),
('Sports'),
('Medical');

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
(1,1,'101',2,0),(2,1,'102',2,0),(3,1,'103',2,0),(4,1,'104',2,0),
(5,2,'201',3,0),(6,2,'202',3,0),(7,2,'203',3,0),(8,2,'204',3,0),
(9,3,'301',4,0),(10,3,'302',4,0),(11,3,'303',4,0),(12,3,'304',4,0),
(13,4,'101',2,0),(14,4,'102',2,0),(15,4,'103',2,0),(16,4,'104',2,0),
(17,5,'201',3,0),(18,5,'202',3,0),(19,5,'203',3,0),(20,5,'204',3,0),
(21,6,'301',4,0),(22,6,'302',4,0),(23,6,'303',4,0),(24,6,'304',4,0),
(25,7,'101',2,0),(26,7,'102',2,0),(27,7,'103',2,0),(28,7,'104',2,0),
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
(6,'WARDEN');

-- Staff Data---------------------------
INSERT INTO staff_info (staff_id, name, email, phone, division_id, floor_id) VALUES
(1,'Naveen Kumar','naveen@sgms.com','9876543210',NULL,NULL),
(2,'Priya Sharma','priya@sgms.com','9876543211',9,NULL),
(3,'Prof. Amit Shah','amit@sgms.com','839489283',NULL,NULL),
(4,'Dr. Rakesh Patel','rakesh@sgms.com','672836384',NULL,NULL),
(5,'Patel Rajputh','patel@sgmsadmin.com','839489234',NULL,NULL);

-- staff_role Data-------------------
INSERT INTO staff_role (staff_id, role_id) VALUES
(1,1),
(2,2),
(3,4),
(4,5),
(5,1);
select*from student_info;
DESCRIBE TABLE student_info;
-- Student_info data--------------------------
INSERT INTO student_info (student_id,name,email,year,enrollment_no,division_id,room_id) VALUES
(1,'Rithish Chowdary','rithish@sgms.com','THIRD_YEAR','2303031240505',1,1),
(2,'Mounika Patel','mouni@sgms.com','THIRD_YEAR','2303031240506',1,25);

-- escalation_matix Data--------------------
INSERT INTO escalation_matrix (id, category, level, role_id, resolution_time_hours) VALUES
(1,'ACADEMIC',1,2,24),
(2,'ACADEMIC',2,4,48),
(3,'ACADEMIC',3,5,72);