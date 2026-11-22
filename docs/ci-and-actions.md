# PR 검증과 Codex Actions

## 1. GitHub Actions CI

`.github/workflows/ci.yml`의 `Project CI`는 모든 PR, `develop` push, 수동 실행에서 동작한다. 파일 경로 필터로 검증을 건너뛰지 않으므로 문서만 변경한 PR에서도 두 작업이 실행된다.

| 작업 이름 | 환경 | 검증 |
| --- | --- | --- |
| Backend verification | Ubuntu · JDK 17 | `bash ./gradlew test bootJar --no-daemon --console=plain` |
| Frontend verification | Ubuntu · `.nvmrc`의 Node 24 | `npm ci` → `npm run lint` → `npm run test:ci` → `npm run build` |

- 테스트에는 운영 비밀값이나 Docker 서비스를 주입하지 않는다. 백엔드는 앞 단계에서 만든 H2·Mockito 구성을 사용한다.
- 토큰 권한은 `contents: read`이며, checkout 자격증명은 작업 디렉터리에 남기지 않는다.
- 같은 PR·브랜치의 새 실행이 시작되면 이전 CI 실행을 취소한다. 작업 제한 시간은 각각 20분이다.
- 백엔드 테스트 보고서·XML은 실패 시에도 가능한 범위에서 `backend-test-reports` 아티팩트로 올리고 7일간 보관한다.
- 새 워크플로에는 배포·SSH·실제 메일·결제 호출 단계가 없다.

### 프론트 lint 기준

`npm run lint`는 전체 `src/`를 검사한다. **오류는 CI를 실패시키고, 기존 경고는 로그에 표시한다.** 현재 기준은 오류 0개·경고 32개다. 경고 수를 고정하거나 새 경고를 자동 차단하는 구성은 아니다.

이후 CRA 빌드에서는 `CI=true`를 유지하고 `DISABLE_ESLINT_PLUGIN=true`만 설정한다. 앞 단계에서 이미 ESLint를 실행했으므로 중복 검사를 피하면서 기존 lint 경고가 CRA에 의해 빌드 오류로 재분류되는 것을 방지한다. lint 실패를 무시하거나 테스트를 건너뛰지는 않는다. lint 이외의 빌드 오류·경고는 CRA의 CI 처리 기준을 따른다.

### 원격 적용과 병합 제한

파일은 로컬에 작성했으며 아직 GitHub로 push하거나 원격 CI를 실행하지 않았다. PR 브랜치에 커밋·push되어야 이 워크플로가 실행된다.

워크플로 추가만으로 병합이 자동 차단되지는 않는다. 저장소의 브랜치 보호 또는 Ruleset에서 `Backend verification`, `Frontend verification`을 필수 상태 검사로 지정하는 작업은 별도다.

운영 서버를 사용하지 않으므로 `backend-deploy.yml`, `frontend-deploy.yml`의 `develop` push 트리거를 주석 처리했다. 두 워크플로는 GitHub Actions 화면의 수동 실행(`workflow_dispatch`)으로만 시작된다. 운영을 재개하면 각 파일의 `# push:` 블록 주석을 해제하면 된다. 자동 배포를 켰을 때의 동작은 다음과 같다.

기존 `backend-deploy.yml`, `frontend-deploy.yml`은 독립적으로 동작한다. 이번 CI는 배포 작업의 선행 조건으로 연결하지 않았다. 따라서 `develop` push에서는 검증·배포가 각각 시작될 수 있으며, CI 실패 시 배포도 자동 중단된다고 해석하면 안 된다.

## 2. Codex 실행 Actions

설정 파일은 `.codex/environments/environment.toml`이다. 설치된 Codex 앱의 로컬 환경 스키마를 확인해 작성했으며, Windows용 검증 Action 3개를 등록했다.

| Action | 실행 명령 |
| --- | --- |
| Backend 검증 | `powershell -NoProfile -File scripts/verify.ps1 -Target Backend` |
| Frontend 검증 | `powershell -NoProfile -File scripts/verify.ps1 -Target Frontend` |
| 전체 검증 | `powershell -NoProfile -File scripts/verify.ps1 -Target All` |
| 브라우저 검증 | `powershell -NoProfile -File scripts/verify.ps1 -Target Browser` |
| Docker 실행 상태 검증 | `powershell -NoProfile -File scripts/verify.ps1 -Target Runtime` |

macOS에서는 같은 이름의 Action 5개가 `platform = "darwin"`으로 등록돼 있으며 `bash scripts/verify.sh backend|frontend|all|browser|runtime`을 실행한다. `verify.sh`는 `verify.ps1`과 같은 대상·순서로 검사한다. 맥에 JDK 17이 설치돼 있으면 기본 JDK가 다른 버전이어도 `/usr/libexec/java_home -v 17`로 선택한다. Codex 앱에서 맥 버튼이 표시되는지는 확인하지 않았다.

