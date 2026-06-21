package com.example.upload.service;

import com.example.upload.storage.FileStorage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 업로드 파일을 검증·sanitize한 뒤 {@link FileStorage}에 저장하고 메타를 반환한다.
 */
@Service
public class FileStorageService {

    private final FileStorage storage;

    public FileStorageService(FileStorage storage) {
        this.storage = storage;
    }

    /** 멀티파트 업로드(M1). 공통 스트리밍 경로로 위임한다. */
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return store(file.getOriginalFilename(), open(file));
    }

    /** 스트리밍 업로드(M2). 바디를 메모리에 통째로 올리지 않고 저장에 흘려보낸다. */
    public StoredFile store(String filename, InputStream content) {
        String originalFilename = sanitize(filename);
        String id = UUID.randomUUID().toString();
        long size;
        try {
            size = storage.store(id, content);
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + originalFilename, e);
        }
        return new StoredFile(id, originalFilename, size);
    }

    private static InputStream open(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }
    }

    /**
     * 파일명을 정리하고 경로 분리자/상위 참조가 섞인 위험한 이름을 거부한다.
     */
    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidFilenameException("Filename is required");
        }
        String cleaned = StringUtils.cleanPath(filename);
        if (cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\")) {
            throw new InvalidFilenameException("Invalid filename: " + filename);
        }
        return cleaned;
    }
}
