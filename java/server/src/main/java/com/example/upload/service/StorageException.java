package com.example.upload.service;

/**
 * 저장 과정에서 복구 불가능한 I/O 오류가 났을 때 던진다.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
