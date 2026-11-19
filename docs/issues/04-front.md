# [Refactor] 프론트엔드 인증을 JSON Access Token·Bearer 요청 방식으로 연동

대상: 기존 [#4](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/4) 갱신

### 📋 작업 내용
현재 백엔드 계약에 맞춰 로그인·사용자 조회·재발급·로그아웃과 공통 API 호출을 연결한다.

**우선순위:** P0 · 인증 기능 연결 / 서버 접근 권한 변경과 함께
**검토 기준:** `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

### 🔍 현재 상황
- 기존 이슈의 ‘로그인 응답 헤더에서 Access Token을 수신한다’는 설명은 기준 브랜치의 구현과 다르다.
- 현재 백엔드는 로그인·재발급 응답 JSON의 accessToken으로 전달하고, 일반 요청의 Authorization: Bearer 헤더에서 검증한다.
- 프론트 Login은 이전 /api/users/login 주소와 Cookies.get('token')을 사용한다. Refresh Token은 HttpOnly 쿠키이므로 JS에서 읽지 않는다.
- 여러 화면에 운영 API 주소가 하드코딩되어 있다.

### ✅ 할 일
- [ ] 실제 서버·프록시 매핑을 확인해 로그인/재발급/로그아웃 경로를 일치시킨다.
- [ ] 응답 JSON의 accessToken을 처리하고 공통 API 호출에서 Bearer 헤더를 추가한다.
- [ ] Access Token 저장 방식은 새로고침 복원 및 XSS 영향을 검토해 명시한다. localStorage 사용을 전제로 고정하지 않는다.
- [ ] Refresh 쿠키가 필요한 요청에 credentials를 적용하고 동시 401 재발급을 하나로 합친다.
- [ ] 재시도는 한 번으로 제한하고 Refresh 실패 시 로그인 상태를 정리한다.
- [ ] API 기본 URL을 환경 설정으로 분리해 로컬 테스트가 운영 API에 요청하지 않게 한다.

### 🧪 검증 및 완료 기준
- [ ] 로그인→사용자 조회→Access 만료→재발급→로그아웃 흐름을 네트워크 대체 테스트로 확인한다.
- [ ] 동시 401에서 재발급이 중복되지 않고 무한 재시도가 발생하지 않는다.
- [ ] 새로고침 복원과 관리자/일반 사용자 화면 분기를 확인한다.

### 📎 코드 근거
- [Login.jsx:L37](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Frontend/src/pages/Login.jsx#L37)
- [App.js:L45](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Frontend/src/App.js#L45)
- [LoginFilter.java:L88](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/filter/LoginFilter.java#L88)

### 참고
기존 #4를 중복 생성하지 않고 갱신한다. 기존 담당자 및 상태는 유지한다.

구현·검증은 이 이슈의 후속 작업이며, 이슈 등록만으로 수정 완료를 의미하지 않는다.
