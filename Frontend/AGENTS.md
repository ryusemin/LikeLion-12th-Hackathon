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
- 현재 Login·App·Header 등에는 과거 쿠키 기반 인증 코드가 남아 있다. `Cookies.get('token')` 또는 기존 로그인 주소를 새 API 계약으로 간주하지 않는다.
- 인증 연동 변경은 로그인·사용자 조회·재발급·로그아웃·재접속 흐름을 함께 검토한다. 자동 재발급을 구현한다면 동시 401의 중복 재발급과 무한 재시도를 방지한다.
- 토큰 저장 위치는 기존 동작과 보안 영향을 확인해 결정한다. 편의를 위해 Refresh Token을 localStorage로 옮기거나 HttpOnly를 해제하지 않는다.
- 여러 화면에 `https://nimn.store`가 직접 사용된다. 테스트 중 해당 운영 API에 요청하지 않도록 네트워크 호출을 대체한다. 로컬 대상 설정은 관련 작업에서 명시적으로 정리한다.
- WebSocket에서는 연결·구독·해제를 함께 확인하고 인증된 사용자와 구독 대상이 일치하는지 서버 계약을 확인한다.

## 실행과 검증

아래 명령은 `Frontend/` 디렉터리에서 실행한다. Node 24·npm 11에서 설치·테스트·빌드를 검증했으며, 환경과 남은 경고는 `../docs/verification.md`를 참고한다.

| 목적 | Windows PowerShell | Linux / CI |
| --- | --- | --- |
| lockfile 기준 설치 | `npm.cmd ci` | `npm ci` |
| 개발 서버 | `npm.cmd start` | `npm start` |
| 테스트 일회 실행 | `npm.cmd run test:ci` | `npm run test:ci` |
| Lint | `npm.cmd run lint` | `npm run lint` |
| 빌드 | `npm.cmd run build` | `npm run build` |

- build는 `react-scripts build`를 사용하고 공통 소스맵 옵션은 `.env.production`에서 설정한다. Windows에서도 `npm.cmd run build`를 사용한다.
- PR CI와 Codex Actions는 lint 오류를 실패 처리하고 경고를 출력한다. 이후 빌드의 중복 ESLint 검사는 생략한다. 같은 검증은 루트의 `scripts/verify.ps1 -Target Frontend`로 실행하며 정책은 `../docs/ci-and-actions.md`를 참고한다.
- Node 기준은 `.nvmrc`의 24.x LTS, npm은 `package.json`의 11.x이며 CI도 `.nvmrc`를 읽는다. 이 설정이 PC 버전을 자동 전환하거나 호환성 검증을 대신하지 않는다.
- npm과 기존 `package-lock.json`을 사용한다. 관련 없는 의존성 업그레이드나 패키지 매니저 교체를 하지 않는다. 설치 실패를 lockfile 삭제로 우회하지 않는다.
- `App.test.js`는 비로그인 화면·주문 진입·입력값 검증을 확인하며 fetch를 대체한다. 새 테스트도 운영 API나 WebSocket에 접속하지 않도록 구성한다. Jest의 Swiper 매핑은 설치된 실제 ESM 파일을 변환하기 위한 설정이다.
- UI 로직 변경은 사용자 입력·화면 상태·실패 처리 중심으로 검증한다. 화면을 바꿨다면 가능한 범위에서 브라우저로 확인하고, 확인하지 못한 범위는 명시한다.
