package com.example.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 업로드 벤치마크 공용 유틸 — peak 힙 샘플러, 생성형(메모리 비점유) 스트림, 멀티파트 인코더,
 * 결과 리포트. 측정 전용이며 프로덕션 코드와 무관하다.
 */
final class BenchmarkSupport {

    private BenchmarkSupport() {
    }

    /** 백그라운드로 힙 사용량을 샘플링해 baseline 대비 peak 증가분을 기록한다. */
    static final class HeapSampler implements AutoCloseable {
        private final MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
        private final long baseline;
        private volatile long peak;
        private volatile boolean running = true;
        private final Thread thread;

        HeapSampler() {
            System.gc();
            this.baseline = used();
            this.peak = baseline;
            this.thread = new Thread(() -> {
                while (running) {
                    long u = used();
                    if (u > peak) {
                        peak = u;
                    }
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "heap-sampler");
            thread.setDaemon(true);
            thread.start();
        }

        private long used() {
            return bean.getHeapMemoryUsage().getUsed();
        }

        long peakDeltaMb() {
            return (peak - baseline) / (1024 * 1024);
        }

        @Override
        public void close() {
            running = false;
            thread.interrupt();
        }
    }

    /** {@code size} 바이트('A')를 즉석 생성하는 스트림. 전체를 메모리에 들고 있지 않는다. */
    static InputStream generatingStream(long size) {
        return new InputStream() {
            private long remaining = size;

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
        };
    }

    /** multipart/form-data 본문을 스트리밍으로 생성한다(헤더 + 생성형 바디 + 푸터). */
    static InputStream multipartBody(String boundary, String filename, long size) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        return new SequenceInputStream(Collections.enumeration(List.of(
                new ByteArrayInputStream(header.getBytes(StandardCharsets.US_ASCII)),
                generatingStream(size),
                new ByteArrayInputStream(footer.getBytes(StandardCharsets.US_ASCII)))));
    }

    /** 결과 1줄을 stdout + build/benchmark-results.md 에 기록. */
    static synchronized void report(String path, long payloadMb, long latencyMs, long peakHeapMb) {
        String line = String.format("| %-26s | %6d MB | %8d ms | %9d MB |",
                path, payloadMb, latencyMs, peakHeapMb);
        System.out.println("[BENCHMARK] " + line);
        try {
            Path out = Path.of("build", "benchmark-results.md");
            Files.createDirectories(out.getParent());
            Files.writeString(out, line + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
