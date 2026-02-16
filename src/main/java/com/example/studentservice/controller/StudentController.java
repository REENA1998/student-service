package com.example.studentservice.controller;

import com.example.studentservice.model.Student;
import com.example.studentservice.service.StudentClient;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private Logger logger = LoggerFactory.getLogger(StudentController.class);

    private final StudentClient studentClient;

    public StudentController(StudentClient studentClient) {
        this.studentClient = studentClient;
    }


    int retryCount = 1;

    @GetMapping("/fetch-students")
    // @CircuitBreaker(name = "schoolServiceBreaker", fallbackMethod = "schoolFallback")
    //@Retry(name = "schoolServiceBreaker", fallbackMethod = "schoolFallback")
    @RateLimiter(name = "studentRateLimiter", fallbackMethod = "schoolFallback")
    public List<Student> fetchStudents() {
        logger.info("Retry count: {}", retryCount);
        retryCount++;
        return studentClient.getStudents();
    }

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

    @GetMapping
    public List<String> getStudents(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null) {
            return List.of("Unauthorized: Missing token");
        }
        return List.of("Student1", "Student2", "Token received: " + authHeader);
    }
}
