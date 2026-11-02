# [Fix] Refresh Token Rotation의 원자성 및 세션 무효화 정책 보완

대상: 새 이슈

### 📋 작업 내용
재발급·로그아웃에서 현재 세션 토큰만 안전하게 교체하거나 삭제한다.

**우선순위:** P1 · 인증 안정성
**검토 기준:** `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

### 🔍 현재 상황
- Redis 조회·비교·삭제·저장이 개별 작업이어서 동시 재발급 요청 두 건이 모두 검증을 통과할 수 있다.
- 불일치 시 이메일의 현재 토큰 전체를 삭제하고, 로그아웃도 저장된 토큰과 비교하지 않고 이메일 키를 삭제한다.
- JWT에 고유 ID가 없어 동일 클레임·발급 시각으로 같은 토큰이 만들어지는 경우 Rotation의 구분이 약해진다.

### ✅ 할 일
- [ ] Redis Lua 또는 트랜잭션을 사용해 예상 토큰 비교와 교체를 원자적으로 처리한다.
- [ ] 토큰에 고유 ID와 용도 정보를 넣고 재발급 시 새 ID를 발급한다.
- [ ] 이메일당 단일 로그인/여러 기기 로그인 정책과 재사용 탐지 시 무효화 범위를 명시한다.
- [ ] 로그아웃도 현재 세션 토큰을 비교한 뒤 삭제하도록 정책에 맞춘다.
- [ ] 프론트 #4에서 동시 재발급 요청을 합치되 서버 원자성도 별도로 보장한다.

### 🧪 검증 및 완료 기준
- [ ] 실제 로컬/테스트 Redis에서 동시 재발급 시 허용한 정책대로 하나의 유효한 결과만 남는다.
- [ ] 이전 토큰으로 로그아웃하거나 재발급할 때 새 세션 처리 결과가 정책과 일치한다.
- [ ] 연속 발급된 Refresh Token의 고유 ID가 다르다.

### 📎 코드 근거
- [AuthServiceImpl.java:L43](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/service/auth/AuthServiceImpl.java#L43)
- [JWTUtil.java:L70](https://github.com/ryusemin/LikeLion-12th-Hackathon/blob/4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814/Backend/src/main/java/com/demo/nimn/filter/JWTUtil.java#L70)

### 참고
기존 Mockito 재발급 테스트는 Redis의 원자성을 검증하지 않는다.

구현·검증은 이 이슈의 후속 작업이며, 이슈 등록만으로 수정 완료를 의미하지 않는다.
