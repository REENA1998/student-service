package com.example.studentservice.service;

import com.example.studentservice.model.Student;
import com.example.studentservice.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final SqsService sqsService;

    public StudentService(StudentRepository studentRepository, SqsService sqsService) {
        this.studentRepository = studentRepository;
        this.sqsService = sqsService;
    }

    public Student addStudent(Student student) {
        Student savedStudent = studentRepository.save(student);
        // Send message to SQS after successfully saving student
        sqsService.sendStudentCreatedMessage(savedStudent);
        return savedStudent;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(String id) {
        return studentRepository.findById(id);
    }

    public Student updateStudent(String id, Student studentDetails) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        student.setName(studentDetails.getName());
        student.setGrade(studentDetails.getGrade());
        return studentRepository.save(student);
    }

    public void deleteStudent(String id) {
        studentRepository.deleteById(id);
    }
}

