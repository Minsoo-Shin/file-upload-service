# Code Review Standards

리뷰 기준(무엇이 합격인가)을 정의한다. *실제 리뷰 실행*은 빌트인 `/code-review`,
보안은 `/security-review`를 쓴다. (이 규칙은 그 엔진과 Claude가 참고할 *체크리스트*다.)

## When to Review

**리뷰 트리거:**
- 코드를 쓰거나 수정한 직후
- 커밋(특히 공유 브랜치) 전
- 보안 민감 코드 변경 시 (인증, 사용자 입력, 파일 처리, 결제 등)
- 구조적 변경 시

**리뷰 전 준비:**
- 자동 검사(테스트/빌드) 통과
- 머지 충돌 해소, 대상 브랜치와 최신화

## Review Checklist

완료로 표시하기 전:

- [ ] 읽기 쉽고 이름이 명확하다
- [ ] 함수가 집중돼 있다 (<50 lines)
- [ ] 파일이 응집적이다 (<800 lines)
- [ ] 깊은 중첩 없음 (>4 levels → early return)
- [ ] 에러를 명시적으로 처리한다 (조용히 삼키지 않음)
- [ ] 하드코딩된 시크릿/크리덴셜 없음
- [ ] 디버그 출력·임시 로그 없음 (`System.out.println`, 임시 `printStackTrace` 등)
- [ ] 새 기능에 테스트가 있다
- [ ] 커버리지 80% 이상

## Security Review Triggers

다음에 해당하면 **멈추고 `/security-review` 실행** (그리고 `.claude/rules/java/security.md` 확인):

- 인증/인가 코드
- 사용자 입력 처리 (특히 **업로드 파일명·경로** — path traversal)
- DB 쿼리
- 파일시스템 작업 (저장 경로, 스트림)
- 외부 API 호출
- 암호화 연산

## Review Severity Levels

| Level | 의미 | 조치 |
|---|---|---|
| CRITICAL | 보안 취약점·데이터 손실 위험 | **BLOCK** — 머지 전 반드시 수정 |
| HIGH | 버그·중대한 품질 이슈 | **WARN** — 머지 전 수정 권장 |
| MEDIUM | 유지보수 우려 | **INFO** — 가능하면 수정 |
| LOW | 스타일·사소한 제안 | **NOTE** — 선택 |

## Review Workflow

```
1. git diff로 변경 파악
2. 보안 체크리스트 먼저
3. 품질 체크리스트
4. 관련 테스트 실행
5. 커버리지 ≥ 80% 확인
6. 빌트인 /code-review (필요시 /security-review)
```

## Common Issues to Catch

### Security
- 하드코딩 크리덴셜(API 키·비밀번호·토큰)
- SQL 인젝션 (쿼리 문자열 연결)
- path traversal (sanitize 안 한 파일 경로) — **이 프로젝트 핵심**
- 인증 우회, 권한 검사 누락
- 에러 메시지로 내부 정보 노출

### Code Quality
- 큰 함수(>50 lines) → 분리
- 큰 파일(>800 lines) → 모듈 추출
- 깊은 중첩(>4) → early return
- 에러 처리 누락 → 명시적 처리
- 불변성 위반 → 가능하면 immutable
- 테스트 누락 → 커버리지 추가

### Performance
- N+1 쿼리 → JOIN/배칭 (DB 도입 단계)
- 페이지네이션 누락 → 제한 추가
- 대용량을 메모리에 통째로 적재 → **스트리밍** (이 프로젝트 핵심)
- 비싼 연산 반복 → 캐싱

## Approval Criteria

- **Approve**: CRITICAL/HIGH 없음
- **Warning**: HIGH만 있음 (주의해서 머지)
- **Block**: CRITICAL 있음

## Integration

- `.claude/rules/java/testing.md` — 커버리지/테스트 기준
- `.claude/rules/java/security.md` — 보안 체크리스트(파일 업로드 포함)
- `.claude/CLAUDE.md` — 커밋 규칙
- 실행 엔진: 빌트인 `/code-review`, `/security-review`

> 출처: ECC `rules/common/code-review.md` 시드 후 정리 (에이전트 참조 제거 → 빌트인으로 교체). ADR-0001.
