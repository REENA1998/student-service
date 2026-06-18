package com.example.studentservice;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lambda Handler for Spring Boot Application
 *
 * Handler configuration: com.example.studentservice.StreamLambdaHandler::handleRequest
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(StudentServiceApplication.class);
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        // Log raw event so we can see exactly what API Gateway is sending
        byte[] inputBytes = inputStream.readAllBytes();
        String rawEvent = new String(inputBytes, StandardCharsets.UTF_8);
        context.getLogger().log("RAW_EVENT_START: " + rawEvent + " :RAW_EVENT_END");

        handler.proxyStream(new ByteArrayInputStream(inputBytes), outputStream, context);
    }
}
