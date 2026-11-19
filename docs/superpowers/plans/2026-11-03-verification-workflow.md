# 검증 작업 흐름 구현 계획

Goal: 구현 전에 완료 기준을 정하고 코드·브라우저·실행 상태를 실제 검사할 수 있게 한다.

Architecture: 기존 검증 스크립트와 CI를 확장하고 공통 문서를 Codex·Claude Code Skill이 참조한다.

Tech Stack: PowerShell, Playwright/Chromium, 기존 JUnit·Jest·ESLint, GitHub Actions.

Spec: `docs/verification-workflow.md`. 로그인·재발급·로그아웃 구현과 운영 배포는 제외한다.

1. Docker 구성 검증 후 별도 커밋하고 `chore/verification-workflow`를 만든다.
2. 완료 기준 양식·공통 절차·두 도구의 Skill을 작성한다.
3. Playwright 비로그인 화면 검사와 Docker 상태 검사 명령을 추가한다.
4. 기존 전체 검증과 CI에 브라우저 검사를 연결한다.
5. 실제 코드·브라우저·실행 검사를 수행하고 실패 경로·Skill 판단을 확인한다.
6. 독립 리뷰 후 지적을 반영하고 남은 범위를 보고한다.

구현 결과: 1~6을 수행했다. 기존 서버 재사용 문제를 독립 리뷰에서 발견해 전용 3001번 서버로 수정했다. 명령별 결과와 한계는 `docs/verification.md`에 기록한다. 원격 CI와 두 도구의 자동 Skill 선택은 미확인이다.
