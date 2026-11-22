# 작업 완료 기준과 검증 절차

작업 전에 확인할 동작을 정하고, 구현 후 같은 기준으로 실제 결과를 확인한다. 자동 검사·브라우저 확인·독립 리뷰는 서로 다른 근거다. AI 리뷰만으로 테스트 통과나 사람의 승인을 대신하지 않는다.

## 구현 전에 정할 것

의미 있는 기능·리팩토링 작업에는 [작업 기록 양식](templates/task-verification.md)을 사용한다. 작은 문구 수정에는 대화에서 간단히 기록해도 된다.

- 범위: 사용자가 어떤 동작을 할 수 있어야 하는가?
- 제외: 이번에 변경하지 않을 동작·기술은 무엇인가?
- 완료 기준: 입력·행동에 대한 기대 결과와 실패 조건은 무엇인가?
- 확인 수단: 각 기준을 어떤 테스트·명령·화면으로 확인할 것인가?

예: 인증 연동이라면 정상 로그인뿐 아니라 잘못된 비밀번호, 만료 토큰, 재발급 실패, 로그아웃 후 접근을 기준으로 정한다. 인가 변경에는 타인 데이터 접근 거부와 데이터가 변경되지 않았다는 검사도 필요하다. 아직 구현하지 않은 동작을 기존 테스트 통과만으로 완료 처리하지 않는다.

## 검사 선택

저장소 루트의 Windows PowerShell에서 실행한다. macOS(zsh)에서는 같은 대상을 `bash scripts/verify.sh backend|frontend|browser|all|runtime`으로 실행한다. 설치·환경 준비는 [개발 환경](development.md), Docker 준비는 [Docker 실행](docker-development.md)을 따른다.

| 명령 | 확인 범위 | 보장하지 않는 것 |
| --- | --- | --- |
| `powershell -NoProfile -File scripts/verify.ps1 -Target Backend` | 백엔드 테스트와 실행 JAR 생성 | 실제 MySQL·Redis·보안 필터 전체 검증 |
| `powershell -NoProfile -File scripts/verify.ps1 -Target Frontend` | ESLint 오류, Jest 테스트, 배포 빌드 | 실제 브라우저·서버 연동 |
| `powershell -NoProfile -File scripts/verify.ps1 -Target Browser` | Chromium의 비로그인 화면·이동·입력 제한 | 실제 인증·메일·결제·가입 |
| `powershell -NoProfile -File scripts/verify.ps1 -Target All` | 위 세 가지 코드·화면 검사, 실행 상태 스크립트의 실패 처리 검사 | Docker 실행 상태·전체 업무 기능 |
| `powershell -NoProfile -File scripts/verify.ps1 -Target Runtime` | 실행 중인 네 서비스의 health, 홈·Swagger·OpenAPI 응답, Redis PING | 현재 소스와 이미지 일치·데이터 영속성·인증 연동 |

브라우저 검사는 최초 한 번 `Frontend/`에서 `npx.cmd playwright install chromium`(macOS는 `npx playwright install chromium`)이 필요하다. 같은 디렉터리에서 `npm.cmd run test:e2e`(macOS는 `npm run test:e2e`)로 직접 실행할 수 있다. 로컬·CI 모두 현재 소스로 3001번 개발 서버를 새로 시작하고 검사 후 종료한다. 해당 포트가 이미 사용 중이면 실패하며 기존 Docker의 3000번 서버는 유지한다. API 응답은 대체하며 외부 접속은 차단한다. 실패한 화면·trace는 `Frontend/test-results/`, HTML 보고서는 `Frontend/playwright-report/`에 생성한다. 이 파일들은 Git에 올리지 않는다.

변경에 해당하는 검사부터 실행한다. 프론트 화면 변경에는 브라우저 검사를 포함하고, 백엔드 인증·인가에는 실제 Security 필터가 켜진 허용·거부 테스트를 추가한다. 기존 H2·mock 테스트나 필터를 끈 Swagger 테스트는 이를 대체하지 않는다. 환경 설정 변경은 로컬·배포 주소 선택을, Docker 변경은 이미지 빌드와 실행 검사를 확인한다. 문서만 바꾼 경우 전체 빌드를 반복하지 않는다.

실행 상태 스크립트 자체를 바꾸면 `powershell -NoProfile -File scripts/tests/verify-runtime.test.ps1`로 서비스 누락·unhealthy 상태의 실패 처리와 작업 경로 복원을 확인한다. macOS용 `verify-runtime.sh`를 바꾸면 `bash scripts/tests/verify-runtime.test.sh`로 같은 실패 처리를 확인한다. 실제 Docker는 사용하지 않는 격리 검사다.

