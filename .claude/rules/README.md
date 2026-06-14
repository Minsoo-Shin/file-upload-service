# .claude/rules

경로 한정(path-scoped) 규칙 모음. 각 파일의 `paths:` frontmatter에 매칭되는 파일을
Claude가 열 때만 컨텍스트에 로드된다. (전역 규칙은 `.claude/CLAUDE.md`에 둔다.)

## 출처
`java/` 규칙은 [ECC](https://github.com/affaan-m/ECC) `rules/java`를 **시드(seed)로 가져와
이 프로젝트에 맞게 정리**한 것이다 (없는 공통 규칙/스킬 참조 제거, 파일 업로드 보안 항목 추가).
이후 변경은 이 repo의 규칙으로 직접 관리한다.

## 목록
- `java/coding-style.md` — 포맷·불변성·네이밍·모던 자바·Optional·예외·스트림
- `java/patterns.md` — Repository/Service/생성자 주입/DTO/Builder/Sealed
- `java/security.md` — 시크릿·SQLi·입력검증·**파일 업로드 보안**·에러 메시지
- `java/testing.md` — JUnit5/AssertJ/Mockito/Testcontainers, TDD
