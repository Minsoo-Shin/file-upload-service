package com.example.upload.web;

import com.example.upload.service.StoredFile;

/**
 * 업로드 성공 응답 본문.
 */
public record UploadResponse(String id, String filename, long size) {

    public static UploadResponse from(StoredFile stored) {
        return new UploadResponse(stored.id(), stored.originalFilename(), stored.size());
    }
}
