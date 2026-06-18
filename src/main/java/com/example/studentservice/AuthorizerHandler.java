package com.example.studentservice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Separate Lambda Authorizer Handler
 * Deploy this as a NEW Lambda function (e.g., student-authorizer)
 * Handler: com.example.studentservice.AuthorizerHandler::handleRequest
 */
public class AuthorizerHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    // Replace with your real token or fetch from Parameter Store / Secrets Manager
    private static final String VALID_TOKEN = "bearer-toker";

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Authorizer event: " + event);

        String token = (String) event.get("authorizationToken");
        String methodArn = (String) event.get("methodArn");

        if (token != null && token.equals(VALID_TOKEN)) {
            context.getLogger().log("Token valid - allowing request");
            return generatePolicy("user", "Allow", methodArn);
        }

        context.getLogger().log("Token invalid - denying request");
        return generatePolicy("user", "Deny", methodArn);
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

