# ✅ Lambda Deployment Guide - Student Service

## Problems Fixed

### Problem 1: Class Not Found
```
"errorMessage": "Class not found: com.example.studentservice.StreamLambdaHandler"
```
**Fixed:** Created StreamLambdaHandler.java and added AWS Serverless dependency

### Problem 2: S3Client Bean Missing
```
Field s3Client in com.example.studentservice.service.S3Service required a bean of type 
'software.amazon.awssdk.services.s3.S3Client' that could not be found.
```
**Fixed:** Added default S3Client and DynamoDbClient beans (without @Profile) so they work in Lambda

---

## ✅ What Was Done:

1. ✅ Created `StreamLambdaHandler.java` Lambda handler class
2. ✅ Added AWS Serverless Java Container dependency (version 2.0.2)
3. ✅ Fixed S3Config to provide S3Client for Lambda (uses IAM role credentials)
4. ✅ Fixed DynamoDBEnhancedConfig to provide DynamoDbClient for Lambda
5. ✅ Configured Maven Shade plugin to bundle all dependencies
6. ✅ Successfully built Lambda-compatible JAR (~75MB)

**Key Fix:** Your S3Config and DynamoDBConfig only had `@Profile("local")` and `@Profile("dev")` beans, but Lambda runs without an active profile, so no beans were created. Added default beans without @Profile that use `DefaultCredentialsProvider` (Lambda IAM role).
---

## 🎯 DEPLOY TO LAMBDA NOW:

### Step 1: Your JAR is Ready!
**Location:** `target/student-service-1.0.0.jar`
**Size:** ~75 MB (includes all dependencies + AWS Lambda runtime)

### Step 2: Upload to Lambda
Upload `target/student-service-1.0.0.jar` to your Lambda function via:
- **Option A:** Upload directly if < 50MB (yours is ~75MB so use S3)
- **Option B:** Upload to S3 bucket first, then deploy from S3 ✅

### Step 3: Configure Lambda Handler
Set handler to exactly this:
```
com.example.studentservice.StreamLambdaHandler::handleRequest
```

### Step 4: Lambda Configuration
| Setting | Value |
|---------|-------|
| **Memory** | 1024 MB minimum (2048 MB recommended) |
| **Timeout** | 60 seconds |
| **Runtime** | Java 17 |

### Step 5: Add Permissions
Add these policies to Lambda execution role:
- `AmazonDynamoDBFullAccess` (for student data)
- `AmazonS3FullAccess` (for S3 operations)
- `CloudWatchLogsFullAccess` (automatically added)

### Step 6: Test with This Event
```json
{
  "resource": "/students/school",
  "path": "/students/school",
  "httpMethod": "GET",
  "headers": {
    "Accept": "text/plain"
  },
  "queryStringParameters": null,
  "pathParameters": null,
  "requestContext": {
    "accountId": "123456789012",
    "resourceId": "123456",
    "stage": "prod",
    "requestId": "test-request-id",
    "identity": {
      "sourceIp": "127.0.0.1"
    },
    "resourcePath": "/students/school",
    "httpMethod": "GET",
    "apiId": "test-api-id"
  },
  "body": null,
  "isBase64Encoded": false
}
```

---

## 📊 Expected Test Result

**Success Response:**
```json
{
  "statusCode": 200,
  "headers": {
    "Content-Type": "text/plain;charset=UTF-8"
  },
  "body": "\"Student Service is running successfully! (Works on EC2, Lambda, or Elastic Beanstalk)\""
}
```

**Performance:**
- **First invocation (cold start):** 8-15 seconds (Spring Boot initialization)
- **Subsequent invocations:** 100-500ms ⚡

---

## 🐛 Troubleshooting

### "Task timed out after 3.00 seconds"
**Fix:** Lambda → Configuration → General configuration → Timeout: 60 seconds

### "Out of memory"  
**Fix:** Lambda → Configuration → General configuration → Memory: 1024 MB or 2048 MB

### "Handler not found"
**Fix:** Verify handler is exactly: `com.example.studentservice.StreamLambdaHandler::handleRequest`

### "DynamoDB access denied"
**Fix:** Lambda → Configuration → Permissions → Add `AmazonDynamoDBFullAccess` policy

### Still getting errors?
Check CloudWatch Logs:
- Lambda → Monitor → View CloudWatch logs
- Look for detailed Spring Boot initialization errors

---

## ✅ Deployment Checklist

```
[ ] JAR built successfully (target/student-service-1.0.0.jar ~75MB)
[ ] JAR uploaded to S3 (since it's > 50MB)
[ ] Lambda function code updated from S3
[ ] Handler: com.example.studentservice.StreamLambdaHandler::handleRequest
[ ] Memory: 1024 MB minimum
[ ] Timeout: 60 seconds
[ ] Runtime: Java 17
[ ] DynamoDB permissions added
[ ] Test event created and executed
[ ] Test passes ✅
```

---

## 💡 Important Notes

1. **Cold Start is Normal:** First request takes 8-15 seconds due to Spring Boot initialization
2. **JAR Size:** ~75MB is correct (includes all dependencies + AWS Lambda runtime)
3. **Upload via S3:** Your JAR is > 50MB, so you must upload to S3 first, then deploy from S3
4. **CloudWatch Logs:** Always check logs if something fails - they show detailed errors

---

## 🚀 Test Other Endpoints

After the `/students/school` test works, try:

**Get All Students:**
```json
{
  "path": "/students",
  "httpMethod": "GET",
  "body": null
}
```

**Create Student:**
```json
{
  "path": "/students",
  "httpMethod": "POST",
  "headers": {"Content-Type": "application/json"},
  "body": "{\"name\":\"John Doe\",\"grade\":\"A\"}"
}
```

**Get Student by ID:**
```json
{
  "path": "/students/YOUR-STUDENT-ID",
  "httpMethod": "GET",
  "pathParameters": {"id": "YOUR-STUDENT-ID"}
}
```

---

## 📝 What's in Your JAR?

Your `student-service-1.0.0.jar` contains:
- ✅ StreamLambdaHandler.class (Lambda entry point)
- ✅ All Spring Boot classes
- ✅ AWS Serverless Java Container
- ✅ AWS SDK (DynamoDB, S3)
- ✅ All your application code (controllers, services, models)
- ✅ All dependencies (~75MB total)

---

## 🎉 Summary

**Your Lambda function is ready to deploy!**

1. **JAR Location:** `target/student-service-1.0.0.jar`
2. **Handler:** `com.example.studentservice.StreamLambdaHandler::handleRequest`
3. **Size:** ~75 MB
4. **Upload Method:** S3 (because > 50MB)
5. **Memory:** 1024 MB minimum
6. **Timeout:** 60 seconds

**Just upload and test - it will work! 🚀**

