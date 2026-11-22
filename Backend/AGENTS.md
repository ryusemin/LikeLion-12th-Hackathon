# Backend 작업 지침

루트 `AGENTS.md`와 함께 적용한다.

## 구조와 구현 기준

- Java 17, Spring Boot 3.5.16, Gradle Wrapper를 사용한다. 의존성의 실제 선언은 `build.gradle`을 확인한다.
- 기본 패키지는 `src/main/java/com/demo/nimn/`이다. `controller`, `service`, `repository`, `entity`, `dto` 안에서 도메인별로 코드를 나눈다.
- 요청·응답은 DTO로 구분하고 Controller는 HTTP 처리를, Service는 업무 로직을, Repository는 DB 접근을 담당하게 한다. 필요한 객체는 생성자로 주입한다.
- 여러 저장 작업의 일관성은 서비스의 트랜잭션 경계에서 검토한다. 외부 API·Redis 작업까지 DB 트랜잭션으로 원자성이 보장된다고 가정하지 않는다.
- 목록 조회에서는 데이터 양과 연관관계 쿼리를 확인한다. 성능 문제는 쿼리·측정 근거로 판단하고, 검증 없이 캐시나 병렬 처리를 추가하지 않는다.

## 인증과 예외 처리

- 현재 Access Token은 로그인·재발급 응답 JSON의 `accessToken`으로 발급하고, 일반 요청의 `Authorization: Bearer {accessToken}`에서 검증한다. 응답 위치와 요청 위치를 구분한다.
- 현재 Refresh Token은 `refreshToken` HttpOnly 쿠키로 전달하며, Redis의 `refresh:{email}`에 7일 TTL로 저장한다. 인증 구조를 변경할 때는 필터·서비스·컨트롤러·프론트 호출을 함께 확인한다.
- dev/prod의 접근 규칙은 `config/security/ApiAccessRules.java` 한 곳에서 공개·회원(`authenticated`)·관리자(`ROLE_ADMIN`)로 나눈다. 새 API는 기본적으로 회원 전용이며, 공개·관리자 API는 이 파일에 경로와 HTTP 메서드를 함께 추가하고 `AbstractApiAccessControlTest`에 허용·거부 검사를 넣는다. 경로 전체가 관리자용이면 메서드 없이 지정한다. Spring MVC가 HEAD를 GET 핸들러로 실행하므로 GET만 지정하면 HEAD로 우회된다. 인가 거부는 `SecurityErrorHandler`가 401 `AUTHENTICATION_REQUIRED`·403 `ACCESS_DENIED`로 응답한다. 현재 본인 데이터인지 확인하는 소유자 검증은 보완 대상이다(#8). 테스트 편의를 위해 인가를 더 완화하지 않는다. 로그인 확인과 관리자 권한·데이터 소유자 검증은 각각 검토한다.
- 인증된 사용자 정보는 SecurityContext의 Principal을 기준으로 삼는다. 요청 본문의 이메일이나 사용자 ID만으로 타인의 데이터를 변경할 권한을 인정하지 않는다.
- 토큰 재발급에서는 서명·만료·용도, Redis 저장값, Rotation의 동시성·재사용 정책을 확인한다. Redis의 조회·삭제·저장을 각각 호출하면 원자적 교체가 아니다. 현재 구현의 보완은 #11에서 다룬다.
- 쿠키를 변경하면 발급·갱신·삭제의 Path·Domain과 HttpOnly·Secure·SameSite, CSRF 및 프론트 credentials 설정을 함께 검토한다.
- MVC 업무 예외는 기존 `CustomException`, `ErrorCode`, `ErrorResponse`, `GlobalExceptionHandler`를 우선 활용한다. 클라이언트에 내부 예외 메시지나 스택을 노출하지 않는다.
- `@RestControllerAdvice`가 앞단 Security 필터의 예외까지 처리한다고 가정하지 않는다. 필터 실패 응답은 해당 필터 또는 Spring Security 예외 처리 경로에서 확인한다.
- 이메일 인증 성공은 가입·비밀번호 재설정 API에서 서버가 검증해야 한다. 프론트에서 인증 화면을 거쳤다는 사실을 서버 검증의 대체로 삼지 않는다. 인증번호와 인증 성공 증명(`EmailVerificationService`)은 Redis에 email·목적(`SIGNUP`·`PASSWORD_RESET`)별로 저장하며, 가입·재설정 API는 증명 토큰을 소비한 뒤에만 진행한다.

## 실행과 검증

아래 명령은 `Backend/` 디렉터리에서 실행한다. 지금까지 실행한 결과와 범위는 `../docs/verification.md`에 기록한다.

| 목적 | Windows PowerShell | Linux / CI |
| --- | --- | --- |
| 컴파일 | `.\gradlew.bat compileJava` | `./gradlew compileJava` |
| 테스트 | `.\gradlew.bat test` | `./gradlew test` |
| 특정 테스트 | `.\gradlew.bat test --tests '패키지명.테스트클래스'` | `./gradlew test --tests '패키지명.테스트클래스'` |
| 실행 JAR 생성 | `.\gradlew.bat bootJar` | `./gradlew bootJar` |
| 로컬 서버 실행 | `.\gradlew.bat bootRun` | `./gradlew bootRun` |

- 설정 진입점은 `src/main/resources/application.properties`이며 `nimn.env`를 선택적으로 읽는다. `SPRING_PROFILES_ACTIVE`, DB·JWT·메일·외부 API 설정이 필요한지 확인한다.
- 전체 로컬 실행은 저장소 루트 `compose.yaml`과 `docs/docker-development.md`를 우선 사용한다. Docker DB는 `nimn_docker`, PC 포트는 MySQL 3307·Redis 6380이며 컨테이너끼리는 mysql:3306·redis:6379로 연결한다. 기존 `Backend/compose.yml`은 인프라만 띄우는 이전 구성으로 PC 포트 3306·6379를 사용하므로 혼동하지 않는다.
- 단위 테스트에서는 외부 호출을 대체한다. 통합 테스트는 로컬·테스트 DB와 Redis를 사용하고 실제 메일·결제·AI API를 호출하지 않도록 구성한다.
- 컨텍스트 테스트는 `test` 프로필과 H2를 사용하고 `nimn.env`를 읽지 않는다. 외부 클라이언트·Redis 연결·스케줄 작업은 Mockito로 대체한다. MySQL·Redis 실제 연동이나 Security 필터까지 검증한 것으로 해석하지 않는다.
- 배포 워크플로의 `build -x test`는 테스트 통과의 증거가 아니다. 변경한 보안·업무 로직의 검증은 따로 수행한다.
- API를 변경하면 관련 Swagger 설명·응답 코드·프론트 호출을 확인한다. Swagger 설명보다 실제 응답 코드를 기준으로 판단한다.
