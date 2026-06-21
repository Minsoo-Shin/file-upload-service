# Plan: M2 스트리밍 저장

**Source PRD**: docs/PRD.md
**Selected Milestone**: M2 — 스트리밍 저장 ("10GB도 서버 메모리 폭증 없이 저장")
**Complexity**: Medium

## Summary
요청 본문을 **메모리에 통째로 올리지 않고 스트림으로** 저장하는 intake를 추가한다.
저장 계층(`LocalFileStorage`)은 이미 `Files.copy(InputStream)`로 스트리밍하므로, 이번 단계의
핵심은 **intake 경로**다. 원본 바디(`application/octet-stream`)를 직접 `FileStorage`로 흘려보내
멀티파트 임시파일↔저장소 **이중 쓰기**를 없애고, 상수 메모리로 처리됨을 테스트로 검증한다.

## 현재 상태 (grounding)
- `LocalFileStorage.store` (`storage/LocalFileStorage.java:32`) — `Files.copy(InputStream)` 이미 스트리밍 ✅
- `FileStorageService.store(MultipartFile)` (`service/FileStorageService.java:30`) — `file.getInputStream()` 사용
- 멀티파트는 디스크로 spill되지만(서블릿 기본), 요청이 `max-file-size`(100MB)에 묶이고 이중 쓰기 발생

## Decision to confirm
- **스트리밍 intake 방식**: `PUT /api/files`(또는 `POST /api/files/stream`), `Content-Type: application/octet-stream`,
  원본 파일명은 `X-Filename` 헤더로 받아 sanitize. 본문은 `InputStream`으로 받아 바로 저장. ← 동의?
- M1 멀티파트 엔드포인트(`POST /api/files`)는 **유지**(소형/폼 업로드용). 대용량은 스트리밍 엔드포인트로.
- 대용량 한계: 스트리밍 경로는 `multipart.max-file-size`에 안 묶임. 서블릿 `max-http-request-size`(또는 비제한) 점검.

## Patterns to Mirror
| Category | Source | Pattern |
|---|---|---|
| 저장 추상화 | `storage/FileStorage.java`, `LocalFileStorage.java` | 기존 `store(key, InputStream)` 재사용 |
| 서비스 sanitize | `service/FileStorageService.java` | 파일명 검증 로직 재사용/공유 |
| REST | `web/FileUploadController.java` | ResponseEntity 201 + `UploadResponse` |
| 테스트 | `springboot-tdd`, `rules/java/testing.md` | MockMvc, `@SpringBootTest`, AssertJ |

## Files to Change
| File | Action | Why |
|---|---|---|
| `service/FileStorageService.java` | UPDATE | `store(String filename, InputStream)` 오버로드 추가(스트리밍), sanitize 공유 |
| `web/FileUploadController.java` | UPDATE | `PUT /api/files` 스트리밍 핸들러 추가(octet-stream + `X-Filename`) |
| `test/.../service/FileStorageServiceTest.java` | UPDATE | 스트리밍 store 단위 테스트(RED) |
| `test/.../web/FileUploadControllerTest.java` | UPDATE | PUT 스트리밍 컨트롤러 테스트(RED) |
| `test/.../UploadStreamingIntegrationTest.java` | CREATE | 큰(예: 64MB) 합성 페이로드 end-to-end + 바이트 검증 |
| `application.properties` | UPDATE | 스트리밍 경로용 요청 크기 정책 점검/주석 |

## Tasks (TDD)
### Task 1 (RED): 서비스 스트리밍 store
- **Action**: `FileStorageServiceTest` — `store(filename, inputStream)` 가 sanitize·저장·메타 반환, traversal 거부
- **Mirror**: 기존 sanitize/`StoredFile`

### Task 2 (GREEN): 서비스 구현
- **Action**: `store(String, InputStream)` 오버로드 추가, 기존 sanitize 공유, `storage.store(id, in)` 위임

### Task 3 (RED): 스트리밍 컨트롤러
- **Action**: `FileUploadControllerTest` — `PUT /api/files` octet-stream + `X-Filename` → 201 메타, 파일명 없으면 400

### Task 4 (GREEN): 컨트롤러 구현
- **Action**: `@PutMapping(consumes=APPLICATION_OCTET_STREAM)` 핸들러, `InputStream`/`HttpServletRequest.getInputStream()` 직결

### Task 5: 메모리 안전 통합 검증
- **Action**: `UploadStreamingIntegrationTest` — 64MB 합성 스트림 PUT → 저장 크기 일치 확인
- **선택**: 테스트 JVM 힙을 제한(`-Xmx256m`)해 "파일보다 작은 힙으로도 성공"을 시연 (gradle test jvmArgs) — 동의 시 포함

## Validation
```bash
cd java/server && ./gradlew test
```

## Risks
| Risk | Likelihood | Mitigation |
|---|---|---|
| "메모리 안전"을 테스트로 증명하기 어려움 | High | 제한 힙(-Xmx)으로 큰 페이로드 성공을 시연; 바운드 버퍼 복사 확인 |
| 64MB 테스트가 느림/디스크 사용 | Medium | 합성 스트림(생성형), 임시 디렉터리, 테스트 후 정리 |
| 스트리밍 경로의 요청 크기 제한 충돌 | Medium | octet-stream 경로는 multipart 한도와 분리, 서버 max-request-size 점검 |

## Acceptance
- [ ] `./gradlew test` 통과 (M1 회귀 없음)
- [ ] `PUT /api/files` (octet-stream)로 대용량 스트리밍 저장 + 메타 응답
- [ ] 제한 힙에서 파일보다 큰 입력이 OOM 없이 저장됨(시연, 선택 시)
- [ ] `docs/devlog/02-*.md` 작성
