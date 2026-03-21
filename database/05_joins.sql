use sgdb;
-- Move data from student to users
INSERT INTO users(email, password, account_type, is_temp_password)
SELECT email,'$2a$10$et2gHKnelwV0Z9uCmx.4wej4iRz1HDomu90x4KVWpt/gG8VmS6Fy2','STUDENT', TRUE
FROM student_info
WHERE email NOT IN (SELECT email FROM users);

-- StudentInfo to users(email)
UPDATE student_info s
JOIN users u ON s.email = u.email
SET s.user_id = u.user_id;
select*from staff_info;

-- Move data from saff to users
INSERT INTO users (email, password, account_type, is_temp_password)
SELECT email,'$2a$10$et2gHKnelwV0Z9uCmx.4wej4iRz1HDomu90x4KVWpt/gG8VmS6Fy2','STAFF', TRUE
FROM staff_info
WHERE email NOT IN (SELECT email FROM users);

--  LINK staff to users
UPDATE staff_info AS s
JOIN users u ON s.email = u.email
SET s.user_id = u.user_id;

-- Verify Your Mapping
SELECT s.name, r.role_name
FROM staff_info s
JOIN staff_role sr ON s.staff_id = sr.staff_id
JOIN role r ON sr.role_id = r.role_id;

-- Find Staff/ Now complaint should go to that staff
SELECT s.staff_id, s.name
FROM staff_info s
JOIN staff_role sr ON s.staff_id = sr.staff_id
WHERE sr.role_id = 7;

-- Assign Complaint Automatically
