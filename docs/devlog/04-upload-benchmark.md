# Devlog 04 — 업로드 경로별 메모리·Latency 벤치마크

날짜: 2026-06-21 · 성격: M2 후속 측정 스파이크 · 블로그 소재
실행: `cd java/server && ./gradlew benchmarkTest` (`-Xmx2g`, `@Tag("benchmark")`)

## 질문
"스트리밍이 정말 메모리를 덜 쓰나? 얼마나? 그리고 latency는?"

## 방법
동일 **256MB** 페이로드를 3경로로 업로드하고 **서버 peak 힙 증가분**(백그라운드 샘플러)과
**end-to-end latency**(클라 wall-clock)를 측정. 클라이언트는 통짜 `byte[]` 없이 **생성형 스트림**으로
보내 클라 메모리를 상수로 고정(차이는 서버 쪽). 코드: `BenchmarkSupport`, `UploadBenchmarkTest`,
`MultipartInMemoryBenchmarkTest`.

## 결과
| 경로 | latency | peak 힙 |
|---|---|---|
| octet-stream PUT (스트리밍) | **263 ms** | **48 MB** |
| 멀티파트 (threshold=0, 디스크 spool) | 728 ms | 54 MB |
| 멀티파트 (메모리 보관) | 563 ms | **1037 MB** |

## 해석
- **메모리**: 스트리밍·디스크 spool은 **상수(~50MB, 페이로드와 무관)**. 메모리 보관 멀티파트는
  256MB 업로드에 **~1GB(≈4×)** — 파싱 중 byte[] 성장·복사 때문. 대용량에선 즉시 OOM.
- **Latency**: 스트리밍이 최저(단일 패스). 디스크 spool이 최고 — **임시파일↔저장소 이중 쓰기**(디스크 I/O 2회).
  메모리 보관은 중간(디스크는 안 쓰나 대용량 alloc/GC 비용).
- **결론**: 대용량은 스트리밍이 **메모리·속도 모두 우위**. 기본 멀티파트(디스크 spool)는 메모리는
  안전하지만 이중 쓰기로 느리다. 메모리 보관 멀티파트는 대용량에 금물.

## Caveat
- 루프백·in-process 측정이라 **절대 latency는 실제 네트워크가 아님** — 상대 비교가 핵심.
- peak 힙은 GC 타이밍에 따라 근사치(샘플링). 경향은 뚜렷.

## 함의 (왜 스트리밍 intake인가 — 정리)
스트리밍을 가능케 하는 건 메서드/컨텐츠타입이 아니라 **멀티파트 파싱 레이어를 안 거치는 것**.
octet-stream raw 바디는 파싱이 없어 `request.getInputStream()`을 핸들러에 직결 → `Files.copy`로 흘림.
멀티파트는 핸들러 전에 파싱·spool(또는 메모리 적재)가 일어나 위 비용이 발생.
