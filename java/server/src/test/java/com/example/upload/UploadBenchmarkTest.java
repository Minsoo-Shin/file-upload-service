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
import java.util.concurrent.Callable;

import static com.example.upload.BenchmarkSupport.HeapSampler;
import static com.example.upload.BenchmarkSupport.generatingStream;
import static com.example.upload.BenchmarkSupport.multipartBody;
import static com.example.upload.BenchmarkSupport.report;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 업로드 벤치마크 (threshold=0 = 멀티파트는 디스크 spool). 분리 태스크 `benchmarkTest`(-Xmx2g).
 * 동일 256MB 페이로드를 스트리밍 PUT vs 멀티파트(디스크)로 보내 latency·peak 힙 비교.
 */
@Tag("benchmark")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.servlet.multipart.file-size-threshold=0",
        "spring.servlet.multipart.max-file-size=1GB",
        "spring.servlet.multipart.max-request-size=1GB"
})
class UploadBenchmarkTest {

    static final long PAYLOAD = 256L * 1024 * 1024;

    static Path storageDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws IOException {
        storageDir = Files.createTempDirectory("upload-bench-disk");
        registry.add("upload.storage.dir", storageDir::toString);
    }

    @LocalServerPort
    int port;

    final HttpClient client = HttpClient.newHttpClient();

    @Test
    @DisplayName("octet-stream PUT (스트리밍)")
    void streamingPut() throws Exception {
        measure("octet-stream PUT (stream)", () -> {
            HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/api/files"))
                    .header("Content-Type", "application/octet-stream")
                    .header("X-Filename", "bench.bin")
                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> generatingStream(PAYLOAD)))
                    .build();
            return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
        });
    }

    @Test
    @DisplayName("multipart POST (threshold=0, 디스크 spool)")
    void multipartDisk() throws Exception {
        String boundary = "BENCHBOUNDARY";
        measure("multipart (disk spool)", () -> {
            HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/api/files"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofInputStream(
                            () -> multipartBody(boundary, "bench.bin", PAYLOAD)))
                    .build();
            return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
        });
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private void measure(String label, Callable<Integer> upload) throws Exception {
        try (HeapSampler sampler = new HeapSampler()) {
            long t0 = System.nanoTime();
            int status = upload.call();
            long latencyMs = (System.nanoTime() - t0) / 1_000_000;
            assertThat(status).isEqualTo(201);
            report(label, PAYLOAD / (1024 * 1024), latencyMs, sampler.peakDeltaMb());
        }
    }
}
