package com.example.upload.service;

import com.example.upload.storage.FileStorage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String originalFilename = sanitize(file.getOriginalFilename());
        String id = UUID.randomUUID().toString();
        try {
            storage.store(id, file.getInputStream());
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + originalFilename, e);
        }
        return new StoredFile(id, originalFilename, file.getSize());
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
