INSERT INTO department(department_id,name)
VALUES
(1, 'CSE'),
(2, 'IT'),
(3, 'AI/DS'),
(4, 'Mechanical'),
(5, 'Civil'),
(6, 'EEE'),
(7, 'ECE'),
(8, 'Administration'),
(9, 'Exam Cell'),
(10, 'Library'),
(11, 'Hostel'),
(12, 'Transport'),
(13, 'Sports'),
(14, 'Medical');


SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM academic_division;
DELETE FROM department;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO academic_division (division_id, name, department_id) VALUES
(1, 'CSE-A', 1),
(2, 'CSE-B', 1),

(3, 'IT-A', 2),
(4, 'IT-B', 2),

(5, 'AI-DS-A', 3),

(6, 'MECH-A', 4),

(7, 'CIVIL-A', 5),

(8, 'EEE-A', 6),

(9, 'ECE-A', 7),
(10, 'ECE-B', 7);

SELECT * FROM academic_division ORDER BY division_id;
SELECT*FROM department;

INSERT INTO hostel (hostel_id, name, type) VALUES
(1, 'Boys Hostel A', 'BOYS'),
(2, 'Boys Hostel B', 'BOYS'),
(3, 'Girls Hostel A', 'GIRLS'),
(4, 'Girls Hostel B', 'GIRLS');

-- 
INSERT INTO hostel_floor (floor_id, hostel_id, floor_number) VALUES
-- Boys Hostel A
(1, 1, '1'),
(2, 1, '2'),
(3, 1, '3'),

-- Boys Hostel B
(4, 2, '1'),
(5, 2, '2'),
(6, 2, '3'),

-- Girls Hostel A
(7, 3, '1'),
(8, 3, '2'),
(9, 3, '3'),

-- Girls Hostel B
(10, 4, '1'),
(11, 4, '2'),
SELECT * FROM hostel_floor ORDER BY floor_id;

--  
INSERT INTO room (room_id, floor_id, room_number, capacity, current_occupancy) VALUES

-- Floor 1 (Boys A)
(1, 1, '101', 2, 0),
(2, 1, '102', 2, 0),
(3, 1, '103', 2, 0),
(4, 1, '104', 2, 0),
-- floor 2
(5, 2, '201', 3, 0),
(6, 2, '202', 3, 0),
(7, 2, '203', 3, 0),
(8, 2, '204', 3, 0),
-- Floor 3
(9, 3, '301', 4, 0),
(10,3, '302', 4, 0),
(11,3, '303', 4, 0),
(12, 3, '304',4, 0),
-- Floor 1 (BOYS B)
(13, 4, '101', 2, 0),
(14, 4, '102', 2, 0),
(15, 4, '103', 2, 0),
(16, 4, '104', 2, 0),
-- Floor 2
(17, 5, '201', 3, 0),
(18, 5, '202', 3, 0),
(19, 5, '203', 3, 0),
(20, 5, '204', 3, 0),
-- Floor 3
(21, 6, '301', 4, 0),
(22, 6, '302', 4, 0),
(23, 6, '303', 4, 0),
(24, 6, '304', 4, 0),
-- Girls Hostel-A
-- Floor-1
(25, 7, '101', 2, 0),
(26, 7, '102', 2, 0),
(27, 7, '103', 2, 0),
(28, 7, '104', 2, 0),
-- Floor 2
(29, 8, '201', 3, 0),
(30, 8, '202', 3, 0),
(31, 8, '203', 3, 0),
(32, 8, '204', 3, 0),
-- Floor 3
(33, 9, '301', 4, 0),
(34, 9, '302', 4, 0),
(35, 9, '303', 4, 0),
(36, 9, '304', 4, 0),
-- Girls Hostel-B
-- Floor-1
(37, 10, '101', 2, 0),
(38, 10, '102', 2, 0),
(39, 10, '103', 2, 0),
(40, 10, '104', 2, 0),
-- Floor 2
(41, 11, '201', 3, 0),
(42, 11, '202', 3, 0),
(43, 11, '203', 3, 0),
(44, 11, '204', 3, 0),
-- Floor 3
(45, 12, '301', 4, 0),
(46, 12, '302', 4, 0),
(47, 12, '303', 4, 0),
(48, 12, '304', 4, 0);
-- role------------------
INSERT INTO role (role_id, role_name) VALUES
(1, 'ADMIN'),
(2, 'STAFF'),
(3, 'STUDENT');
INSERT INTO role (role_id, role_name) VALUES
(4,'HOD'),
(5,'DEAN'),
(6,'WARDEN');
UPDATE role
SET role_name = 'MFT'
WHERE role_id = 2;
-- staff_role------
INSERT INTO staff_role (staff_id, role_id) VALUES
(1,1),
(2,2),
(3,2),
(4,3);

-- staff_info
INSERT INTO staff_info (staff_id, name, email, password, phone,division_id,floor_id)
VALUES
(1,'Naveen Kumar', 'naveen@sgms.com', '123456', '9876543210',1,NULL),
(2,'Priya Sharma', 'priya@sgms.com', '123456', '9876543211',9,NULL);
INSERT INTO staff_info (staff_id, name, email, password, phone,division_id,floor_id)
VALUES
(3,'Prof. Amit Shah','amit@sgms.com','123456','839489283',NULL,NULL),
(4,'Dr. Rakesh Patel','rakesh@sgms.com','123456','672836384',NULL,NULL);

INSERT INTO staff_info (staff_id, name, email, password, phone,division_id,floor_id)
VALUES
(5,'patel rajputh','patel@sgmsadmin.com','123456','839489234',NULL,NULL);
INSERT INTO staff_role (staff_id, role_id) VALUES
(5,1);
INSERT INTO staff_role (staff_id, role_id) VALUES
(1,2),
(2,2);
SELECT*FROM staff_info;

-- student_info
INSERT INTO student_info(student_id, name, email, password, year,enrollment_no,division_id,room_id)
VALUES
(1,'Rithish Chowdary', 'rithish@sgms.com', '123456','3rd year','2303031240505',1,1),
(2,'Mounika patel', 'mouni@sgms.com', '123456', '3rd year','2303031240506',1,25);
SELECT*FROM student_info;
DESCRIBE student_info;

UPDATE room
SET current_occupancy = current_occupancy + 1
WHERE room_id = 1;

UPDATE room
SET current_occupancy = current_occupancy + 1
WHERE room_id = 25;
-- auto updations-----------
CREATE TRIGGER update_room_occupancy
AFTER INSERT ON student_info
FOR EACH ROW
UPDATE room
SET current_occupancy = current_occupancy + 1
WHERE room_id = NEW.room_id;
--         
SELECT*FROM academic_division;
SELECT*FROM room;

INSERT INTO staff_role (staff_id, role_id)
VALUES
(3,4),   -- Amit Shah → HOD
(4,5);   -- Rakesh Patel → Dean

SELECT*FROM escalation_matrix;
INSERT INTO escalation_matrix (id, category, level, role_id, resolution_time_hours)
VALUES
-- Academic complaints
(1,'ACADEMIC',1,2,24),   -- MFT gets 24 hours
(2,'ACADEMIC',2,4,48),   -- HOD gets 48 hours
(3,'ACADEMIC',3,5,72);   -- Dean gets 72 hours

-- -- Hostel complaints
-- (4,'HOSTEL',1,6,24),     -- Warden gets 24 hours
-- (5,'HOSTEL',2,1,48);     -- Admin gets 48 hours
