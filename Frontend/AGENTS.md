# Frontend 작업 지침

루트 `AGENTS.md`와 함께 적용한다.

## 구조와 구현 기준

- React 18, JavaScript/JSX, React Router 6, react-scripts 5 기반이다. 기존 함수형 컴포넌트와 Hooks 패턴을 따른다.
- `src/pages/`는 화면, `src/components/`는 공통 UI, `src/CSS/`는 스타일이며 라우팅 진입점은 `src/App.js`다.
- 화면 변경은 기존 CSS와 컴포넌트 구성을 우선 활용한다. 요청 없이 TypeScript 전환, 빌드 도구 교체, 전역 상태 관리 도입을 섞지 않는다.
- 비동기 요청의 로딩·성공·실패 상태를 처리한다. 입력 폼과 버튼은 키보드 사용, 레이블과 오류 안내도 함께 확인한다.
- API 응답 형식·상태 코드·경로를 변경하면 Backend의 실제 구현과 대조한다. 브라우저의 관리자 화면 숨김은 서버 인가를 대신하지 않는다.

## 인증과 API 연동

- 현재 Backend는 로그인·재발급 응답 JSON으로 Access Token을 반환하고, API 요청에서 `Authorization: Bearer {accessToken}`을 읽는다. 로그인 응답 헤더에 토큰이 있다고 가정하지 않는다.
- Refresh Token은 HttpOnly 쿠키다. JavaScript로 쿠키 값을 읽거나 별도로 복사하지 않으며, 쿠키가 필요한 요청은 credentials와 CORS 설정을 함께 확인한다.
- API 호출은 `src/api/client.js`의 `apiFetch`·`login`·`logout`을 사용한다. Access Token은 이 모듈의 변수에만 두고 브라우저 저장소에 남기지 않는다. 페이지를 열면 첫 요청 전에 `/api/auth/refresh`로 한 번 복원하고, 401이면 재발급을 하나로 합쳐 한 번만 재시도하고, 재발급이 실패하면 비로그인 요청으로 다시 보낸다.
- 서버는 Refresh마다 Refresh Token을 교체하고 이전 토큰 재사용을 세션 탈취로 보아 폐기한다. 재발급 요청을 직접 추가하지 말고 공통 클라이언트를 거친다. 여러 탭의 동시 재발급 경합은 #11에서 다룬다.
- 인증 연동 변경은 로그인·사용자 조회·재발급·로그아웃·재접속 흐름을 함께 검토한다. 편의를 위해 Access Token을 localStorage에 저장하거나 Refresh Token의 HttpOnly를 해제하지 않는다. JWT payload의 역할은 화면 분기에만 쓰고 권한 판단은 서버가 한다.
- API·이미지·WebSocket·결제 복귀 주소는 `src/config/endpoints.js`를 통해 환경변수를 사용한다. `npm start`는 `.env.development`, 배포 빌드는 `.env.production`, 테스트는 `.env.test`를 읽는다. 개인 설정은 해당 환경의 `.local` 파일로 덮어쓰며, 테스트 중 네트워크 호출은 대체한다.
- WebSocket에서는 연결·구독·해제를 함께 확인하고 인증된 사용자와 구독 대상이 일치하는지 서버 계약을 확인한다. 현재 서버의 CONNECT 인증·구독 권한 보완은 #10에서 다룬다.

## 실행과 검증

전체 Docker 실행은 루트 `compose.yaml`과 `docs/docker-development.md`를 사용한다. 컨테이너 내부에서도 브라우저 API 주소는 `127.0.0.1:8080`이다. 프론트 src/public만 마운트하고 호스트 node_modules는 마운트하지 않는다. 패키지·env 변경은 이미지 재빌드 또는 컨테이너 재생성이 필요하다.

아래 명령은 `Frontend/` 디렉터리에서 실행한다. 지금까지 실행한 결과와 남은 경고는 `../docs/verification.md`에 기록한다.

| 목적 | Windows PowerShell | Linux / CI |
| --- | --- | --- |
| lockfile 기준 설치 | `npm.cmd ci` | `npm ci` |
| 개발 서버 | `npm.cmd start` | `npm start` |
| 테스트 일회 실행 | `npm.cmd run test:ci` | `npm run test:ci` |
| 브라우저 화면 검사 | `npm.cmd run test:e2e` | `npm run test:e2e` |
| Lint | `npm.cmd run lint` | `npm run lint` |
| 빌드 | `npm.cmd run build` | `npm run build` |

- build는 `react-scripts build`를 사용하고 공통 소스맵 옵션은 `.env.production`에서 설정한다. Windows에서도 `npm.cmd run build`를 사용한다.
- PR CI와 Codex Actions는 lint 오류를 실패 처리하고 경고를 출력한다. 이후 빌드의 중복 ESLint 검사는 생략한다. 같은 검증은 루트의 `scripts/verify.ps1 -Target Frontend`로 실행하며 정책은 `../docs/ci-and-actions.md`를 참고한다.
- Node 기준은 `.nvmrc`의 24.x LTS, npm은 `package.json`의 11.x이며 CI도 `.nvmrc`를 읽는다. 이 설정이 PC 버전을 자동 전환하거나 호환성 검증을 대신하지 않는다.
- npm과 기존 `package-lock.json`을 사용한다. 관련 없는 의존성 업그레이드나 패키지 매니저 교체를 하지 않는다. 설치 실패를 lockfile 삭제로 우회하지 않는다.
- `App.test.js`는 비로그인 화면·주문 진입·입력값 검증을 확인하며 fetch를 대체한다. 새 테스트도 운영 API나 WebSocket에 접속하지 않도록 구성한다. Jest의 Swiper 매핑은 설치된 실제 ESM 파일을 변환하기 위한 설정이다.
- UI 로직 변경은 사용자 입력·화면 상태·실패 처리 중심으로 검증한다. 화면을 바꿨다면 가능한 범위에서 브라우저로 확인하고, 확인하지 못한 범위는 명시한다.
- Playwright 검사는 Chromium으로 비로그인 화면을 확인하며 API 응답을 대체한다. 최초 브라우저 설치와 검사 범위는 `../docs/verification-workflow.md`를 따른다. 실제 로그인·가입 완료의 증거로 사용하지 않는다.
