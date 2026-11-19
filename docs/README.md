# 프로젝트 자료 안내

개발 클래스 외에 문서·설정·스크립트가 어떤 역할을 하는지 정리한 안내다. 상세 설명과 명령은 연결된 파일에서 관리한다.

## 목적별로 먼저 볼 문서

| 하고 싶은 일 | 먼저 볼 자료 |
| --- | --- |
| 프로젝트 소개 확인 | [루트 README](../README.md) |
| 전체 서비스를 로컬에서 실행 | [Docker 개발 안내](docker-development.md) |
| Java·Node·환경변수 준비 또는 개별 실행 | [개발 환경 안내](development.md) |
| 작업 범위·완료 기준·검증 방법 결정 | [검증 절차](verification-workflow.md) |
| 실제 확인한 결과와 남은 한계 확인 | [검증 기록](verification.md) |
| GitHub 자동 검사·Codex 검증 버튼 이해 | [CI와 Actions 안내](ci-and-actions.md) |
| 다음 리팩토링 대상 선택 | [개선 과제 목록](issues/README.md) |

`verification-workflow.md`는 앞으로 따를 절차이고, `verification.md`는 실제 수행한 검증의 기록이다. 기록에 적힌 결과는 해당 시점과 환경에 대한 것이며, 이후 변경까지 자동으로 보장하지 않는다.

## 작업 규칙·양식·계획

| 자료 | 역할·참고 시점 |
| --- | --- |
| [루트 AGENTS.md](../AGENTS.md) | AI가 프로젝트 전반에서 지킬 작업·보안·검증 규칙 |
| `CLAUDE.md`(루트·Backend·Frontend) | Claude Code가 같은 위치의 AGENTS.md를 불러오는 연결 파일. 내용은 AGENTS.md에서만 수정 |
| [Backend/AGENTS.md](../Backend/AGENTS.md) | 백엔드 구조·인증·예외 처리·테스트 규칙. 백엔드 작업 시 함께 참고 |
| [Frontend/AGENTS.md](../Frontend/AGENTS.md) | 프론트 구조·API 연동·화면 검증 규칙. 프론트 작업 시 함께 참고 |
| [작업 검증 양식](templates/task-verification.md) | 주요 작업의 범위·제외 범위·완료 기준·검사 결과를 적을 때 사용 |
| [Docker 구성 계획](superpowers/plans/2026-11-03-docker-development.md) | Docker 개발 환경을 구성한 계획과 실행 기록 |
| [검증 체계 구성 계획](superpowers/plans/2026-11-03-verification-workflow.md) | 완료 기준·스크립트·브라우저 검사·Skill을 구성한 계획과 결과 |
| [docs/issues/](issues/README.md) | 발견한 개선 과제의 배경·완료 기준·코드 근거와 GitHub 이슈 링크. 문서가 있다는 사실이 수정 완료를 의미하지는 않음 |

`superpowers/plans/`는 작업 계획을 보관하는 폴더다. 실행 방법이 필요하면 계획 기록보다 위의 개발 안내를 먼저 참고한다.

## AI 도구 설정

| 자료 | 역할 |
| --- | --- |
| [Codex 검증 Skill](../.agents/skills/verify-project/SKILL.md) | Codex에서 프로젝트 검증 절차를 적용하는 진입점. `$verify-project`로 요청 |
| [Claude Code 검증 Skill](../.claude/skills/verify-project/SKILL.md) | Claude Code에서 같은 절차를 적용하는 진입점. `/verify-project`로 요청 |
| [Codex 로컬 환경 설정](../.codex/environments/environment.toml) | Codex 앱의 검증 실행 명령·버튼 설정 |

AGENTS.md는 공통 작업 규칙, Skill은 검증 작업에 사용하는 절차다. 두 Skill은 [공통 검증 문서](verification-workflow.md)를 참조하고 실제 검사는 아래 스크립트가 수행한다. Skill의 자동 선택·앱 버튼 표시 여부와 검사 실행 성공은 별도로 확인한다.

## 실행·환경 설정

