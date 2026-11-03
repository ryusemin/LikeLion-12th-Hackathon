# 로컬 개발 환경

## 1. 실행 기준

| 구성 | 프로젝트 기준 | 확인 위치 |
| --- | --- | --- |
| Java | JDK 17 | `Backend/build.gradle`의 toolchain |
| Spring Boot | 3.5.0 | `Backend/build.gradle` |
| Gradle | Wrapper 8.14 | `Backend/gradle/wrapper/gradle-wrapper.properties` |
| Node.js | 24.x LTS의 최신 패치 | `Frontend/.nvmrc` |
| npm | 11.x | `Frontend/package.json`의 engines |
| 로컬 DB·캐시 | MySQL·Redis | `Backend/compose.yml` |

Node 18·23·26으로 나뉘어 있던 실행 기준을 Node 24 LTS로 정리했다. 지원 상태는 [Node.js 공식 릴리스 안내](https://nodejs.org/en/about/previous-releases)를 참고한다. CI도 `.nvmrc`를 읽는다. 메이저 버전을 고정하되 보안 패치를 받을 수 있게 패치 버전은 고정하지 않는다.

`.nvmrc`나 engines는 PC의 Node를 자동 설치·전환하지 않는다. Windows에서는 사용 중인 버전 관리 도구 또는 [공식 다운로드](https://nodejs.org/en/download)로 Node 24.x와 npm 11.x를 준비하고, 새 터미널에서 확인한다. nvm이 설치된 Linux/macOS에서는 `Frontend/`에서 `nvm install`, `nvm use`로 선택할 수 있다.

```powershell
# 작업 위치: 저장소 루트 / Windows PowerShell
java -version
javac -version
node --version
npm.cmd --version
docker compose version
```

Windows에서 JDK 17·Node 24·npm 11로 설치·테스트·빌드를 검증했다. 실제 버전·결과·남은 경고는 [검증 기록](verification.md)을 참고한다. 전역 런타임은 변경하지 않고, 검증용 런타임과 캐시는 Git에서 제외한 `.local-tools/`에 준비했다.

이 작업 환경의 프로젝트용 런타임을 다시 사용하려면 새 PowerShell에서 아래처럼 현재 프로세스에만 설정할 수 있다. 다른 PC에서는 설치한 JDK 17·Node 24 경로를 사용한다.

```powershell
# 작업 위치: 저장소 루트 / Windows PowerShell
$localJdk = Get-ChildItem .local-tools -Directory -Filter 'jdk-17*' | Select-Object -First 1
$localNode = Get-ChildItem .local-tools -Directory -Filter 'node-v24.*-win-x64' | Select-Object -First 1
if (-not $localJdk -or -not $localNode) { throw '프로젝트용 런타임이 없습니다. JDK 17과 Node 24를 준비하세요.' }
$env:JAVA_HOME = $localJdk.FullName
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $localNode.FullName + ';' + $env:PATH
$env:GRADLE_USER_HOME = Join-Path (Get-Location).Path '.local-tools/gradle'
$env:npm_config_cache = Join-Path (Get-Location).Path '.local-tools/npm-cache'
```

## 2. 백엔드 환경변수 준비

`Backend/src/main/resources/application.properties`는 작업 디렉터리의 `nimn.env`를 Java properties 형식으로 읽는다. 백엔드 명령은 `Backend/`에서 실행한다. 파일에는 `export`나 셸 명령을 넣지 않는다.

```powershell
# 작업 위치: 저장소 루트 / Windows PowerShell
Set-Location Backend
if (Test-Path -LiteralPath nimn.env) {
    throw '기존 nimn.env가 있습니다. 덮어쓰지 말고 예제와 필요한 항목만 비교하세요.'
}
Copy-Item -LiteralPath nimn.env.example -Destination nimn.env
```

복사한 파일의 JWT 키를 아래 명령으로 생성한다. 키를 화면에 출력하지 않고 로컬 파일에 기록한다. 예제 파일의 표시값만 교체하므로 이미 변경한 키는 덮어쓰지 않는다.

```powershell
# 작업 위치: Backend / Windows PowerShell
$localKeyBytes = New-Object byte[] 32
$localKeyGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $localKeyGenerator.GetBytes($localKeyBytes) }
finally { $localKeyGenerator.Dispose() }
$localEnvPath = Join-Path (Get-Location).Path 'nimn.env'
$localEnvText = [System.IO.File]::ReadAllText($localEnvPath)
$localEnvText = $localEnvText.Replace('JWT_SECRET=REPLACE_WITH_BASE64_32_BYTE_RANDOM_KEY', 'JWT_SECRET=' + [Convert]::ToBase64String($localKeyBytes))
[System.IO.File]::WriteAllText($localEnvPath, $localEnvText, [System.Text.UTF8Encoding]::new($false))
```

`nimn.env`는 `Backend/.gitignore`에서 제외하며, `nimn.env.example`만 공유한다. IDE로 실행할 때도 작업 디렉터리를 `Backend/`로 지정한다. 기존 OS 환경변수가 있으면 파일보다 우선할 수 있으므로 대상 DB와 프로필을 함께 확인한다.

| 설정 | 용도·로컬 기본값 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 로컬은 `dev`; 현재 dev도 전체 요청 허용이므로 격리된 로컬에서 사용 |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | 기존 Compose의 로컬 데모 계정; 운영용 자격증명이 아님 |
| `spring.datasource.url` | 기존 기본 DB와 구분한 `nimn_local` 사용 |
| `spring.data.redis.host`, `spring.data.redis.port` | 로컬 Redis 위치 |
| `JWT_SECRET` | HS256 서명을 위한 32바이트 이상 난수의 Base64 값 |
| `OPENAI_API_KEY`, `OPENAI_CHAT_MODEL`, `OPENAI_IMAGE_MODEL` | AI 클라이언트 초기화용 더미값 |
| `OPENAI_CHAT_URL`, `OPENAI_IMAGE_URL` | 로컬 대체 서버 주소; 서버는 별도로 준비해야 함 |
| `import_apiKey`, `import_secretKey` | 결제 클라이언트 초기화용 더미값; 기존 대소문자 유지 |
| `GOOGLE_EMAIL`, `GOOGLE_APP_PASSWORD` | 메일 설정용 더미값 |
| `spring.mail.*` | 로컬 SMTP `127.0.0.1:1025`, 인증·STARTTLS 비활성화 |

예제의 외부 연동값은 실제 서비스를 사용할 수 있는 값이 아니다. SMTP·AI 대체 서버는 자동 실행되지 않으며, 결제 SDK의 호출 대상도 로컬로 변경되지 않는다. 실제 메일·AI·결제 동작을 확인하려면 다음 단계에서 호출 대체 또는 명시적인 테스트 환경을 준비한다.

`nimn.env.example`은 현재 실제 `nimn.env`의 필수 변수 이름 13개를 모두 포함하며, 실제 비밀값은 복사하지 않는다. 파일 이름이 `.env`가 아닌 `nimn.env`인 것은 `spring.config.import`가 이 이름을 읽도록 설정돼 있기 때문이다. 개인 설정 파일은 그대로 보존하고 예제만 공유한다. 배포에서는 `SPRING_PROFILES_ACTIVE=prod`와 배포 환경의 DB·Redis·JWT·외부 연동 설정을 사용한다.

## 3. 로컬 MySQL·Redis

Docker Desktop의 엔진이 실행 중이어야 한다. `compose.yml`은 MySQL 3306, Redis 6379를 사용한다. 기존 서비스가 해당 포트를 사용 중이면 종료·삭제로 우회하지 말고 사용할 인프라와 연결 설정을 먼저 정한다.

```powershell
# 작업 위치: Backend / Windows PowerShell
docker compose up -d
docker compose ps
```

MySQL이 접속 가능한 상태인지 확인한 뒤 백엔드를 실행한다. 예제 비밀번호는 기존 Compose의 신규 컨테이너 기준이며, 기존 DB를 재사용한다면 해당 로컬 계정에 맞춰 `nimn.env`를 수정한다.

- 현재 JPA의 `ddl-auto=update`는 연결한 DB 스키마를 변경할 수 있다. 전용 로컬 DB를 사용한다.
- Compose에는 DB·Redis 이미지 버전과 명시적 데이터 볼륨이 고정되어 있지 않다. 컨테이너 재생성 후의 데이터 보존을 보장하는 구성으로 간주하지 않는다.
- 필요할 때 `docker compose stop`으로 중지한다. 이 안내에서는 컨테이너나 볼륨 삭제를 수행하지 않는다.
- 스케줄러는 앱 실행 중 활성화된다. 빈 DB의 식단 생성·알림 작업까지 정상 동작이 검증된 상태는 아니다.

## 4. 백엔드 실행·검증 명령

```powershell
# 작업 위치: Backend / Windows PowerShell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat bootJar
```

각 명령의 결과를 확인한다. 컴파일·패키징 성공은 테스트 성공이나 서버 정상 실행을 의미하지 않는다. Linux/CI에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용한다.

설정과 로컬 인프라를 준비한 뒤 서버 실행은 별도 터미널에서 수행한다.

```powershell
# 작업 위치: Backend / Windows PowerShell
.\gradlew.bat bootRun
```

기본 포트는 8080이며 Swagger UI는 [로컬 /docs](http://localhost:8080/docs), 명세는 [로컬 /api-docs](http://localhost:8080/api-docs)다. 앱이 실제로 시작된 뒤 확인한다.

기본 테스트는 `test` 프로필의 H2를 사용하고 `nimn.env` import를 비운다. 컨텍스트 테스트의 DB 접속값은 테스트 프로퍼티로 고정해 기존 환경변수의 실제 DB를 읽지 않도록 한다. Redis 연결, 메일·결제·AI 클라이언트, 정기 알림·식단 작업은 Mockito로 대체한다.

따라서 `gradlew test`에는 Docker·로컬 MySQL·Redis·실제 API 키가 필요하지 않다. 환경 설정 실패를 테스트 제외로 우회하지 않는다. H2 스키마 생성 오류는 실패로 처리하며, MySQL과 다른 예약어 `DAY`만 테스트 URL에서 비예약어로 설정한다. 이 테스트는 MySQL SQL·Redis 동작이나 다중 서버의 동시성을 보장하지 않는다.

## 5. 프론트엔드 준비·검증 명령

```powershell
# 작업 위치: 저장소 루트 / Windows PowerShell
Set-Location Frontend
node --version
npm.cmd --version
npm.cmd ci
npm.cmd run test:ci
```

Node 24.x·npm 11.x를 선택한 뒤 기존 lockfile로 설치한다. 설치 실패 시 오류를 기록하며, lockfile 삭제나 의존성 일괄 업그레이드로 우회하지 않는다. `App.test.js`는 실제 App의 비로그인 화면·주문 시 로그인 이동·빈 로그인 입력값을 검증하며 fetch를 대체한다. Jest는 실제 Swiper ESM 파일을 매핑·변환해 읽는다.

빌드 명령은 Windows·Linux에서 동일한 npm 스크립트를 사용한다. `GENERATE_SOURCEMAP=false` 옵션은 `.env.production`으로 옮겨 셸별 환경변수 문법 차이를 제거했다.

```powershell
# 작업 위치: Frontend / Windows PowerShell
npm.cmd run build
```

Linux/CI에서는 `npm.cmd` 대신 `npm`을 사용한다. `npm run lint`로 오류를 별도 검증하며 경고는 표시한다. CI와 Codex 검증 Actions는 lint 통과 후 중복 ESLint 플러그인 검사 없이 빌드한다. 정확한 적용 범위는 [PR 검증과 Actions](ci-and-actions.md)를 참고한다.

## 6. 프론트 주소의 로컬·배포 환경 분리

| 실행 | 자동으로 읽는 파일 | API 기본 주소 | 프론트 기본 주소 |
| --- | --- | --- | --- |
| `npm.cmd start` | `Frontend/.env.development` | `http://127.0.0.1:8080` | `http://127.0.0.1:3000` |
| `npm.cmd run build` | `Frontend/.env.production` | `https://nimn.store` | `https://nimn.store` |
| `npm.cmd run test:ci` | `Frontend/.env.test` | 로컬 주소, 네트워크 대체 | 로컬 주소 |

`src/config/endpoints.js`가 다음 값을 읽으며, 실제 화면의 HTTP·이미지·WebSocket·결제 복귀 주소에서 함께 사용한다.

| 환경변수 | 의미 |
| --- | --- |
| `REACT_APP_API_BASE_URL` | 백엔드 기본 주소. **`/api`를 붙이지 않는다.** 호출부에 `/api/users` 등의 경로가 이미 있다. |
| `REACT_APP_WS_URL` | SockJS 연결 주소 전체. 로컬은 `http://127.0.0.1:8080/ws/notification`, 배포는 `https://nimn.store/ws/notification`이다. SockJS이므로 `http(s)`를 사용한다. |
| `REACT_APP_APP_BASE_URL` | 사용자가 접속하는 프론트 주소. 결제 완료 후 복귀 주소에 사용한다. |

기본 파일에 로컬·배포 주소가 있으므로 복사 없이 실행할 수 있다. PC별 주소를 바꾸려면 다음과 같이 예제를 개인 개발 설정으로 복사한다. 기존 파일이 있으면 덮어쓰지 않는다.

```powershell
# 작업 위치: Frontend / Windows PowerShell
if (Test-Path -LiteralPath .env.development.local) { throw '기존 개인 설정이 있습니다.' }
Copy-Item -LiteralPath .env.example -Destination .env.development.local
```

배포 주소를 개인적으로 바꾸는 파일은 `.env.production.local`이며, CI에서는 동일한 `REACT_APP_*` 환경변수로 지정할 수도 있다. 개인 `.local` 파일은 Git에서 제외한다. 공통 `.env.local`은 개발·배포 양쪽에 영향을 줄 수 있으므로, 개발 주소를 바꿀 때는 `.env.development.local`을 사용한다.

CRA가 주소를 빌드할 때 포함하므로 **개발 서버는 환경변수 변경 후 재시작**, 배포 주소 변경 후에는 **재빌드·재배포**해야 한다. 프론트 환경변수에는 비밀키를 넣지 않는다. 로컬에서는 쿠키의 사이트 구분이 달라지지 않도록 프론트와 API 모두 `127.0.0.1`로 접속한다.

이번 작업은 주소만 분리한다. 이전 쿠키 기반 Access Token 처리, 이전 로그인·로그아웃 경로와 자동 재발급은 그대로여서 로그인 전체 연동은 후속 작업이다. 실제 결제·메일·AI 호출도 검증하지 않는다.

## 7. 검증 범위와 다음 작업

1. 현재 검증 결과와 재현 명령은 [검증 기록](verification.md)에서 확인한다.
2. PR 검증 CI와 Codex Actions의 설정·사용법은 [자동 검증 안내](ci-and-actions.md)에서 확인한다. Linux에서의 실제 실행은 원격 CI로 추가 확인한다.
3. 실제 MySQL·Redis·Security 필터를 포함한 통합 검증은 별도로 추가한다.
4. 로컬 API 주소 분리는 완료했으며, 이전 인증 코드를 현재 백엔드 계약으로 연결하는 변경은 별도 범위로 진행한다.

검증한 것은 의존성 설치·자동 테스트·컴파일·패키징이다. 서버 실행, 브라우저에서의 전체 연동, 실제 결제·메일·AI 호출의 성공까지 확인한 것은 아니다.
