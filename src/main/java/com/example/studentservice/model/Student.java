package com.example.studentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.UUID;

@Data
@Builder
//@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Student {

    private String id;
    private String name;
    private String grade;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("studentid")
    public String getId() {
        return id;
    }

    public Student() {
        this.id = UUID.randomUUID().toString();
    }
}
