package com.example.upload.web;

import com.example.upload.service.FileStorageService;
import com.example.upload.service.StoredFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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

    /** 멀티파트 업로드 (M1) — 폼/소형 파일용. */
    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        StoredFile stored = fileStorageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(UploadResponse.from(stored));
    }

    /** 스트리밍 업로드 (M2) — 원본 바디를 버퍼링 없이 저장. 대용량용. */
    @PutMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<UploadResponse> uploadStream(
            @RequestHeader("X-Filename") String filename,
            InputStream body) {
        StoredFile stored = fileStorageService.store(filename, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(UploadResponse.from(stored));
    }
}
