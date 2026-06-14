package com.example.upload.service;

/**
 * 업로드 파일명이 유효하지 않을 때(빈 값, path traversal 등) 던진다.
 */
public class InvalidFilenameException extends RuntimeException {

    public InvalidFilenameException(String message) {
        super(message);
    }
}
