# [Fix] 비밀번호 재설정 및 회원가입의 이메일 인증 증명 검증

대상: 새 이슈

### 📋 작업 내용
이메일 인증 성공을 서버가 증명하고, 비밀번호 재설정과 회원가입에서 해당 증명을 검증하도록 연결한다.

**우선순위:** P0 · 계정 보호 / 가장 먼저
**검토 기준:** `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

### 🔍 현재 상황
- 비밀번호 재설정 서비스는 이메일과 새 비밀번호만으로 변경하며 기존 비밀번호 또는 이메일 인증 증명을 확인하지 않는다.
- 이메일 인증 성공 시 세션의 인증번호를 제거하지만, 가입·재설정 API가 소비할 검증 완료 증명이 없다.
- 현재 전체 요청 허용 설정과 결합하면 인증 화면을 거치지 않는 요청으로 계정이 변경될 수 있다.

### ✅ 할 일
- [ ] 이메일·목적(가입/재설정)·만료에 연결된 일회용 인증 증명을 발급한다.
- [ ] 가입/재설정 API에서 증명을 검증하고 성공 시 원자적으로 소비한다.
- [ ] 인증번호 발송·확인에 시도 횟수 제한과 발급 시점 기준 만료를 적용한다.
- [ ] 비밀번호 재설정 후 기존 Refresh Token 폐기 정책을 적용한다.

### 🧪 검증 및 완료 기준
- [ ] 미인증·다른 이메일·다른 목적·만료·재사용 증명으로 변경할 수 없다.
- [ ] 유효한 증명은 한 번만 사용할 수 있고 새 비밀번호로 로그인할 수 있다.

### 📎 코드 근거
- [UserServiceImpl.java:L145](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/service/auth/UserServiceImpl.java#L145)
- [EmailController.java:L71](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/controller/email/EmailController.java#L71)

### 참고
정적 코드 검토 결과이며 운영 요청으로 재현하지 않았다. 서버 검증을 먼저 추가하고 프론트의 증명 전달도 함께 맞춘다.

구현·검증은 이 이슈의 후속 작업이며, 이슈 등록만으로 수정 완료를 의미하지 않는다.
