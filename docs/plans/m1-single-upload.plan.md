# Plan: M1 단일 업로드 (MVP)

**Source PRD**: docs/PRD.md
**Selected Milestone**: M1 — 단일 업로드 (MVP)
**Complexity**: Medium (신규 프로젝트 골격 포함)

## Summary
서버가 multipart 파일 1개를 받아 로컬 스토리지에 저장하고 메타(저장 ID·원본 파일명·크기)를
응답한다. Java/Spring 프로젝트 골격을 처음 세우고, MockMvc + 통합 테스트로 HTTP 계약을 검증한다.
저장은 처음부터 인터페이스(`FileStorage`)로 추상화해 이후(M8 S3)를 대비한다.

## Decisions to confirm (신규 프로젝트라 먼저 합의)
1. **빌드툴**: Gradle (Groovy DSL) 제안 — Maven 선호면 변경
2. **런타임**: Java 21 (LTS) + Spring Boot 3.3.x 제안
3. **레이아웃**: `java/` 루트 멀티모듈, M1은 `:server`만 생성. 클라이언트(`:client`)는 M1 미포함
4. **클라이언트 범위**: M1은 **서버 우선** — MockMvc/통합 테스트로 end-to-end HTTP 계약을 검증한다.
   실제 Spring 기반 클라이언트 전송은 M1 후속(또는 M7 진행률·병렬과 함께)으로 미룬다. ← 동의 여부 확인

## Patterns to Mirror
| Category | Source | Pattern |
|---|---|---|
| 코드 스타일 | `.claude/rules/java/coding-style.md` | record DTO, `final`, 생성자 주입 |
| 아키텍처 | `.claude/rules/java/patterns.md` | Controller→Service→Storage(인터페이스) 계층 |
| 보안 | `.claude/rules/java/security.md` | 파일명 sanitize, path traversal 방지(M1부터) |
| 테스트 | `.claude/rules/java/testing.md`, `springboot-tdd` | JUnit5+AssertJ+MockMvc, `src/test` 미러 |
| 기존 코드 | 없음 (신규 프로젝트) | — |

## Files to Change (전부 CREATE — 신규)
| File | Action | Why |
|---|---|---|
| `java/settings.gradle` | CREATE | 멀티모듈 루트 (`:server`) |
| `java/server/build.gradle` | CREATE | Spring Boot web 의존성 |
| `java/server/src/main/.../UploadServerApplication.java` | CREATE | 부트 진입점 |
| `.../web/FileUploadController.java` | CREATE | `POST /api/files` multipart → 201 + 메타 |
| `.../web/dto/UploadResponse.java` | CREATE | 응답 record(id, filename, size) |
| `.../service/FileStorageService.java` | CREATE | 저장 오케스트레이션 + 파일명 sanitize |
| `.../storage/FileStorage.java` | CREATE | 저장 추상화 인터페이스(포트) |
| `.../storage/LocalFileStorage.java` | CREATE | 로컬 FS 구현 |
| `.../resources/application.yaml` | CREATE | multipart 한도, 저장 경로 |
| `.../test/.../service/FileStorageServiceTest.java` | CREATE | 단위 테스트(RED 먼저) |
| `.../test/.../web/FileUploadControllerTest.java` | CREATE | MockMvc 테스트 |
| `.../test/.../UploadIntegrationTest.java` | CREATE | `@SpringBootTest` end-to-end |

## Tasks (TDD: RED → GREEN → 리팩터)
### Task 1: 골격 + 부팅 스모크
- **Action**: Gradle 멀티모듈 + `:server`(Spring Boot web), `contextLoads` 테스트
- **Validate**: `cd java && ./gradlew :server:test`

### Task 2: 저장 서비스 (RED)
- **Action**: `FileStorageServiceTest` — 저장 시 sanitize된 이름·크기 반환, `../` 등 위험 파일명 거부
- **Mirror**: `java/security.md`(sanitize), `java/testing.md`(AssertJ)

### Task 3: 저장 구현 (GREEN)
- **Action**: `FileStorage` 인터페이스 + `LocalFileStorage` + `FileStorageService`로 Task 2 통과
- **Mirror**: `java/patterns.md`(Repository/포트 추상화)

### Task 4: 업로드 API (RED)
- **Action**: `FileUploadControllerTest`(MockMvc) — multipart POST → 201 + 메타 JSON
- **Mirror**: `springboot-tdd`(`@WebMvcTest`, `jsonPath`)

### Task 5: 컨트롤러 (GREEN)
- **Action**: `FileUploadController` 구현으로 Task 4 통과
- **Mirror**: `springboot-patterns`(REST 구조, `@Valid`, ResponseEntity)

### Task 6: 통합 검증
- **Action**: `@SpringBootTest`로 실제 파일 업로드→저장→응답 end-to-end 확인

## Validation
```bash
cd java && ./gradlew :server:test
```

## Risks
| Risk | Likelihood | Mitigation |
|---|---|---|
| 신규 스캐폴드에 시간 소요 | Medium | Spring Initializr 구조 따르기, 의존성 최소(web) |
| M1에서 전체 메모리 적재(스트리밍 전) | Low (M1은 소형 파일) | 대용량/스트리밍은 M2로, M1은 작은 파일로 검증 |
| path traversal | Medium | M1부터 파일명 sanitize 테스트 포함 |

## Acceptance
- [ ] `./gradlew :server:test` 통과
- [ ] `POST /api/files` 가 파일 저장 + 메타(201) 응답
- [ ] 위험 파일명 거부 테스트 통과
- [ ] 패턴을 재발명하지 않고 `.claude/rules/java/*` 따름
- [ ] `docs/devlog/01-*.md` 작성
