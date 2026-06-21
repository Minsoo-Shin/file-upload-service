# Plan: M2 스트리밍 저장 (상세)

**Source PRD**: docs/PRD.md
**Selected Milestone**: M2 — 스트리밍 저장 ("10GB도 서버 메모리 폭증 없이 저장")
**Complexity**: Medium

## Summary
원본 바디를 **메모리에 통째로 올리지 않고 스트림으로** 저장하는 intake를 추가한다.
저장 계층은 이미 `Files.copy(InputStream)`로 스트리밍하므로 핵심은 **intake 경로**다.
검증 스파이크([devlog 02](../devlog/02-multipart-memory-vs-disk.md)) 결론: 멀티파트는 기본적으로
디스크로 spool되지만 **임시파일↔저장소 이중 쓰기** + `max-file-size` 제약이 있다. M2는 raw
바디 직결로 이를 없앤다.

## API 계약 (신규 스트리밍 엔드포인트)
```
PUT /api/files
  Content-Type: application/octet-stream        (필수)
  X-Filename: <원본 파일명>                       (필수, sanitize 대상)
  Body: raw bytes (파일 내용; 버퍼링 없이 스트림)

201 Created
  { "id": "<uuid>", "filename": "<sanitized>", "size": <bytesWritten> }

400  X-Filename 누락/공백, 또는 traversal('..','/','\\') 포함
500  저장 중 I/O 오류
```
- 기존 `POST /api/files`(멀티파트, M1)는 **유지** — 폼/소형 업로드용.
- size는 멀티파트와 달리 사전에 알 수 없으므로 **실제 기록된 바이트 수**를 사용한다.

## 인터페이스 리팩터 (M1·M2 공유)
`FileStorage.store`가 **기록한 바이트 수를 반환**하도록 바꾼다 (스트리밍은 size를 사전에 모름):
```java
// before
void store(String key, InputStream content) throws IOException;
// after
long store(String key, InputStream content) throws IOException;   // bytes written
```
- `LocalFileStorage`: `Files.copy(...)`가 이미 `long`(복사 바이트)을 반환 → 그대로 반환.
- `FileStorageService`: M1 `store(MultipartFile)`도 공통 코어로 위임해 중복 제거(altitude):
```java
public StoredFile store(MultipartFile file) {            // M1: 멀티파트
    if (file == null || file.isEmpty()) throw new IllegalArgumentException("empty");
    return store(file.getOriginalFilename(), open(file));   // 공통 경로로 위임
}
public StoredFile store(String filename, InputStream content) {  // M2: 스트리밍
    String name = sanitize(filename);
    String id = UUID.randomUUID().toString();
    long size;
    try { size = storage.store(id, content); }
    catch (IOException e) { throw new StorageException("store failed: " + name, e); }
    return new StoredFile(id, name, size);
}
```
- M1 기존 동작 보존(테스트 회귀 없어야 함). 단, M1 size 의미가 `file.getSize()` → `bytesWritten`로
  바뀌지만 값은 동일(전체 저장)하므로 기존 단언 영향 없음.

## Files to Change
| File | Action | 내용 |
|---|---|---|
| `storage/FileStorage.java` | UPDATE | `store` 반환형 `void→long` |
| `storage/LocalFileStorage.java` | UPDATE | `Files.copy` 결과(long) 반환 |
| `service/FileStorageService.java` | UPDATE | `store(String, InputStream)` 추가, M1을 공통 경로로 위임 |
| `web/FileUploadController.java` | UPDATE | `PUT /api/files` (octet-stream, `X-Filename`, `InputStream` 바디) |
| `web/ApiExceptionHandler.java` | UPDATE(선택) | `MissingRequestHeaderException`→400 명시(또는 기본 400 활용) |
| `test/.../service/FileStorageServiceTest.java` | UPDATE | 스트리밍 store 단위 테스트 |
| `test/.../web/FileUploadControllerTest.java` | UPDATE | PUT 스트리밍 컨트롤러 테스트 |
| `test/.../UploadStreamingIntegrationTest.java` | CREATE | end-to-end 저장 검증(@SpringBootTest+MockMvc) |
| `test/.../StreamingMemorySafetyTest.java` | CREATE | 제한 힙 메모리 안전 시연(분리 태스크) |
| `build.gradle` | UPDATE | 메모리 시연용 테스트 태스크(`-Xmx`) + JUnit 태그 분리 |

