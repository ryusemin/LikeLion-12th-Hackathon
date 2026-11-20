# 개발 환경 검증 기록

## 환경

- 검증일: 2026-11-02
- 기준 코드: `feat/global-exception-handler`의 `4c4f5ad` 및 현재 작업 트리의 준비 작업
- OS·셸: Windows, PowerShell
- Java: Temurin JDK 17.0.20.1
- Gradle: Wrapper 8.14
- Node.js: 24.22.2
- npm: 11.19.1

공식 배포본의 SHA-256을 확인한 후 `.local-tools/`에 JDK·Node를 풀어 사용했다. 전역 Java·Node는 변경하지 않았다. 다운로드·의존성 설치 및 Gradle/Jest의 샌드박스 권한 오류는 승인된 실행으로 재시도했다. `.local-tools/`는 Git에서 제외한다.

## 결과

| 작업 | 작업 디렉터리·명령 | 결과 |
| --- | --- | --- |
| 프론트 설치 | `Frontend`: `npm ci --no-audit --no-fund` | 성공, 기존 의존성 버전 유지 |
| 프론트 테스트 | `Frontend`: `npm run test:ci` | 3개 통과 |
| 프론트 빌드 | `Frontend`: `npm run build` | 성공, 기존 lint 경고 있음 |
| 백엔드 테스트·JAR | `Backend`: `gradlew.bat test bootJar --no-daemon --console=plain` | 13개 통과, 실행 JAR 생성 |

위 표는 최초 Windows 실행 결과다. Linux 컨테이너에서의 후속 검증은 아래 Docker 항목을 참고한다. 호스트 Gradle 테스트 보고서는 `Backend/build/reports/tests/test/index.html`, 결과 XML은 `Backend/build/test-results/test/`에서 확인할 수 있다. 생성물은 Git에서 제외한다.

2026-11-02 브랜치 분리·커밋 직전 `scripts/verify.ps1 -Target All`을 다시 실행해 종료 코드 0을 확인했다. 백엔드 테스트·JAR 작업은 Gradle의 `UP-TO-DATE` 결과로 기존 검증 산출물을 재사용했고, 프론트 테스트 3개와 빌드는 다시 실행되어 통과했다. lint는 오류 0개·경고 32개다. 이번 테스트 실행에는 기존 act API deprecation 외에 Header 비동기 상태 갱신의 act 경고도 출력되어 후속 정리가 필요하다. 샌드박스 내 첫 실행은 Gradle 데몬 연결에 실패했으며 승인된 외부 실행에서 전체 검증을 마쳤다.

## 해결한 실행 문제

- 기존 프론트 build의 `GENERATE_SOURCEMAP=false` 셸 문법은 Windows에서 실패했다. 옵션을 `.env.production`으로 옮기고 build를 `react-scripts build`로 통일했다. 생성된 소스맵은 0개였다.
- Jest 27이 Swiper 11의 exports·ESM 경로를 해석하지 못했다. 테스트에서 설치된 Swiper의 실제 파일을 매핑하고 해당 패키지의 ESM을 변환하도록 설정했다. 화면 컴포넌트 전체를 가짜로 바꾸지는 않았다.
- 기본 프론트 테스트는 현재 앱에 없는 `learn react` 문구를 검사했다. 실제 비로그인 화면과 로그인 진입·입력값 동작을 검증하도록 교체했다.
- 기존 백엔드 테스트는 로컬 `nimn.env`와 MySQL을 읽어 통과했다. 이후 H2·테스트 프로필·Mockito 대체로 분리해 그 파일이나 실행 중인 MySQL·Redis 없이 검증하도록 변경했다. 기존 로컬 설정 파일은 수정하지 않았다.
- H2에서 `DAY` 컬럼이 예약어여서 스키마 생성 경고가 발생했다. 테스트 DB에만 `NON_KEYWORDS=DAY`를 적용하고, DDL 오류가 발생하면 테스트가 실패하도록 설정했다.

## 테스트가 확인하는 것

| 테스트 | 확인 범위 |
| --- | --- |
| `NimnApplicationTests` 1개 | H2로 컨텍스트 초기화, 실제 접속 URL 확인, 스키마 생성 오류 시 실패 |
| `AuthServiceImplTest` 7개 | 누락·만료·폐기·불일치 토큰 거부, 유효 토큰 교체와 현재 권한 사용, 로그아웃 폐기·파싱 실패 처리 |
| `AuthControllerTest` 5개 | Access Token JSON과 HttpOnly 쿠키 계약, 누락 토큰 401, 내부 오류 메시지 비노출, 로그아웃 쿠키 제거 |
| `App.test.js` 3개 | 비로그인 추천 식단 표시, 주문 진입 시 로그인 이동, 빈 입력에서 로그인 API 호출 방지 |

