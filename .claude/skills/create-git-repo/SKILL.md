---
name: create-git-repo
description: Initialize a git repository for the current project and optionally create + push to a GitHub remote with gh. Use when the user wants to set up version control, start a fresh repo, reset an existing .git, or publish the project to GitHub (public or private).
---

# create-git-repo

현재 프로젝트의 git 저장소를 초기화하고, 필요하면 GitHub 원격 저장소를 만들어 푸시한다.

## 언제 쓰나
- 새 프로젝트에 git을 처음 셋업할 때
- 기존 `.git`을 지우고 깨끗이 다시 시작할 때
- 로컬 repo를 GitHub에 올릴 때 (public / private)

## 원칙
- **되돌리기 어렵거나 외부에 공개되는 동작은 진행 전 사용자 승인을 받는다.** 특히:
  - 기존 `.git` 삭제 (커밋/리모트가 있으면 손실 위험 → 먼저 `git log`·`git remote`로 확인하고 알린다)
  - **공개(public) 리포 생성** — 이름과 공개범위를 사용자가 명시적으로 말하기 전에는 만들지 않는다
    (auto 모드 분류기도 이를 차단하므로, 추측으로 진행하지 말 것)
- 커밋 메시지는 `.claude/CLAUDE.md`의 커밋 규칙(Conventional Commits)을 따른다.

## 절차

### 1. 현재 상태 확인
```bash
git rev-parse --show-toplevel 2>/dev/null   # 이미 repo인가
git log --oneline -5 2>&1                    # 커밋 유무
git remote -v 2>&1                           # 리모트 유무
gh auth status 2>&1 | head                   # GitHub 인증 계정
```
- 이미 커밋/리모트가 있는 repo면 **재초기화 전에 사용자에게 알리고 확인**한다.

### 2. (선택) 기존 git 리셋
커밋·리모트가 없는 빈 repo이거나 사용자가 명시적으로 원할 때만:
```bash
rm -rf .git && git init -b main
```

### 3. .gitignore 확인
없으면 스택에 맞게 만든다. (Java/Gradle·CLI·업로드 스토리지 등)

### 4. 첫 커밋
```bash
git add -A
git status --short
git commit -m "<type>: <요약>"   # Conventional Commits
```

### 5. GitHub 원격 (사용자 명시 승인 필요)
진행 전 **반드시** 확인할 것: ① 리포 이름(예: `<owner>/<repo>`) ② 공개범위(public/private).
승인받은 뒤에만:
```bash
gh repo create <owner>/<repo> --<public|private> --source=. --remote=origin --push
git remote -v
gh repo view --json url --jq .url
```
- gh 인증이 없으면 사용자에게 터미널에서 `! gh auth login` 실행을 안내한다.
- 사용자가 직접 만들 경우 리모트 URL만 받아 `git remote add origin <url>` 후 `git push -u origin main`.

## 완료 후
- 원격 URL을 사용자에게 보여준다.
- 단계 기록이 필요한 프로젝트면 `docs/devlog/`에 한 줄 남길지 물어본다.
