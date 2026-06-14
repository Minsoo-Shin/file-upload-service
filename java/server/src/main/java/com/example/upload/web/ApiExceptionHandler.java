package com.example.upload.web;

import com.example.upload.service.InvalidFilenameException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 잘못된 업로드 요청을 안전한 일반 메시지로 매핑한다(내부 정보 비노출).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({InvalidFilenameException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
