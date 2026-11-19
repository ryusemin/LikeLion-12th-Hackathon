# [Fix] 로그인 필터와 이메일 인증번호의 요청 간 공유 상태 제거

대상: 새 이슈

### 📋 작업 내용
로그인 정보와 인증번호를 요청별로 분리하여 다른 요청의 값이 섞이지 않도록 한다.

**우선순위:** P0 · 동시 요청 안전성
**검토 기준:** `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

### 🔍 현재 상황
- 싱글톤 LoginFilter의 loginRequest 필드를 각 요청이 덮어쓴다. 이메일과 비밀번호를 읽는 사이 다른 요청이 필드를 변경할 수 있다.
- MailService의 static 인증번호를 메일 작성과 반환에 공유하여 동시 발송에서 보낸 코드와 저장된 코드가 달라질 수 있다.

### ✅ 할 일
- [ ] LoginFilter에서 JSON을 지역변수 DTO로 파싱하고 같은 지역변수로 인증 토큰을 생성한다.
- [ ] 인증번호를 지역변수로 생성해 메일 내용과 반환값에 동일하게 사용한다.
- [ ] 인증번호는 SecureRandom을 사용하고 이메일·목적별 저장과 연결한다.

### 🧪 검증 및 완료 기준
- [ ] 두 로그인 요청을 의도적으로 겹쳐도 각 요청의 이메일과 비밀번호가 함께 전달된다.
- [ ] 동시 이메일 발송 시 각 수신자에게 보낸 번호와 해당 사용자에게 저장된 번호가 같다.

### 📎 코드 근거
- [LoginFilter.java:L33](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/filter/LoginFilter.java#L33)
- [MailService.java:L19](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/service/email/MailService.java#L19)

### 참고
정적 코드 검토에 근거한 개선 이슈다. 운영 환경에서 재현한 결과가 아니며, 구현 시 관련 호출부와 테스트를 함께 확인한다.

구현·검증은 이 이슈의 후속 작업이며, 이슈 등록만으로 수정 완료를 의미하지 않는다.
