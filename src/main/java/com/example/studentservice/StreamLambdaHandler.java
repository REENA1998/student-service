package com.example.studentservice;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lambda Handler for Spring Boot Application
 *
 * Handler configuration: com.example.studentservice.StreamLambdaHandler::handleRequest
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    private static final String VALID_TOKEN = "bearer-toker";
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

        byte[] inputBytes = inputStream.readAllBytes();
        String rawEvent = new String(inputBytes, StandardCharsets.UTF_8);
        context.getLogger().log("RAW_EVENT: " + rawEvent);

        // Detect if this is a TOKEN authorizer event
        Map<String, Object> eventMap = MAPPER.readValue(rawEvent, Map.class);

        if ("TOKEN".equals(eventMap.get("type")) && eventMap.containsKey("authorizationToken")) {
            // Handle as custom authorizer
            context.getLogger().log("Handling as TOKEN authorizer");
            String token = (String) eventMap.get("authorizationToken");
            String methodArn = (String) eventMap.get("methodArn");

            boolean isValid = VALID_TOKEN.equals(token);
            Map<String, Object> policy = generatePolicy("user", isValid ? "Allow" : "Deny", methodArn);

            context.getLogger().log("Token " + (isValid ? "valid - Allow" : "invalid - Deny"));
            outputStream.write(MAPPER.writeValueAsBytes(policy));
        } else {
            // Handle as normal API Gateway proxy request → Spring Boot
            context.getLogger().log("Handling as API Gateway proxy request");
            handler.proxyStream(new ByteArrayInputStream(inputBytes), outputStream, context);
        }
    }

    private Map<String, Object> generatePolicy(String principalId, String effect, String resource) {
        Map<String, Object> statement = new HashMap<>();
        statement.put("Action", "execute-api:Invoke");
        statement.put("Effect", effect);
        statement.put("Resource", resource);

        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put("Version", "2012-10-17");
        policyDocument.put("Statement", List.of(statement));

        Map<String, Object> policy = new HashMap<>();
        policy.put("principalId", principalId);
        policy.put("policyDocument", policyDocument);

        return policy;
    }
}
