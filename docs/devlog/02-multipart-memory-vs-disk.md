# Devlog 02 — Spring 멀티파트는 언제 메모리, 언제 디스크에 쓰나 (검증)

날짜: 2026-06-21 · 성격: M2 착수 전 검증 스파이크 · 블로그 소재

## 처음의 오해
"`spring.servlet.multipart.max-file-size`(예: 100MB)보다 크면 디스크, 작으면 메모리에 둔다"
— 흔히 듣지만 **틀린 설명**이다. 두 개의 다른 파라미터를 섞은 것.

## 진짜 동작
| 파라미터 | 기본값 | 역할 |
|---|---|---|
| `max-file-size` | 1MB | **허용 상한.** 초과 → `MaxUploadSizeExceededException`으로 **요청 거부**(저장 안 함). 메모리/디스크 스위치가 아님 |
| `max-request-size` | 10MB | 요청 전체 상한 |
| **`file-size-threshold`** | **0** | **메모리↔디스크 스위치.** 이 값 이하 → 메모리(byte[]), 초과 → `location`에 임시파일 spool |
| `location` | 컨테이너 temp | spool된 임시파일 위치 |

- 스위치는 `max-file-size`가 아니라 **`file-size-threshold`**.
- 기본값이 **0**이라, 별도 설정이 없으면 **모든 업로드가 디스크로** 간다.
- spool은 Spring이 아니라 **서블릿 컨테이너(Tomcat)**가 수행한다.

## 어떻게 검증했나
- **MockMvc로는 재현 불가**: `MockMultipartFile`은 Tomcat 파싱을 거치지 않아 spool이 일어나지 않는다.
- 그래서 **실제 서버(`@SpringBootTest(webEnvironment=RANDOM_PORT)`) + 실제 HTTP 멀티파트(`RestTemplate`)**로 검증.
- 프로브 컨트롤러가 요청 처리 시점에 `location` 디렉터리의 임시파일 개수를 보고 → 메모리/디스크 판정.
- 설정: `file-size-threshold=64KB`, `max-file-size=1MB`, `max-request-size=10MB`.
- 테스트: `MultipartSpoolingExperimentTest`.

## 결과 (3케이스, 모두 그린)
| 업로드 크기 | 기대 | 결과 |
|---|---|---|
| 10KB (< threshold 64KB) | 메모리 (임시파일 0) | ✅ inMemory=true |
| 256KB (> threshold, < max) | 디스크 spool (임시파일 ≥1) | ✅ inMemory=false |
| 1.5MB (> max-file-size 1MB) | 요청 거부 | ✅ 4xx/5xx 예외 |

## 함정 메모 (구현하며 막힌 것)
- `RestClient`로 멀티파트를 보내면 `NoClassDefFoundError: org/reactivestreams/Publisher`
  (reactor 의존) → **`RestTemplate`으로 교체**해 해결.
- 프로브 컨트롤러를 `@RequestMapping`만 단 `@Bean`으로 등록했더니 매핑이 안 잡혀 **404**.
  → `@RestController` + `@Import`로 등록해야 핸들러로 인식됨.

## M2로 가는 함의
- 기본값(threshold 0)이라 멀티파트는 이미 디스크로 spool → "메모리 폭증"의 핵심 위험은 아님.
- 진짜 비용은 **임시파일 ↔ 최종 저장소 이중 쓰기**와, 요청이 `max-file-size`에 묶이는 점.
- → M2는 **원본 바디를 직접 스트리밍**해 이중 쓰기를 없애는 intake를 추가한다. ([plan](../plans/m2-streaming.plan.md))