## 실행 대상과 증거

검사 전에 브랜치와 `git status --short`를 확인한다. 컨테이너가 예전 JAR로 실행되고 있으면 새 소스가 검증된 것이 아니다. 백엔드 변경 후 실행을 확인할 때는 현재 소스로 `docker compose up -d --build backend`를 수행하고 새 컨테이너의 시작·health와 해당 API를 확인한다. 패키지·프론트 환경변수 변경은 프론트 이미지 재빌드도 필요하다. 단순 health 통과를 소스 일치의 증거로 쓰지 않는다.

실행한 명령, 종료 코드, 테스트 결과와 확인한 환경을 기록한다. 실패는 이번 변경·기존 문제·환경 문제로 구분한다. 실패를 숨기기 위해 테스트를 제거하거나 인가를 완화하지 않는다. 도구를 사용할 수 없으면 검증하지 못한 범위를 남긴다. 실제 비밀번호·토큰·개인정보나 확장된 Compose 환경 설정을 보고서에 출력하지 않는다.

## 리뷰와 완료 보고

인증·인가·데이터 변경·보안 설정 변경은 PR을 만들기 전에 독립 리뷰를 실행한다. Claude Code에서는 `superpowers:requesting-code-review`로 작업 맥락 없는 리뷰어 에이전트에 범위·완료 기준·커밋 구간·검사 결과를 전달하고 보안·회귀·빠진 검사에 집중하게 한다. 결과와 조치는 `docs/verification.md`에 남긴다. 리뷰 에이전트를 사용할 수 없으면 직접 검토했다고 명시한다. 보안 설정 변경은 크기와 관계없이 대상이다. 이 네 가지에 해당하지 않는 문서·일반 설정의 작은 변경에는 강제하지 않는다. Claude Code의 프로젝트 hook(`.claude/settings.json`)은 `gh pr create`를 실행 전에 한 번 막고, 막은 이유로 이 기준을 Claude에게 전달한다. Claude는 기준에 따라 리뷰를 마쳤거나 해당하지 않음을 확인한 뒤 명령 앞에 `PR_REVIEW_CHECKED=1`을 붙여 다시 실행한다. 이 표시는 PR 생성 명령 바로 앞의 환경 변수 할당일 때만 인정하며, PR 본문 등 인자 안의 같은 글자는 인정하지 않는다. 확인 창(ask)은 자동 승인 모드에서 표시되지 않아 쓰지 않는다. 이 표시는 강제가 아니라 PR 생성 전에 기준을 확실히 떠올리게 하는 장치다. CodeRabbit 자동 리뷰는 `develop` 대상의 초안이 아닌 PR에만 실행되고 `.coderabbit.yaml`의 제외 경로는 리뷰하지 않는다. 다른 브랜치 대상·초안 PR이나 제외 경로만 바꾼 PR에는 리뷰가 달리지 않을 수 있으므로 검토자가 확인한다([PR 검증과 Actions](ci-and-actions.md)). 리뷰 지적은 실제 코드로 판단하고, 수정했다면 관련 검사를 다시 실행한다.

마지막으로 변경 파일과 `git diff --check`를 확인한다. 보고에는 변경 내용, 실행한 검증, 확인하지 못한 동작을 구분한다. 커밋·push·배포는 별개의 작업이며 이 절차나 Skill 자체가 외부 작업 권한을 부여하지 않는다.

## Codex와 Claude Code에서 사용

공통 기준은 이 문서에 두고 도구별 Skill은 이를 읽도록 구성한다. Codex는 `$verify-project`, Claude Code는 `/verify-project`로 명시적으로 요청할 수 있다. 새 Skill이 목록에 없다면 프로젝트를 연 새 세션에서 확인한다. 이 저장소의 파일 추가만으로 사용자 전역 설정이나 도구 권한이 바뀌지 않는다.

- [Codex 공식 Skill 문서](https://learn.chatgpt.com/docs/build-skills): `.agents/skills/verify-project/SKILL.md`
- [Claude Code 공식 Skill 문서](https://code.claude.com/docs/en/skills): `.claude/skills/verify-project/SKILL.md`
- [Playwright 서버 관리](https://playwright.dev/docs/test-webserver), [네트워크 대체](https://playwright.dev/docs/network)
