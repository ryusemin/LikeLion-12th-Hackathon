# [Fix] 결제의 주문 연결·중복 요청·취소 실패 정합성 보완

대상: 새 이슈

### 📋 작업 내용
PG 결제와 내부 주문을 정확히 연결하고 중복 요청 및 부분 실패에도 결제 상태를 일치시킨다.

**우선순위:** P1 · 결제 데이터 정합성
**검토 기준:** `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

### 🔍 현재 상황
- PG 상태와 금액만 확인하고 PG 주문 식별자와 내부 주문 ID를 대조하지 않는다.
- existsByUid 후 저장은 동시 요청에 안전하지 않으며 uid에 유일 제약이 없다.
- 금액 불일치에서 주문 취소→PG 취소→RuntimeException이 실행되어 DB 취소는 롤백되고 외부 취소만 남을 수 있다.
- 리뷰 생성 실패는 로그만 남기고 결제 흐름이 계속되어 후속 데이터 누락이 남을 수 있다.

### ✅ 할 일
- [ ] 주문 소유자와 PG 주문번호·금액·통화를 서버 주문과 대조한다.
- [ ] 결제 UID 유일 제약과 주문 상태 전이 보호를 적용한다. 기존 중복 데이터 점검 후 마이그레이션한다.
- [ ] 동일 결제의 재요청에 기존 결과를 반환하는 멱등성을 구현한다.
- [ ] PG 호출과 DB 상태 변경 경계를 나누고 취소 실패/DB 실패를 기록·재시도할 복구 흐름을 만든다.
- [ ] 리뷰 생성 실패의 재처리 또는 명시적 실패 정책을 정한다.

### 🧪 검증 및 완료 기준
- [ ] 금액이 같은 다른 주문의 결제 UID를 연결할 수 없다.
- [ ] 동일 UID의 동시·반복 요청으로 결제와 후속 리뷰가 중복 생성되지 않는다.
- [ ] PG 취소 성공+DB 실패, PG 타임아웃, 리뷰 실패 상태를 대체 클라이언트로 검증한다.

### 📎 코드 근거
- [PaymentServiceImpl.java:L50](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/service/payment/PaymentServiceImpl.java#L50)
- [Payment.java:L25](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/entity/payment/Payment.java#L25)

### 참고
실제 결제·취소 API 호출 없이 테스트한다. DB 트랜잭션이 외부 PG 호출까지 롤백하지 않는다.

구현·검증은 이 이슈의 후속 작업이며, 이슈 등록만으로 수정 완료를 의미하지 않는다.