컨텍스트 테스트에서는 Redis 연결·메일·결제·AI 클라이언트와 스케줄 작업을 Mockito로 대체한다. 프론트 테스트의 fetch는 모의 응답을 사용한다. 인증 컨트롤러 테스트는 MockMvc standalone 방식이므로 Security 필터·URL 인가 정책을 검증한 것은 아니다. 인증 서비스 테스트도 서명 암호화나 Redis 원자성을 검증하지 않는다.

최종 백엔드 실행에서는 프로세스에 `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:1/must_not_be_used`, MySQL 드라이버를 의도적으로 지정했다. 테스트는 이 값보다 우선하는 테스트 프로퍼티로 H2를 사용했고 13개가 모두 통과했다. 잘못 상속된 일반 프로필·DB 환경변수에 대한 격리를 확인한 것이며, 모든 형태의 외부 설정을 감사했다는 의미는 아니다.

## 남은 제약

### Swagger 명세 생성 오류 복구 (2026-11-02)

- 기존 Spring Boot 3.5.0 + springdoc 2.2.0에서 `/api-docs` 요청이 500을 반환했다. 실제 MVC와 전역 예외 처리기를 포함하는 테스트에서 `ControllerAdviceBean(Object)`의 `NoSuchMethodError`로 재현했다.
- Spring Boot를 3.5.16, springdoc을 2.10.0으로 맞췄다. [공식 호환표](https://springdoc.org/v2/#_what_is_the_compatibility_matrix_of_springdoc_openapi_with_spring_boot) 및 [springdoc 2.10.0 릴리스](https://github.com/springdoc/springdoc-openapi/releases/tag/v2.10.0)의 빌드 기준 조합이다. Java 17은 유지한다.
- springdoc만 2.9.1 또는 2.10.0으로 올리고 Boot 3.5.0을 유지하면 정적 리소스 경로 결합에서 `PatternParseException`으로 컨텍스트 시작이 실패했다. Boot 패치 버전을 함께 맞춰 해결했다. 전역 경로 매칭 설정이나 예외 처리기를 우회하지 않았다.
- `scripts/verify.ps1 -Target Backend` 종료 코드 0: 백엔드 테스트 14개와 `bootJar` 통과. 새 회귀 테스트는 `/api-docs`의 200 응답, OpenAPI 버전 및 `/users` 경로 생성을 확인한다.
- MVC 회귀 테스트는 필터를 제외하므로 인증·인가 검증을 의미하지 않는다. 기존 H2·외부 연동 대체 범위도 동일하다.
- 수정된 JAR를 로컬 MySQL의 `nimn_local`과 Redis를 사용하는 dev 프로필로 재시작했다. 실제 HTTP에서 `/api-docs`(OpenAPI 3.1.0, 경로 35개), `/api-docs/swagger-config`, `/docs`, `/swagger-ui/swagger-ui-bundle.js` 모두 200을 확인했다. dev 프로필의 명세에는 기존 설정에 따라 `/api` 접두사가 붙으며, test 프로필의 회귀 테스트 경로에는 붙지 않는다. 실제 업무 API나 브라우저의 Try it out을 호출한 검증은 아니다.

### 공통 제약

- H2의 MySQL 호환 모드는 실제 MySQL의 SQL·트랜잭션·동시성 검증을 대신하지 않는다. 실제 Redis TTL·Rotation 경쟁 조건과 함께 후속 통합 테스트가 필요하다.
- 기존 lint 경고(미사용 변수, Hook 의존성, 이미지 alt 누락), Browserslist 데이터 경고, 일부 라이브러리·Gradle의 deprecated 경고가 남아 있다. 숨기거나 전체 업그레이드로 범위를 확대하지 않았다.
- React Testing Library 13이 React 18.3에서 deprecated act API를 사용한다는 경고가 남는다. 테스트가 유발한 비동기 상태 갱신은 기다리도록 정리했다.
- npm 설치 시 오래된 전이 의존성 경고가 있다. 이번 실행은 보안 취약점 전체 감사가 아니며 의존성 일괄 업데이트도 하지 않았다.
- 프론트 주소는 아래와 같이 환경별로 분리했지만 이전 쿠키 기반 인증은 그대로다. 로컬 dev 서버 기동과 Swagger HTTP 응답은 위와 같이 확인했지만, 로그인 전체 연동, 서버 운영 기동, 실제 메일·AI·결제 요청은 검증하지 않았다.
- PR 검증 CI와 Codex 실행 Actions의 설정·로컬 검증은 [자동 검증 안내](ci-and-actions.md)에 기록했다. 원격 CI·앱 UI 확인은 아직 수행하지 않았다. 기존 배포 워크플로는 테스트 통과를 보장하지 않는다.

## 프론트 환경별 주소 분리 (2026-11-02)

- HTTP·이미지·SockJS·결제 복귀 주소의 운영 도메인 하드코딩을 공통 `endpoints.js` 호출로 교체했다. `.env.development`, `.env.production`, `.env.test`와 개인 설정용 `.env.example`을 제공한다.
- `scripts/verify.ps1 -Target Frontend` 종료 코드 0: lint 오류 0개·기존 경고 32개, 프론트 테스트 6개, 배포 빌드 성공.
- 신규 테스트는 로컬/배포 주소 선택, 이미지·결제 복귀 경로, WebSocket 사용자 이메일 인코딩 및 필수 API 주소 누락을 확인한다. 실제 외부 서비스에 접속하지 않는다.
- 백엔드 예제는 기존 `nimn.env`의 키 13개를 모두 포함하는지 값 출력 없이 비교했다. 원본 `nimn.env`는 수정하지 않았다.
- 사용자 요청 범위에 따라 로그인·재발급·로그아웃 구현은 변경하지 않았다.
- 개발 서버 재시작 후 `/`의 HTTP 200과 실제 번들의 로컬 API 주소 포함·운영 도메인 미포함을 확인했다. 배포 빌드는 운영 도메인 포함·로컬 API 주소 미포함을 확인했다. 실제 로그인·결제·메일·AI 요청은 실행하지 않았다.

## 전체 Docker 개발 환경 (2026-11-03)

- 루트 Compose로 frontend/backend/mysql/redis를 모두 실행했다. 네 서비스가 healthy이며, Windows의 이전 Node·Java 프로세스만 종료해 포트를 전환했다. Windows MySQL80과 이전 Redis 컨테이너는 유지한다.
- Linux 이미지 빌드에서 기존 백엔드 테스트 14개(실패·스킵 0개)와 `bootJar`, 프론트 `lint` 및 테스트 6개가 통과했다. 프론트 기존 lint 경고 32개와 라이브러리 경고는 남아 있다.
- 호스트에서 `/`(3000), `/docs`, `/api-docs`(8080) HTTP 200과 컨테이너 Redis PONG, Docker MySQL `nimn_docker` 접속을 확인했다.
- MySQL·Redis 전용 볼륨 연결을 확인했다. Redis 검증용 키는 재시작 후 유지됐으며 확인 후 제거했다. 기존 DB 데이터 이관이나 전체 DB 복구 시험을 한 것은 아니다.
- 프론트 소스에 임시 주석을 추가해 마운트·변경 감지·자동 재컴파일을 확인하고 원복했다. 브라우저의 전체 로그인 흐름이나 시각적 UI 검증은 아니다.
- `.env` 생성·기존 파일 보존·난수 길이·Git 제외를 확인했다. 실제 비밀값은 빌드 컨텍스트에서 제외하며 출력하지 않았다.
- Docker healthcheck는 포트/프로세스 기동 범위이며 인증·권한·결제 검증을 대신하지 않는다. 운영 배포와 실제 외부 연동은 변경하지 않았다.

## 검증 작업 흐름 추가 (2026-11-03)

- [공통 절차](verification-workflow.md)와 작업 기록 양식, Codex·Claude Code의 `verify-project` Skill을 추가했다. 두 Skill의 frontmatter 검사와 독립 판단 시나리오 검토를 통과했다. 도구의 실제 자동 선택이나 Claude Code 실행은 확인하지 않았다.
- 기존 전체 검증은 종료 코드 0이다. 백엔드는 기존 14개 테스트 결과를 Gradle이 재사용했고 JAR 생성 태스크가 성공했다. 프론트는 Jest 6개·배포 빌드·lint 오류 0개이며 기존 경고 32개는 남는다.
- Chromium에서 홈→주문→로그인 이동, 빈 로그인 경고·POST 미호출, 가입 초기 버튼 제한 등 3개가 통과했다. 독립 리뷰에서 기존 서버 재사용 문제를 발견해, 현재 소스로 전용 3001번 서버를 띄우도록 수정하고 3개를 다시 통과했다.
- 실행 상태 스크립트에서 네 서비스 healthy, 홈·Swagger·OpenAPI HTTP 200, Redis PONG을 확인했다. 서비스 누락(JSON 배열)·unhealthy(JSONL) 상황은 격리 검사 2개로 HTTP 검사 전 실패와 작업 경로 복원을 확인했다.
- actionlint와 로컬 환경 TOML 검사가 통과했다. 원격 GitHub CI는 아직 실행하지 않았다. 실제 인증·가입·메일·결제 연동 검증은 포함하지 않는다.
- Skill 없는 사전 판단 시나리오에서도 검증 범위 과장 없이 답했다. Skill 추가로 판단력이 개선됐다고 단정하지 않으며, 공통 절차와 프로젝트 명령을 재사용하는 진입점으로 둔다.

## 이메일 인증 증명 검증 (#6, 2026-11-19)

- 범위: 인증번호와 인증 성공 증명을 세션에서 Redis로 옮겼다. 증명은 email·목적(`SIGNUP`·`PASSWORD_RESET`)에 묶인 10분짜리 일회용 토큰이다. 가입·비밀번호 재설정은 이 토큰을 원자적으로 소비(`GETDEL`)해야 진행한다. 인증번호는 발급 기준 3분 유효, 확인 5회 제한, 재발송 60초 간격이다. 재설정 성공 시 Refresh Token을 폐기한다.
- 재설정은 증명 확인을 비밀번호 중복 검사보다 먼저 한다. 증명 없이 "중복" 응답으로 현재 비밀번호를 맞혀 보는 경로를 막기 위해서다.
- `scripts/verify.ps1 -Target All` 종료 코드 0: 백엔드 테스트 36개(신규 22개)·`bootJar`, 프론트 lint 오류 0개(기존 경고 32개)·Jest 8개(신규 2개)·빌드, Chromium 3개 통과.
- `EmailProofSecurityTest`는 dev 프로필의 Security 필터와 `/api` 경로로 증명 없는 재설정·가입이 403이고 비밀번호·회원이 변하지 않음을 확인한다. 재설정의 증명 확인을 임시로 끄면 이 테스트와 서비스 테스트 4개가 실패하는 것을 확인하고 원복했다.
- 실제 연동(2026-11-20): 루트 `compose.yaml`의 backend·MySQL 8.4·Redis 7.4.12에서 `/api` 경로로 직접 호출했다. 메일 서버가 없어 인증번호는 `redis-cli`로 넣었다.
  - 증명 없는 가입·재설정 403, 틀린 번호 401(시도 횟수 1), 맞는 번호 200과 토큰 발급(코드 키 삭제, 증명 TTL 약 600초), 같은 번호 재확인 400
  - 가입 토큰으로 재설정 403, 다른 email로 재설정 403, 증명 재사용 403
  - 증명을 갖춘 가입 201, 재설정 200 후 이전 비밀번호 로그인 401·새 비밀번호 로그인 200
  - 6번째 확인 시도 429와 코드 폐기, 쿨다운 키가 있을 때 발송 429, 메일 발송 실패 시 500과 코드·쿨다운 키 정리
- 함께 발견한 기존 제약: `t_user.email`이 `length = 30`이라 31자 이상 이메일 가입은 MySQL에서 503으로 실패했다. #19에서 수정했다.
- 미확인: 실제 메일 발송, 재설정 직후 Refresh Token 삭제의 실제 Redis 확인(단위 테스트만). 이미 발급된 Access Token 무효화(#11), 인가 분리(#7), 메일 발송 남용 제한은 범위 밖이다.

## 이메일 컬럼 길이 (#19, 2026-11-20)

- 범위: `Users.email`과 `DailyDiet.userEmail`의 길이를 30에서 254(전달 가능한 이메일 주소 최대 길이)로 늘렸다. 입력 형식·길이 검증(`@Email`·`@Size`)은 범위 밖이다.
- `EmailColumnLengthTest`(H2 MySQL 모드)는 254자 이메일로 회원과 일일 식단을 저장한다. 엔티티 하나씩 30으로 되돌리면 각각 실패하는 것을 확인하고 원복했다.
- 기존 DB 반영: 30자로 만들어진 로컬 Docker MySQL 8.4 볼륨에서 새 코드로 backend만 재시작하자, 수동 ALTER 없이 두 컬럼이 254로 바뀌었고 unique 인덱스·NOT NULL은 유지됐다. `ddl-auto=update`가 길이 확장을 반영한 것이다.
- 실제 연동: 같은 환경에서 61자 이메일로 증명을 갖춘 가입 201, 로그인 200, 재설정 200, 새 비밀번호 로그인 200.
- 미확인: 운영 DB는 운영 재개 후 첫 배포에서 같은 방식으로 반영되는지 컬럼 길이를 확인해야 한다. 반영되지 않으면 아래 SQL을 실행한다. 긴 이메일로 식단을 저장하는 API 호출은 실제 서버에서 하지 않았다(엔티티 저장은 테스트로 확인).

```sql
ALTER TABLE t_user MODIFY email VARCHAR(254) NOT NULL;
ALTER TABLE t_daily_diet MODIFY user_email VARCHAR(254);
```
