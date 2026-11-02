# [Fix] 공개·회원·관리자 API의 Spring Security 접근 권한 분리

대상: 새 이슈

### 📋 작업 내용
공개 API만 허용하고 나머지 API에 인증 및 역할 기반 권한 검사를 적용한다.

**우선순위:** P0 · API 접근 통제
**검토 기준:** `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

### 🔍 현재 상황
- dev/prod 모두 anyRequest().permitAll()로 모든 요청을 허용한다.
- JWT 필터는 토큰이 없으면 다음 필터로 넘긴다. 필터 존재만으로 비로그인 요청을 차단하지 않는다.
- 관리자 화면 숨김은 서버 API의 권한 검사를 대신하지 못한다.

### ✅ 할 일
- [ ] Controller 매핑과 배포의 /api 전달 방식을 확인하고 공개/회원/관리자 API 목록을 정리한다.
- [ ] 공개 API만 permitAll, 일반 API는 authenticated, 관리자 API는 관리자 역할로 제한한다.
- [ ] 미인증 401 및 권한 부족 403 응답 형식을 통일한다.
- [ ] 권한 제한 적용 전 프론트 인증 연동 #4와 변경 순서를 맞춘다.

### 🧪 검증 및 완료 기준
- [ ] 실제 SecurityFilterChain을 포함한 테스트로 공개 API 접근과 회원 API의 비로그인 401을 확인한다.
- [ ] 일반 사용자 관리자 접근 403, 관리자 정상 접근을 확인한다.

### 📎 코드 근거
- [SecurityConfigProd.java:L75](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/config/security/SecurityConfigProd.java#L75)
- [SecurityConfigDev.java:L78](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/config/security/SecurityConfigDev.java#L78)

### 참고
정적 코드 검토에 근거한 개선 이슈다. 운영 환경에서 재현한 결과가 아니며, 구현 시 관련 호출부와 테스트를 함께 확인한다.

구현·검증은 이 이슈의 후속 작업이며, 이슈 등록만으로 수정 완료를 의미하지 않는다.
