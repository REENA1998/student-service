package com.example.studentservice.repository;

import com.example.studentservice.model.Student;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudentRepository {

    private  DynamoDbTable<Student> studentTable;
    private final DynamoDbEnhancedClient dynamoDBEnhancedClient;

    @PostConstruct
    public void init(){
        studentTable = dynamoDBEnhancedClient.table("student", TableSchema.fromBean(Student.class));
    }

    public Student save(Student student) {
        studentTable.putItem(student);
        return student;
    }

    public Optional<Student> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        Student student = studentTable.getItem(key);
        return Optional.ofNullable(student);
    }

    public List<Student> findAll() {
        List<Student> students = new ArrayList<>();
        studentTable.scan().items().forEach(students::add);
        return students;
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        studentTable.deleteItem(key);
    }
}

