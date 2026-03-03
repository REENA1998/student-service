-- PostgreSQL Setup Script for Student Service

-- Connect to PostgreSQL as postgres user:
-- psql -U postgres

-- 1. Create database (if not exists)
CREATE DATABASE studentdb;

-- 2. Connect to the database
\c studentdb;

-- 3. (Optional) Create the students table manually
-- Note: Spring Boot with hibernate.ddl-auto=update will create this automatically
CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    grade VARCHAR(255) NOT NULL
);

-- 4. (Optional) Insert sample data for testing
INSERT INTO students (name, grade) VALUES
    ('Reena', 'A'),
    ('John Doe', 'B+'),
    ('Jane Smith', 'A+'),
    ('Bob Wilson', 'B');

-- 5. Verify the data
SELECT * FROM students;

-- Useful queries:

-- Count total students
SELECT COUNT(*) FROM students;

-- Find students by grade
SELECT * FROM students WHERE grade = 'A';

-- Delete all students (be careful!)
-- DELETE FROM students;

-- Drop table (if needed to recreate)
-- DROP TABLE students;

