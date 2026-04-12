package com.example.studentservice.service;

import com.example.studentservice.model.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
public class SnsService {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.topic-arn}")
    private String topicArn;

    public SnsService(SnsClient snsClient) {
        this.snsClient = snsClient;
        this.objectMapper = new ObjectMapper();
    }

    public void publishStudentCreatedNotification(Student student) {
        try {
            String subject = "New Student Created: " + student.getName();
            String message = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(student);

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject)
                    .message(message)
                    .build();

            PublishResponse response = snsClient.publish(publishRequest);
            System.out.println("SNS notification sent. MessageId: " + response.messageId());
        } catch (Exception e) {
            System.err.println("Error publishing to SNS: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

