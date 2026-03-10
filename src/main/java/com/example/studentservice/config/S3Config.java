package com.example.studentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {


    // @Value("${cloud.aws.region.static:us-east-1}")
    @Value("${cloud.aws.region.static}")
    private String region;

    // Local profile S3Client with explicit credentials
    @Bean("s3ClientLocal")
    @Profile("local")
    public S3Client s3ClientLocal(@Value("${cloud.aws.credentials.access-key}") String accessKey,
                                   @Value("${cloud.aws.credentials.secret-key}") String secretKey){
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .build();

    }
    // Default S3Client for Lambda and other environments (uses IAM role credentials)
    @Bean("s3Client")
    @Profile("dev") // remove this for lambda testing bcz we cannot give dev
    public S3Client s3ClientDev(){
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}