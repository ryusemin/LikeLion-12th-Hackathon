# [Fix] JWT 필터 실패 응답·토큰 로그·운영 Refresh 쿠키 보완

대상: 새 이슈

### 📋 작업 내용
토큰 검증 실패를 안전하고 일관된 응답으로 처리하고 운영 쿠키 정책을 적용한다.

**우선순위:** P1 · 인증 보안 및 오류 계약
**검토 기준:** `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

### 🔍 현재 상황
- Authorization 헤더 전체를 System.out으로 출력한다.
- 만료 및 파싱 실패가 일반 예외로 발생하며 MVC ControllerAdvice가 필터 예외까지 처리하지 않는다.
- 로그인·재발급 Refresh 쿠키의 Secure가 false이고 SameSite 정책이 명시되지 않았다.
- JWT 검증에서 Access/Refresh 용도를 구분하지 않는다.

### ✅ 할 일
- [ ] Authorization 및 토큰 원문 로그를 제거한다.
- [ ] 서명·만료·용도를 한 번의 파싱 결과로 검증하고 인증 실패를 표준 401 JSON으로 반환한다.
- [ ] 필터 공개 경로 목록과 Security 설정의 실제 API 경로를 일치시킨다.
- [ ] 로그인·재발급·삭제 쿠키의 Path/Domain을 맞추고 환경에 따라 Secure·SameSite를 설정한다.
- [ ] Refresh 쿠키 기반 API의 CSRF 방어와 허용 Origin/credentials를 함께 검토·적용한다.

### 🧪 검증 및 완료 기준
- [ ] 만료·변조·잘못된 용도·잘못된 형식 토큰이 일관된 401을 반환하고 토큰을 로그에 남기지 않는다.
- [ ] 운영 설정의 Set-Cookie 속성과 동일 범위의 쿠키 삭제를 확인한다.
- [ ] MVC 단독 테스트 외에 실제 필터 체인을 포함해 확인한다.

### 📎 코드 근거
- [JwtAuthorizationFilter.java:L38](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/filter/JwtAuthorizationFilter.java#L38)
- [LoginFilter.java:L98](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/filter/LoginFilter.java#L98)
- [AuthController.java:L89](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/controller/auth/AuthController.java#L89)

### 참고
정적 코드 검토에 근거한 개선 이슈다. 운영 환경에서 재현한 결과가 아니며, 구현 시 관련 호출부와 테스트를 함께 확인한다.

구현·검증은 이 이슈의 후속 작업이며, 이슈 등록만으로 수정 완료를 의미하지 않는다.
