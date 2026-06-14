package com.example.upload.web;

import com.example.upload.service.FileStorageService;
import com.example.upload.service.StoredFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 단일 파일 업로드 엔드포인트 (M1).
 */
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        StoredFile stored = fileStorageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(UploadResponse.from(stored));
    }
}