| 자료 | 역할 |
| --- | --- |
| [루트 compose.yaml](../compose.yaml) | 프론트·백엔드·MySQL·Redis를 함께 실행하는 기본 로컬 Docker 구성 |
| [Backend/compose.yml](../Backend/compose.yml) | MySQL·Redis만 실행하는 이전 구성. 루트 Compose와 포트·용도를 구별 |
| [백엔드 Dockerfile.dev](../Backend/Dockerfile.dev)·[프론트 Dockerfile.dev](../Frontend/Dockerfile.dev) | 각 개발용 Docker 이미지의 생성 절차 |
| [백엔드 .dockerignore](../Backend/.dockerignore)·[프론트 .dockerignore](../Frontend/.dockerignore) | Docker 빌드에 전달할 파일의 범위 지정 |
| [루트 .env.example](../.env.example) | Docker용 환경변수 작성 예시. 실제 로컬 값은 루트 `.env`에 보관 |
| [Backend/nimn.env.example](../Backend/nimn.env.example) | 백엔드 직접 실행용 환경변수 예시. 실제 값은 `Backend/nimn.env`에 보관 |
| [Frontend/.env.example](../Frontend/.env.example) | 프론트 개인 환경 설정 작성 예시 |
| [개발용](../Frontend/.env.development)·[배포용](../Frontend/.env.production)·[테스트용](../Frontend/.env.test) 프론트 환경 파일 | 실행 환경별 공개 주소 설정. 자세한 적용 순서는 개발 안내 참고 |
| [Frontend/.nvmrc](../Frontend/.nvmrc) | 사용할 Node 메이저 버전 지정. 버전 자동 설치·전환 기능은 아님 |
| [package.json](../Frontend/package.json)·[package-lock.json](../Frontend/package-lock.json) | 프론트 실행 명령·의존성 목록과 재현 가능한 설치 버전 기록 |
| [build.gradle](../Backend/build.gradle)·[Gradle Wrapper 설정](../Backend/gradle/wrapper/gradle-wrapper.properties) | 백엔드 의존성·빌드 절차·Gradle 버전 설정. `gradlew`·`gradlew.bat`는 실행 도구 |
| [Frontend/netlify.toml](../Frontend/netlify.toml) | Netlify 배포 설정 |

`.env.example`은 공유할 작성 예시이며 실제 비밀값을 넣는 파일이 아니다. 프론트 환경변수는 브라우저 번들에 포함되므로 비밀값 저장 용도로 사용하지 않는다.

## 검사 도구·GitHub 자동화

| 자료 | 역할 |
| --- | --- |
| [scripts/verify.ps1](../scripts/verify.ps1) | 백엔드·프론트·브라우저·실행 상태 검사 명령의 진입점 |
| [scripts/verify-runtime.ps1](../scripts/verify-runtime.ps1) | 실행 중인 Docker 서비스 health·HTTP 응답·Redis 연결 확인 |
| [실행 상태 검사 도구의 테스트](../scripts/tests/verify-runtime.test.ps1) | 가짜 Docker 응답으로 서비스 누락·비정상 상태의 실패 처리와 경로 복원 확인 |
| [scripts/init-docker-env.ps1](../scripts/init-docker-env.ps1) | Docker용 로컬 `.env` 초기 생성. 기존 파일은 보존 |
| [Playwright 설정](../Frontend/playwright.config.js)·[e2e/](../Frontend/e2e/) | 검사용 브라우저·서버·보고서 설정과 실제 화면 검사 시나리오. 현재 API는 대체하며 실제 인증 연동 검사는 아님 |
| [GitHub CI](../.github/workflows/ci.yml) | PR 등에서 테스트·빌드·브라우저 검사를 자동 실행 |
| [백엔드 배포](../.github/workflows/backend-deploy.yml)·[프론트 배포](../.github/workflows/frontend-deploy.yml) | 기존 배포 자동화. 운영 중단으로 develop push 자동 실행을 끄고 수동 실행만 남김. CI와 독립적이므로 CI 실패가 배포 차단을 의미하지는 않음 |
| [이슈 양식](../.github/ISSUE_TEMPLATE/)·[PR 양식](../.github/PULL_REQUEST_TEMPLATE.md) | GitHub에서 문제·변경 내용·검증 결과를 일정한 형식으로 작성 |

검사 명령·통과 기준·보장하지 않는 범위는 [검증 절차](verification-workflow.md), CI 실행 조건은 [CI와 Actions 안내](ci-and-actions.md)에서 확인한다.

## 로컬 관리·생성 자료

| 자료 | 역할 |
| --- | --- |
| [루트 .gitignore](../.gitignore) 및 각 영역의 `.gitignore` | Git에서 제외할 파일 지정. 파일 접근 권한을 제한하는 장치는 아님 |
| `.local-tools/` | 이 PC의 프로젝트용 런타임·캐시·임시 자료. Git 공유 대상이 아니며 다른 PC에서 자동으로 준비되지 않음 |
| `.idea/` | IDE의 프로젝트 설정 |
| `.git/` | Git의 커밋 이력·브랜치 등 내부 관리 정보 |
| `Frontend/node_modules/`, 각 영역의 `build/` | 설치된 패키지 또는 빌드 산출물 |
| `Frontend/playwright-report/`, `Frontend/test-results/` | 브라우저 검사 보고서·실패 화면·trace 등 생성 결과. Git 제외 대상 |

문서·설정의 역할이나 경로가 바뀌면 이 안내의 해당 행과 링크도 함께 갱신한다.
