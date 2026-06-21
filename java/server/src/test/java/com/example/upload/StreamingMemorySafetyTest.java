package com.example.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 메모리 안전 시연 (분리 태스크 `memoryTest`, 힙 -Xmx512m).
 *
 * 힙(512MB)보다 큰 1GB를 즉석 생성 스트림으로 PUT → OOM 없이 저장되면,
 * intake가 파일 크기에 비례해 메모리를 쓰지 않음(상수 메모리 스트리밍)을 증명한다.
 * 클라이언트도 통짜 byte[] 없이 생성형 InputStream으로 보내 양쪽 모두 버퍼링하지 않음을 보인다.
 */
@Tag("memory")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StreamingMemorySafetyTest {

    private static final long PAYLOAD_BYTES = 1024L * 1024 * 1024; // 1GB ≫ 512MB heap

    static Path storageDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        storageDir = Files.createTempDirectory("upload-mem-safety");
        registry.add("upload.storage.dir", storageDir::toString);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("힙(512MB)보다 큰 1GB 스트림이 OOM 없이 저장된다")
    void streamsPayloadLargerThanHeap_withoutOom() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/files"))
                .header("Content-Type", "application/octet-stream")
                .header("X-Filename", "huge.bin")
                .PUT(HttpRequest.BodyPublishers.ofInputStream(GeneratingInputStream::new))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);

        try (Stream<Path> entries = Files.list(storageDir)) {
            List<Path> files = entries.toList();
            assertThat(files).hasSize(1);
            assertThat(Files.size(files.get(0))).isEqualTo(PAYLOAD_BYTES);
        }
    }

    /** {@link #PAYLOAD_BYTES} 바이트를 즉석 생성하는 스트림 (전체를 메모리에 들고 있지 않음). */
    private static final class GeneratingInputStream extends InputStream {
        private long remaining = PAYLOAD_BYTES;

        @Override
        public int read() {
            if (remaining <= 0) {
                return -1;
            }
            remaining--;
            return 'A';
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (remaining <= 0) {
                return -1;
            }
            int n = (int) Math.min(len, remaining);
            Arrays.fill(b, off, off + n, (byte) 'A');
            remaining -= n;
            return n;
        }
    }
}
