# CLAUDE.md — file-upload-service

이 프로젝트는 **서드파티 플러그인(ECC 등)을 배제하고, Claude Code 기본 스킬 + 이 repo의
`.claude/skills` + 프로젝트 자체 규칙만으로** 진행한다. 규칙은 이 파일에서 직접 다듬어 나간다.

## 프로젝트 목적
이 프로젝트의 **주목적은 AI 에이전트 하네스 구축**이다 — Claude Code 위에
프로젝트 전용 스킬·규칙·워크플로를 직접 설계하고 다듬는다.
구축한 하네스를 **파일 업로드 서비스**(Java/Spring 서버 + Picocli CLI)를 만드는
실전 도메인에 적용해 검증한다. (상세 PRD·로드맵은 추후 `docs/PRD.md`에 작성.)

## 작업 규칙
- 기능 단계는 테스트 우선(TDD)으로 구현한다.
- 단계 완료 시 `docs/devlog/NN-*.md`에 무엇을/왜/막힌 점을 남긴다.
- 되돌리기 어려운 결정은 `docs/adr/`에 기록한다.

## 커밋 규칙
- Conventional Commits 형식: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`
- 커밋·푸시·공개 리포 생성 등 외부/되돌리기 어려운 동작은 진행 전 사용자에게 확인한다.

## 하네스 (이 repo의 스킬)
- 반복/절차가 긴 작업만 `.claude/skills/<name>/SKILL.md` 스킬로 만든다.
  (단순히 Claude가 이미 잘 하는 동작은 스킬로 감싸지 않고, 컨벤션은 이 규칙에 둔다.)
- 현재 스킬: `create-git-repo`, `springboot-tdd`, `springboot-patterns`.
- 현재 커맨드(`.claude/commands/`): `plan-prd` (PRD → `docs/PRD.md`), `plan` (마일스톤 구현 계획 → `docs/plans/`).
- 워크플로: `/plan-prd` → `/plan` → `springboot-tdd` → 빌트인 `/code-review` → 커밋.
- 규칙(`.claude/rules/`): `java/*` (coding-style·patterns·security·testing).
- ECC 등 외부 자산은 "시드"로만 가져와 정리해 쓰며 플러그인은 쓰지 않는다 (`docs/adr/0001`).

## 코딩 컨벤션
<!-- 언어/스타일 규칙을 단계 진행하며 직접 정의 -->
- (TBD) Java 포맷·네이밍은 M1 착수 시 확정한다.

## 금지 사항
<!-- 하지 말아야 할 것 -->
- 글로벌/유저 레벨 설정에 의존하는 규칙 추가 금지 (프로젝트 자체 규칙만).
