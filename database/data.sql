INSERT INTO student_info
(name, email, department, class_name, year, enrollment_no)
VALUES
('Ravi Kumar', 'ravi@college.com', 'CSE', 'CSE-A', '3', 'CSE2023001'),
('Anita Sharma', 'anita@college.com', 'CSE', 'CSE-B', '2', 'CSE2023002'),
('Rahul Verma', 'rahul@college.com', 'ECE', 'ECE-A', '3', 'ECE2023001');

INSERT INTO student_info
(name, email, department, class_name, year, enrollment_no)
VALUES
('Priya Singh', 'priya@college.com', 'ECE', 'ECE-B', '2', 'ECE2023002'),
('Madhu', 'madhu@college.com', 'CSE', 'ECE-C', '1', 'CSE2023003'),
('Mounika', 'mounika@college.com', 'CSE', 'CSE-C', '1', 'CSE2023004');

INSERT INTO staff_info (name, email, department, role, phone)
VALUES
('Mr. Arun', 'arun@college.com', 'CSE', 'MFT', '9876543210'),
('Dr. Suresh', 'suresh@college.com', 'CSE', 'HOD', '9876543222'),
('Dr. Rao', 'rao@college.com', 'Administration', 'President', '9876543333');

select * from student_info;
select*from staff_info;

INSERT INTO staff_info (name, email, department, role, phone)
VALUES
('Ms. Priya', 'priya.ece@college.com', 'ECE', 'MFT', '9876544411'),
('Dr. Anand', 'anand.ece@college.com', 'ECE', 'HOD', '9876544422');



