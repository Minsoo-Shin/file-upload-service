package com.example.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UploadIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @DynamicPropertySource
    static void storageDir(DynamicPropertyRegistry registry) {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "upload-it-" + UUID.randomUUID());
        registry.add("upload.storage.dir", tempDir::toString);
    }

    @Test
    @DisplayName("업로드 요청이 파일을 저장하고 201과 메타를 반환한다 (end-to-end)")
    void upload_storesFile_andReturns201() throws Exception {
        var file = new MockMultipartFile("file", "clip.mp4", "video/mp4",
                "hello world".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.filename").value("clip.mp4"))
                .andExpect(jsonPath("$.size").value(11));
    }
}
