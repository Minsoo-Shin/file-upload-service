package com.example.upload.service;

/**
 * 저장 완료된 파일의 메타데이터.
 *
 * @param id               저장 식별자(UUID)
 * @param originalFilename sanitize된 원본 파일명
 * @param size             바이트 크기
 */
public record StoredFile(String id, String originalFilename, long size) {
}
