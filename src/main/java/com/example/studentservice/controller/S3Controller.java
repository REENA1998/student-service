package com.example.studentservice.controller;

import com.example.studentservice.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class S3Controller {
    @Autowired
    private S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException, IOException {
        s3Service.uploadFile(file);
        return ResponseEntity.ok("File uploaded successfully!");
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> download(@PathVariable String filename) {
        byte[] data = s3Service.downloadFile(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(data);
    }

    @DeleteMapping("/delete-version")
    public String deleteVersion(@RequestParam String key,
                                @RequestParam String versionId) {
        s3Service.deleteSpecificVersion(key, versionId);
        return "Deleted specific version";
    }

    @GetMapping("/files")
    public List<String> listFiles() {
        return s3Service.listFiles();
    }
    @GetMapping("/presigned-url")
    public String getPresignedUrl(@RequestParam String key) {
        return s3Service.generatePresignedUrl(key);
    }

}
