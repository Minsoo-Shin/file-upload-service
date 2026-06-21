package com.example.upload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 검증 스파이크(M2 전): Spring/Tomcat 멀티파트의 메모리↔디스크 동작 확인.
 *
 * 핵심 사실:
 *  - file-size-threshold 가 메모리/디스크 스위치 (이 값 초과 → location에 임시파일 spool)
 *  - max-file-size 는 허용 상한 (초과 → 요청 거부, 저장 안 함)
 *
 * MockMvc(MockMultipartFile)로는 재현 불가 → 실제 서버 + 실제 HTTP 멀티파트로 검증.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.servlet.multipart.file-size-threshold=64KB",
        "spring.servlet.multipart.max-file-size=1MB",
        "spring.servlet.multipart.max-request-size=10MB"
})
@Import(MultipartSpoolingExperimentTest.ProbeController.class)
class MultipartSpoolingExperimentTest {

    static Path spoolLocation;

    @DynamicPropertySource
    static void multipartLocation(DynamicPropertyRegistry registry) throws IOException {
        spoolLocation = Files.createTempDirectory("mp-spool-exp");
        registry.add("spring.servlet.multipart.location", spoolLocation::toString);
    }

    @LocalServerPort
    int port;

    final RestTemplate client = new RestTemplate();

    String url;

    @BeforeEach
    void setUp() {
        url = "http://localhost:" + port + "/probe";
    }

    @Test
    @DisplayName("threshold(64KB) 미만 → 메모리 보관 (location에 임시파일 없음)")
    void belowThreshold_keptInMemory() {
        Map<?, ?> result = probe(10 * 1024); // 10KB

        assertThat(((Number) result.get("tempFilesInLocation")).intValue()).isZero();
        assertThat((Boolean) result.get("inMemory")).isTrue();
    }

    @Test
    @DisplayName("threshold 초과·max 미만(256KB) → 디스크로 spool (location에 임시파일 생김)")
    void aboveThreshold_spooledToDisk() {
        Map<?, ?> result = probe(256 * 1024); // 256KB

        assertThat(((Number) result.get("tempFilesInLocation")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat((Boolean) result.get("inMemory")).isFalse();
    }

    @Test
    @DisplayName("max-file-size(1MB) 초과(1.5MB) → 요청 거부 (메모리/디스크 무관)")
    void aboveMaxFileSize_rejected() {
        assertThatThrownBy(() -> probe(1536 * 1024)) // 1.5MB
                .isInstanceOf(RestClientResponseException.class);
    }

    private Map<?, ?> probe(int sizeBytes) {
        byte[] payload = new byte[sizeBytes];
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", namedResource(payload, "probe.bin"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return client.postForObject(url, new HttpEntity<>(parts, headers), Map.class);
    }

    private static Resource namedResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    /**
     * 요청 처리 시점에 멀티파트 location 디렉터리에 임시파일이 있는지 보고하는 프로브.
     * 이 테스트에서만 @Import로 등록한다(컴포넌트 스캔 대상 아님).
     */
    @RestController
    static class ProbeController {
        private final Path location;

        ProbeController(@Value("${spring.servlet.multipart.location}") String location) {
            this.location = Path.of(location);
        }

        @PostMapping("/probe")
        Map<String, Object> probe(@RequestParam("file") MultipartFile file) throws IOException {
            long tempFiles;
            try (Stream<Path> entries = Files.list(location)) {
                tempFiles = entries.count();
            }
            return Map.of(
                    "size", file.getSize(),
                    "tempFilesInLocation", tempFiles,
                    "inMemory", tempFiles == 0);
        }
    }
}
