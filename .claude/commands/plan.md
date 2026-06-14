---
description: Restate requirements, assess risks, and create a step-by-step implementation plan. WAIT for user CONFIRM before touching any code.
argument-hint: "[feature description | docs/PRD.md | empty = next pending milestone]"
origin: "ECC /plan (seeded, repo-local; PRD→docs/PRD.md, plans→docs/plans/, handoff→springboot-tdd; no agents)"
---

# Plan Command

코드를 쓰기 전에 **구현 계획**을 먼저 만든다. 자유 형식 요구사항 또는 `docs/PRD.md`를 입력으로 받는다.

**인라인으로 실행한다. Task 도구/서브에이전트를 부르지 않는다** (이 repo는 에이전트를 두지 않는다 — ADR-0001).

## What This Command Does
1. **요구사항 재진술** — 무엇을 만들지 명확히
2. **리스크 식별** — 잠재 이슈/블로커 표면화
3. **단계 계획** — 구현을 단계로 분해
4. **확인 대기** — 진행 전 **사용자 승인 필수**

## When to Use
- 새 마일스톤/기능 시작, 구조 변경, 복잡한 리팩터, 여러 파일 영향, 요구 모호할 때

## Input Modes
| Input | Mode | Behavior |
|---|---|---|
| `docs/PRD.md` (또는 비움) | PRD 모드 | PRD의 **Delivery Milestones**에서 **다음 pending 마일스톤**을 골라 `docs/plans/{name}.plan.md` 작성 |
| 기타 마크다운 경로 | 참고 모드 | 파일을 컨텍스트로 읽고 인라인 계획 |
| 자유 텍스트 | 대화 모드 | 인라인 계획 |
| (PRD 모드에서) | — | 고른 마일스톤의 Status를 `pending → in-progress`로 갱신 |

## Pattern Grounding (계획 전 코드베이스 관례 조사)
계획 전에 따라야 할 기존 관례를 찾아 파일 참조와 함께 기록한다. 비어 있으면 "없음"이라 명시 — **지어내지 않는다.**

| Category | What to capture |
|---|---|
| Naming | 영향 영역의 파일/함수/타입 네이밍 |
| Error handling | 실패를 어떻게 던지/반환/로깅하나 |
| Logging | 레벨·포맷·무엇을 남기나 |
| Data access | repository/service/파일시스템 패턴 |
| Tests | 테스트 위치·프레임워크·픽스처·단언 스타일 (`.claude/rules/java/testing.md`, `springboot-tdd` 참고) |

> 이 프로젝트 규칙도 함께 반영: `.claude/rules/java/*` (coding-style·patterns·security·testing).

## PRD Artifact Output
`docs/PRD.md`로 호출되면 `docs/plans/{kebab-case-name}.plan.md`에 다음 구조로 작성한다 (`docs/plans/` 없으면 생성):

````markdown
# Plan: {Milestone Name}

**Source PRD**: docs/PRD.md
**Selected Milestone**: {M번호 + 이름}
**Complexity**: {Small | Medium | Large}

## Summary
{2-3 sentences}

## Patterns to Mirror
| Category | Source | Pattern |
|---|---|---|
| Naming | `path:line` | {short} |
| Tests | `path:line` | {short} |

## Files to Change
| File | Action | Why |
|---|---|---|
| `path` | CREATE / UPDATE / DELETE | {reason} |

## Tasks
### Task 1: {name}
- **Action**: {무엇을}
- **Mirror**: {따를 패턴}
- **Validate**: {정확성을 증명할 명령}

## Validation
```bash
# Gradle: ./gradlew test    또는    Maven: ./mvnw test
```

## Risks
| Risk | Likelihood | Mitigation |
|---|---|---|

## Acceptance
- [ ] 모든 Task 완료
- [ ] 검증(테스트) 통과
- [ ] 패턴을 재발명하지 않고 따름
- [ ] (단계 완료 시) `docs/devlog/NN-*.md` 작성
````

작성 후 경로를 보고하고, 코드 작성 전 **확인을 기다린다.**

## CRITICAL
"yes"/"proceed"/"진행" 같은 명시적 승인 전까지 **어떤 코드도 쓰지 않는다.**
수정 요청은 "modify: …", "different approach: …", "skip task 2 …" 형태로.

## Integration (이 repo)
계획 후:
- **`springboot-tdd` 스킬**로 테스트 우선 구현 (RED→GREEN→리팩터)
- **`springboot-patterns` 스킬**로 아키텍처 패턴 참고
- 빌드/타입 에러는 최소 변경으로 수정
- **빌트인 `/code-review`, `/security-review`**로 리뷰
- **`create-git-repo` 스킬 / CLAUDE.md 커밋 규칙**으로 단계별 커밋

> 요구사항이 먼저 필요하면 `/plan-prd`로 `docs/PRD.md`를 만든다.
