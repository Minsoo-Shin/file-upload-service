package com.example.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.example.upload.BenchmarkSupport.HeapSampler;
import static com.example.upload.BenchmarkSupport.multipartBody;
import static com.example.upload.BenchmarkSupport.report;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 업로드 벤치마크 (file-size-threshold 큼 = 멀티파트를 메모리에 보관). 분리 태스크 `benchmarkTest`(-Xmx2g).
 * 대조군: 같은 256MB를 메모리 보관 멀티파트로 받으면 peak 힙이 페이로드만큼 치솟는지 확인.
 */
@Tag("benchmark")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.servlet.multipart.file-size-threshold=1GB",
        "spring.servlet.multipart.max-file-size=1GB",
        "spring.servlet.multipart.max-request-size=1GB"
})
class MultipartInMemoryBenchmarkTest {

    static final long PAYLOAD = 256L * 1024 * 1024;

    static Path storageDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        storageDir = Files.createTempDirectory("upload-bench-mem");
        registry.add("upload.storage.dir", storageDir::toString);
    }

    @LocalServerPort
    int port;

    final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("multipart POST (threshold 큼, 메모리 보관)")
    void multipartInMemory() throws Exception {
        String boundary = "BENCHBOUNDARY";
        try (HeapSampler sampler = new HeapSampler()) {
            long t0 = System.nanoTime();
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/files"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofInputStream(
                            () -> multipartBody(boundary, "bench.bin", PAYLOAD)))
                    .build();
            int status = client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
            long latencyMs = (System.nanoTime() - t0) / 1_000_000;

            assertThat(status).isEqualTo(201);
            report("multipart (in-memory)", PAYLOAD / (1024 * 1024), latencyMs, sampler.peakDeltaMb());
        }
    }
}
