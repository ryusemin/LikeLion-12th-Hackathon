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

위 결과는 Windows 실행 결과다. Linux에서의 실제 실행 결과는 아직 없다. Gradle 테스트 보고서는 `Backend/build/reports/tests/test/index.html`, 결과 XML은 `Backend/build/test-results/test/`에서 확인할 수 있다. 생성물은 Git에서 제외한다.

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
- 프론트의 운영 API 주소·이전 쿠키 기반 인증은 그대로다. 전체 브라우저 연동, 서버 운영 기동, 실제 메일·AI·결제 요청은 검증하지 않았다.
- PR 검증 CI와 Codex 실행 Actions의 설정·로컬 검증은 [자동 검증 안내](ci-and-actions.md)에 기록했다. 원격 CI·앱 UI 확인은 아직 수행하지 않았다. 기존 배포 워크플로는 테스트 통과를 보장하지 않는다.
