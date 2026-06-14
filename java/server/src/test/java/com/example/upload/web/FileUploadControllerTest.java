package com.example.upload.web;

import com.example.upload.service.FileStorageService;
import com.example.upload.service.InvalidFilenameException;
import com.example.upload.service.StoredFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileUploadController.class)
class FileUploadControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FileStorageService fileStorageService;

    @Test
    @DisplayName("multipart 업로드 시 201과 메타를 반환한다")
    void upload_returns201WithMetadata() throws Exception {
        var file = new MockMultipartFile("file", "video.mp4", "video/mp4",
                "data".getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.store(any())).thenReturn(new StoredFile("abc-123", "video.mp4", 4L));

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("abc-123"))
                .andExpect(jsonPath("$.filename").value("video.mp4"))
                .andExpect(jsonPath("$.size").value(4));
    }

    @Test
    @DisplayName("잘못된 파일명이면 400을 반환한다")
    void upload_returns400_onInvalidFilename() throws Exception {
        var file = new MockMultipartFile("file", "bad", "text/plain",
                "x".getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.store(any())).thenThrow(new InvalidFilenameException("bad name"));

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isBadRequest());
    }
}
