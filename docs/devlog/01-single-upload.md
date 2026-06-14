# Devlog 01 — M1 단일 업로드 (MVP)

날짜: 2026-06-15 · 상태: 완료 · 계획: [m1-single-upload.plan.md](../plans/m1-single-upload.plan.md)

## 무엇을
- Java/Spring 서버 골격을 처음 세움 (`java/server`, Gradle + Spring Boot 4.1.0 + Java 21).
- `POST /api/files` (multipart) → 로컬 저장 → 메타(id·파일명·크기) 201 응답.
- 저장은 처음부터 `FileStorage` 인터페이스로 추상화, `LocalFileStorage` 구현 (M8 S3 대비).
- 파일명 sanitize + path traversal 거부, 빈 파일 거부.
- 테스트 7개: smoke 1 / service 3 / controller(MockMvc) 2 / integration(@SpringBootTest) 1.

## 왜
- 로드맵의 출발점(MVP). 가장 단순한 end-to-end부터 시작해 단계적으로 쌓는다(PRD 가설).
- 저장 추상화: M8(스토리지 교체)을 미리 염두 — 포트/어댑터.
- 파일명 sanitize: M3(보안) 항목이지만 traversal은 처음부터 막는 게 안전(`rules/java/security.md`).

## 하네스로 만든 흐름
`/plan-prd` → `docs/PRD.md` → `/plan` → `docs/plans/m1-single-upload.plan.md`
→ `springboot-tdd`로 RED→GREEN (서비스 RED→구현, 컨트롤러/통합) → 7 테스트 GREEN.

## 막힌 점 / 배운 것
- **Spring Boot 4.0 패키지 재편**: 테스트 슬라이스가 모듈별 패키지로 이동.
  - `@WebMvcTest`, `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure.*`
    (구 `org.springframework.boot.test.autoconfigure.web.servlet.*` 아님).
  - 스타터 이름: `spring-boot-starter-web` → `spring-boot-starter-webmvc`(+`-test`).
  - `@MockBean`(deprecated) → `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
- Gradle 미설치 → Spring Initializr(`start.spring.io/starter.zip`)로 **래퍼 포함** 스캐폴드해 해결.
- 통합 테스트 저장 경로는 `@DynamicPropertySource`로 임시 디렉터리 주입(작업 디렉터리 오염 방지).

## 다음 (M2)
스트리밍 저장 — 10GB도 메모리 폭증 없이. `MultipartFile.getInputStream()` 경로 점검,
multipart 임계값/임시파일 정책, 힙 모니터링 테스트.
