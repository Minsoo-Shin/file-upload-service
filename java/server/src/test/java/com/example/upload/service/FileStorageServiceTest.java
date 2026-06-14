package com.example.upload.service;

import com.example.upload.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    FileStorage storage;

    @InjectMocks
    FileStorageService service;

    @Test
    @DisplayName("정상 파일을 저장하고 메타(id·원본명·크기)를 반환한다")
    void store_returnsMetadata_andDelegatesToStorage() throws IOException {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "video.mp4", "video/mp4", body);

        StoredFile result = service.store(file);

        assertThat(result.id()).isNotBlank();
        assertThat(result.originalFilename()).isEqualTo("video.mp4");
        assertThat(result.size()).isEqualTo(body.length);
        verify(storage).store(eq(result.id()), any(InputStream.class));
    }

    @Test
    @DisplayName("path traversal 파일명은 거부한다")
    void store_rejectsPathTraversalFilename() {
        var file = new MockMultipartFile("file", "../../etc/passwd", "text/plain",
                "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(InvalidFilenameException.class);
    }

    @Test
    @DisplayName("빈 파일은 거부한다")
    void store_rejectsEmptyFile() {
        var file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
