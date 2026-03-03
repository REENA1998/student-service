package com.example.studentservice.controller;

import com.example.studentservice.model.Student;
import com.example.studentservice.service.StudentClient;
import com.example.studentservice.service.StudentService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private Logger logger = LoggerFactory.getLogger(StudentController.class);

    // private final StudentClient studentClient;
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        //this.studentClient = studentClient;
        this.studentService = studentService;
    }

    // POST API to add a student
    @PostMapping
    public ResponseEntity<Student> addStudent(@RequestBody Student student) {
        logger.info("Adding new student: {}", student);
        Student savedStudent = studentService.addStudent(student);
        return new ResponseEntity<>(savedStudent, HttpStatus.CREATED);
    }

    // GET API to fetch all students
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        logger.info("Fetching all students");
        List<Student> students = studentService.getAllStudents();
        return new ResponseEntity<>(students, HttpStatus.OK);
    }

    // GET API to fetch a student by ID
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        logger.info("Fetching student with id: {}", id);
        return studentService.getStudentById(id)
                .map(student -> new ResponseEntity<>(student, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // PUT API to update a student
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student studentDetails) {
        logger.info("Updating student with id: {}", id);
        try {
            Student updatedStudent = studentService.updateStudent(id, studentDetails);
            return new ResponseEntity<>(updatedStudent, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // DELETE API to delete a student
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        logger.info("Deleting student with id: {}", id);
        studentService.deleteStudent(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    int retryCount = 1;

//    @GetMapping("/fetch-students")
//    // @CircuitBreaker(name = "schoolServiceBreaker", fallbackMethod = "schoolFallback")
//    //@Retry(name = "schoolServiceBreaker", fallbackMethod = "schoolFallback")
//    @RateLimiter(name = "studentRateLimiter", fallbackMethod = "schoolFallback")
//    public List<Student> fetchStudents() {
//        logger.info("Retry count: {}", retryCount);
//        retryCount++;
//        return studentClient.getStudents();
//    }

    @GetMapping("/school")
    public String fetchSchool() {
        return "My student is running on ec2 instances";
    }

    //creating fall back  method for circuitbreaker
//    public List<Student> schoolFallback(Exception ex) {
//        logger.info("Fallback is executed because service is down : ", ex.getMessage());
//        Student student = Student.builder()
//                .name("reena").grade("A")
//                .build();
//        return List.of(student);
//    }

//    @GetMapping
//    public List<String> getStudents(@RequestHeader(value = "Authorization", required = false) String authHeader) {
//        if (authHeader == null) {
//            return List.of("Unauthorized: Missing token");
//        }
//        return List.of("Student1", "Student2", "Token received: " + authHeader);
//    }
}
