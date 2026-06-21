package com.example.upload.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 로컬 파일시스템 저장 구현. 모든 파일을 설정된 base 디렉터리 아래에 둔다.
 */
@Component
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;

    public LocalFileStorage(@Value("${upload.storage.dir:./storage}") String dir) {
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create storage dir: " + baseDir, e);
        }
    }

    @Override
    public long store(String key, InputStream content) throws IOException {
        Path target = baseDir.resolve(key).normalize();
        // base 디렉터리를 벗어나는 경로(예: key에 ../)를 차단한다.
        if (!target.startsWith(baseDir)) {
            throw new IOException("Resolved path escapes base dir: " + key);
        }
        try (content) {
            // Files.copy는 스트림을 버퍼 단위로 복사하며 기록한 바이트 수를 반환한다.
            return Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
