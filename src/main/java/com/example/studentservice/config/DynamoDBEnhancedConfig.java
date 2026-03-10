package com.example.studentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDBEnhancedConfig {

    // @Value("${cloud.aws.region.static:us-east-1}") for lamda testing
    @Value("${cloud.aws.region.static}")
    private String region;

    // Default DynamoDBEnhancedClient for Lambda and other environments (uses IAM role credentials)
    @Bean("dynamoDBEnhancedClient")
    @Profile("local")
    public DynamoDbEnhancedClient dynamoDBEnhancedClient(@Value("${cloud.aws.credentials.access-key}") String accessKey,
                                                         @Value("${cloud.aws.credentials.secret-key}") String secretKey)
    {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        DynamoDbClient dynamoDBClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .build();


        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDBClient).build();
    }


    @Bean("dynamoDBEnhancedClient")
    @Profile("dev") // comment this for lambda testing
    public DynamoDbEnhancedClient dynamoDBEnhancedClientdev()
    {
        DynamoDbClient dynamoDBClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();


        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDBClient).build();
    }
}
