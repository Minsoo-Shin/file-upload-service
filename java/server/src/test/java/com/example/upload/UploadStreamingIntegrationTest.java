package com.example.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UploadStreamingIntegrationTest {

    static Path storageDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        storageDir = Files.createTempDirectory("upload-stream-it");
        registry.add("upload.storage.dir", storageDir::toString);
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("스트리밍 PUT이 파일을 저장하고 저장 바이트가 일치한다 (end-to-end)")
    void putStream_storesFileBytes_andReturns201() throws Exception {
        byte[] content = "streamed content 1234567890".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(put("/api/files")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header("X-Filename", "big.mp4")
                        .content(content))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("big.mp4"))
                .andExpect(jsonPath("$.size").value(content.length));

        // 신선한 임시 디렉터리라 업로드 1건 → 정확히 1개 파일이 저장돼 있어야 한다.
        try (Stream<Path> entries = Files.list(storageDir)) {
            List<Path> files = entries.toList();
            assertThat(files).hasSize(1);
            assertThat(Files.readAllBytes(files.get(0))).isEqualTo(content);
        }
    }
}