## Tasks (TDD)
### Task 1 (RED): 서비스 스트리밍 store
`FileStorageServiceTest`에 추가:
- `storeStream_returnsMetadata_andDelegates`: `store("a.bin", inputStream)` → mocked `storage.store` 가 `11L` 반환 → `StoredFile{id 비어있지 않음, "a.bin", 11}`; `verify(storage).store(eq(id), any())`.
- `storeStream_rejectsTraversalFilename`: `store("../x", in)` → `InvalidFilenameException`.

### Task 2 (GREEN): 서비스/인터페이스 구현
- `FileStorage.store` → `long`, `LocalFileStorage` 반환, `FileStorageService` 위 시그니처. M1 위임.
- 전체 회귀: `./gradlew test` (M1 5 + 스파이크 3 그대로 통과).

### Task 3 (RED): 스트리밍 컨트롤러
`FileUploadControllerTest`에 추가(@WebMvcTest, service @MockitoBean):
- `putStream_returns201WithMetadata`: `PUT /api/files` + `X-Filename: clip.mp4` + octet-stream 바디 → 201, jsonPath(id/filename/size). `when(service.store(eq("clip.mp4"), any())).thenReturn(...)`.
- `putStream_returns400_whenFilenameHeaderMissing`: 헤더 없이 PUT → 400.

### Task 4 (GREEN): 컨트롤러 구현
- `@PutMapping(consumes = APPLICATION_OCTET_STREAM_VALUE)`, `@RequestHeader("X-Filename") String filename`, `InputStream body` 인자, `service.store(filename, body)` → 201.

### Task 5 (통합): end-to-end 저장
`UploadStreamingIntegrationTest`(@SpringBootTest + @AutoConfigureMockMvc, 임시 storage 디렉터리):
- `PUT /api/files`로 "hello world" 전송 → 201, 응답 size=11, **저장 파일 내용이 바이트 일치** 확인.

### Task 5b (메모리 안전 시연 — 분리 태스크)
- `StreamingMemorySafetyTest`(@SpringBootTest RANDOM_PORT): **즉석 생성 스트림**(byte[] 통짜 아님)으로
  큰 페이로드(예: 1GB)를 `RestTemplate.execute`의 RequestCallback에서 루프로 흘려보냄.
- gradle에 `-Xmx384m` 제한 테스트 태스크 + `@Tag("memory")`로 분리(기본 `test`에서 제외, 명시 실행).
- 성공 = 힙(384MB) ≪ 페이로드(1GB)인데 OOM 없이 저장 → **상수 메모리 스트리밍 증명**.
- 폴백: 1GB가 느리면 512MB로 축소(여전히 힙보다 큼).

## Validation
```bash
cd java/server && ./gradlew test                 # 기능/회귀 (Task 1~5)
cd java/server && ./gradlew memoryTest            # 메모리 시연 (Task 5b, 분리 태스크)
```

## Risks
| Risk | Likelihood | Mitigation |
|---|---|---|
| `InputStream` 핸들러 인자가 실제로 스트리밍인지 | Low | 서블릿 입력스트림 직결; 시연 테스트로 증명 |
| 제한 힙이 Spring 컨텍스트엔 부족 | Medium | 384m로 시작, 부족하면 상향하되 페이로드를 더 키워 비율 유지 |
| 1GB 스트림 테스트 느림 | Medium | 분리 태스크(기본 제외), 즉석 생성 스트림, 저장 후 정리 |
| raw 바디 크기 상한이 막음 | Low | octet-stream은 multipart 한도와 무관; Tomcat maxPostSize는 form 한정 |

## Acceptance
- [ ] `./gradlew test` 통과 (M1·스파이크 회귀 없음)
- [ ] `PUT /api/files`(octet-stream + X-Filename)로 스트리밍 저장 + 메타(201)
- [ ] X-Filename 누락/traversal → 400
- [ ] 저장 파일 바이트 일치 (통합)
- [ ] `./gradlew memoryTest`: 제한 힙(384m)에서 ≥512MB 스트림 OOM 없이 저장
- [ ] `docs/devlog/03-*.md` 작성