앱에서 이 저장소의 로컬 환경을 선택해 사용한다. Actions는 앱의 통합 터미널에서 실행된다. 버튼이 보이지 않으면 저장소를 다시 열거나 설정의 로컬 환경에서 `우리동네 영양사`를 선택한다. 프로젝트 파일은 등록·검증했지만 앱 UI에서 버튼 표시나 클릭 실행을 확인하지는 않았다. [공식 로컬 환경 안내](https://learn.chatgpt.com/docs/environments/local-environment)

자동 setup 스크립트는 비워 두었다. 새 작업 환경에서 자동으로 의존성을 설치하거나 로컬 설정·비밀값을 복사하지 않는다. [개발 안내](development.md)에 따라 JDK 17·Node 24·npm 11과 프론트 의존성을 준비한다.

### 검증 스크립트 동작

- 현재 작업 경로 대신 스크립트 위치에서 저장소 루트를 찾는다.
- `.local-tools/`의 JDK 17·Node 24가 있으면 검증 프로세스에서 사용하고, 없으면 준비된 시스템 런타임을 사용한다. 버전이 맞지 않으면 안내와 함께 실패한다.
- 프론트 의존성이 없으면 `npm ci`가 필요하다고 알린다. 검증 버튼이 설치를 임의로 수행하지 않는다.
- Backend는 테스트·JAR 빌드, Frontend는 CI와 같은 lint·테스트·빌드를 실행한다.
- All은 Backend·Frontend·Browser를 실행한다. Runtime은 별도 명령이며 실행 중인 Docker 서비스를 검사한다.
- PR CI는 Chromium을 설치하고 대체 API 기반 브라우저 검사도 수행한다. 실패 화면·trace와 HTML 보고서를 7일간 보관한다. 현재 배포 워크플로와 브랜치 보호 규칙을 이 작업에서 변경하지 않으므로 이 CI가 배포를 자동 차단한다고 가정하지 않는다.
- 실패한 명령 뒤의 검증은 실행하지 않는다. 원래 작업 경로와 변경한 프로세스 환경변수는 성공·실패 모두에서 복원한다.
- 운영 서버 실행이나 배포는 하지 않는다. H2·Mock 기반 검증 범위는 [검증 기록](verification.md)을 참고한다.

## 3. 이 단계에서 확인한 결과

- `actionlint 1.7.12`로 새 워크플로의 문법·Actions 규칙 검사 통과. 로컬 검사에서 shellcheck·pyflakes는 실행하지 않았다.
- 앱 스키마에 맞는 TOML 파싱·필드 검사 통과.
- Actions가 호출하는 `-Target All` 명령 통과: 백엔드 검증·JAR, 프론트 lint 오류 0개·테스트 3개·빌드 성공. 변경이 없는 백엔드 테스트는 기존 13개 성공 결과를 재사용했다.
- 잘못된 Target 입력은 종료 코드 1로 실패함을 확인했다.
- 격리된 가짜 명령으로 lint가 종료 코드 7을 반환하는 상황을 만들었다. 테스트·빌드로 진행하지 않고 작업 경로·환경변수를 복원함을 확인했다.

GitHub의 Ubuntu 실행 결과와 Codex UI 표시는 아직 확인하지 않았다. 다음 작업은 이 검증 기반을 사용해 리팩토링 후보를 검토하는 것이다.

## 4. 리뷰 단계

리뷰는 세 단계로 겹친다. 앞 단계가 다음 단계를 대신하지 않는다.

| 단계 | 시점 | 실행 | 결과가 남는 곳 |
| --- | --- | --- | --- |
| 독립 리뷰 에이전트 | PR 생성 전, 영향이 큰 변경 | Claude Code `superpowers:requesting-code-review` (`gh pr create`를 hook이 실행 전 1회 차단해 기준 전달, 확인 후 `PR_REVIEW_CHECKED=1`로 재실행) | `docs/verification.md` |
| CodeRabbit | PR 생성·커밋 추가 시 자동 | GitHub 앱 | PR 댓글 |
| 사람 리뷰 | 병합 전 | 팀원 | PR 리뷰 |

## 5. CodeRabbit PR 자동 리뷰

저장소에 CodeRabbit GitHub 앱을 설치했다(공개 저장소라 무료). `develop` 대상 PR이 열리거나 커밋이 추가되면 자동으로 리뷰 댓글을 단다. 설정은 루트 `.coderabbit.yaml`이다.

- 리뷰 언어는 한국어(`ko-KR`), 강도는 `chill`(실제 문제 위주)이다. 초안 PR과 `docs/issues/`, lockfile, 빌드 산출물은 리뷰하지 않는다.
- 경로별 지시(`path_instructions`)로 Security 접근 규칙·소유자 검증(#8)·테스트·프론트 토큰 저장·`.ps1`/`.sh` 짝 맞춤 등 이 저장소의 지침을 확인하게 했다.
- `request_changes_workflow: false`로 두어 CodeRabbit이 병합을 막지 않는다. 리뷰는 참고 의견이며, 병합 판단은 사람의 리뷰와 CI로 한다.
- 자동 리뷰는 한 PR에서 커밋 5개를 리뷰하면 멈춘다(`auto_pause_after_reviewed_commits` 기본값 5, 무료 한도를 아끼려고 기본값을 유지한다). 이후 커밋에도 자동 리뷰를 다시 받으려면 PR 댓글에 `@coderabbitai resume`을 남긴다.
- 한 번만 리뷰를 요청할 때는 `@coderabbitai review`(증분 리뷰) 또는 `@coderabbitai full review`(전체 다시 리뷰)를 쓴다. 이 명령들은 멈춘 자동 리뷰를 다시 켜지 않는다.
- 리뷰 지적은 실제 코드와 테스트로 판단한다. 맞는 지적은 고치고 관련 검사를 다시 실행하며, 틀린 지적은 근거를 PR에 남긴다. 설정 파일 변경은 해당 PR에서 바로 적용되므로, 설정 오류가 있으면 CodeRabbit 댓글에 표시된다.
