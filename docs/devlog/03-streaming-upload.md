# Devlog 03 — M2 스트리밍 저장

날짜: 2026-06-21 · 상태: 완료 · 계획: [m2-streaming.plan.md](../plans/m2-streaming.plan.md)
선행: [devlog 02 — 멀티파트 메모리/디스크 검증](02-multipart-memory-vs-disk.md)

## 무엇을
- 스트리밍 intake 추가: `PUT /api/files` (`application/octet-stream` + `X-Filename` 헤더),
  원본 바디를 `InputStream`으로 받아 버퍼링 없이 저장.
- `FileStorage.store`를 `void → long`(기록 바이트)로 변경 — 스트리밍은 크기를 사전에 모름.
- `FileStorageService`: M1 멀티파트 경로를 **공통 스트리밍 코어로 위임**(중복 제거).
- M1 멀티파트(`POST /api/files`)는 유지(폼/소형용).
- 테스트: 기본 15개(서비스 5·컨트롤러 4·통합 2·스파이크 3·smoke 1) + 분리 태스크 `memoryTest` 1개.

## 왜
- 검증(devlog 02): 멀티파트는 기본 디스크 spool이지만 **임시파일↔저장소 이중 쓰기** + `max-file-size` 제약.
- → raw 바디 직결로 이중 쓰기 제거, 대용량(10GB) 제약 해소, 상수 메모리.

## 메모리 안전 증명
- `StreamingMemorySafetyTest`(`@Tag("memory")`, 분리 태스크 `./gradlew memoryTest`, `-Xmx512m`).
- 즉석 생성 스트림으로 **1GB**를 PUT → **OOM 없이 1.9초만에 저장**(힙 512MB ≪ 1GB).
- 클라이언트도 통짜 `byte[]` 없이 생성형 `InputStream`(JDK HttpClient `ofInputStream`)으로 전송 →
  **양쪽 모두 파일 크기에 비례한 메모리를 쓰지 않음**을 보임.

## 막힌 점 / 배운 것
- Spring Boot 4는 **Jackson 3(`tools.jackson.*`)** — `com.fasterxml.jackson.databind` 없음.
  통합 테스트에서 `ObjectMapper` 대신 **저장 디렉터리 리스팅**으로 바이트 검증(의존성 회피).
- (스파이크에서) `RestClient` 멀티파트는 `reactivestreams` 필요 → `RestTemplate`/JDK `HttpClient` 사용.
- `InputStream` 핸들러 인자 = 서블릿 입력 스트림 직결 → 진짜 스트리밍.

## 다음 (M3)
검증 & 보안 — 크기 상한, MIME/매직넘버, 파일명 sanitize 강화(이미 일부), path traversal 테스트 확대.
