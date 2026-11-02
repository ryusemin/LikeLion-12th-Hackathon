# [Fix] 회원·주문·결제 API의 데이터 소유자 검증

대상: 새 이슈

### 📋 작업 내용
요청에 담긴 이메일 대신 인증된 사용자를 기준으로 데이터를 조회·변경한다.

**우선순위:** P0 · 타인 데이터 접근 방지
**검토 기준:** `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

### 🔍 현재 상황
- 회원 수정은 요청 본문의 이메일로 사용자를 조회한다.
- 결제 조회는 요청 파라미터의 구매자 이메일을 사용하며 조회·삭제에 소유자 검증이 없다.
- 로그인 필수 설정만 추가해도 다른 사용자의 식별자를 전달하는 요청까지 차단되지는 않는다.

### ✅ 할 일
- [ ] 내 정보·내 주문·내 결제의 사용자 식별은 Principal에서 가져온다.
- [ ] 주문·결제·식단·리뷰·알림의 ID 기반 API를 점검하고 소유자 검증을 적용한다.
- [ ] 관리자의 전체 조회/관리 기능은 별도 권한 경로로 구분한다.
- [ ] 타인 접근 시 403/404 중 일관된 정책을 정하고 내부 정보를 노출하지 않는다.

### 🧪 검증 및 완료 기준
- [ ] 사용자 A가 사용자 B의 이메일이나 ID를 넣어도 조회·수정·삭제할 수 없다.
- [ ] 본인 및 허용된 관리자 작업은 정상 처리된다.

### 📎 코드 근거
- [UserServiceImpl.java:L99](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/service/auth/UserServiceImpl.java#L99)
- [PaymentController.java:L79](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/controller/payment/PaymentController.java#L79)
- [OrderServiceImpl.java:L85](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/service/order/OrderServiceImpl.java#L85)

### 참고
정적 코드 검토에 근거한 개선 이슈다. 운영 환경에서 재현한 결과가 아니며, 구현 시 관련 호출부와 테스트를 함께 확인한다.

구현·검증은 이 이슈의 후속 작업이며, 이슈 등록만으로 수정 완료를 의미하지 않는다.
