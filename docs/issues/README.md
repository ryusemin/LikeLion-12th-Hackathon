# 리팩토링 이슈 등록 초안

대상 저장소: [ryusemin/LikeLion-12th-Hackathon](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues)

검토 기준: `feat/global-exception-handler` / `4c4f5ad366b4dd27338e027e1e3e9c6bf56c2814`

## 등록 상태

- GitHub 연결 권한 반영 후 등록 완료: 새 이슈 11건(#6~#16), 기존 #4 갱신 1건.
- 기존 #4의 담당자·상태는 유지했다. 기존 응답 헤더 수신 설명은 현재 JSON 응답 계약으로 바로잡았다.
- 기존 #1~#3은 과거 작업 이력으로 두며 이번 초안으로 수정하지 않는다.
- 재시도 전 저장소 이슈를 다시 확인해 중복 생성을 방지한다.
- 각 파일의 H1은 이슈 제목이다. ‘대상’ 안내 다음의 ‘작업 내용’부터 이슈 본문으로 사용한다.

## 이슈 목록

| 순서 | 작업 | 등록 방식 |
| --- | --- | --- |
| 1 | [[Fix] 비밀번호 재설정 및 회원가입의 이메일 인증 증명 검증](01-reset.md) | 신규 |
| 2 | [[Fix] 공개·회원·관리자 API의 Spring Security 접근 권한 분리](02-access.md) | 신규 |
| 3 | [[Fix] 회원·주문·결제 API의 데이터 소유자 검증](03-owner.md) | 신규 |
| 4 | [[Refactor] 프론트엔드 인증을 JSON Access Token·Bearer 요청 방식으로 연동](04-front.md) | #4 갱신 |
| 5 | [[Fix] 로그인 필터와 이메일 인증번호의 요청 간 공유 상태 제거](05-shared.md) | 신규 |
| 6 | [[Fix] WebSocket CONNECT 인증 및 사용자 구독 권한 검증](06-ws.md) | 신규 |
| 7 | [[Fix] Refresh Token Rotation의 원자성 및 세션 무효화 정책 보완](07-refresh.md) | 신규 |
| 8 | [[Fix] 결제의 주문 연결·중복 요청·취소 실패 정합성 보완](08-payment.md) | 신규 |
| 9 | [[Fix] JWT 필터 실패 응답·토큰 로그·운영 Refresh 쿠키 보완](09-jwt.md) | 신규 |
| 10 | [[Fix] 서버 재시작 후 식사 알림 대상 누락 방지](10-notification.md) | 신규 |
| 11 | [[Fix] AI 식단 응답 형식 검증 및 외부 호출·DB 저장 경계 분리](11-ai.md) | 신규 |
| 12 | [[Refactor] 주간 식단 생성의 반복 조회 제거 및 빈 음식 목록 처리](12-weekly.md) | 신규 |

## 권장 진행 순서

1. 이메일 인증 증명 및 비밀번호 재설정 보호
2. 접근 권한·소유자 검증과 프론트 인증 연동을 함께 적용
3. 요청 공유 상태 제거, JWT 오류 처리, Refresh 원자성
4. WebSocket 인증 및 구독 권한
5. 결제 정합성
6. 예약 알림, AI 응답 검증, 식단 반복 조회

32개 lint 경고는 관련 기능 수정 때 함께 정리한다. 이번 목록은 코드 검토의 12개 주요 항목을 이슈로 정리한 것이며 실제 취약점 재현이나 수정 완료를 뜻하지 않는다.

## GitHub 등록 결과

- [#6 [Fix] 비밀번호 재설정 및 회원가입의 이메일 인증 증명 검증](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/6)
- [#7 [Fix] 공개·회원·관리자 API의 Spring Security 접근 권한 분리](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/7)
- [#8 [Fix] 회원·주문·결제 API의 데이터 소유자 검증](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/8)
- [#4 [Refactor] 프론트엔드 인증을 JSON Access Token·Bearer 요청 방식으로 연동](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/4)
- [#9 [Fix] 로그인 필터와 이메일 인증번호의 요청 간 공유 상태 제거](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/9)
- [#10 [Fix] WebSocket CONNECT 인증 및 사용자 구독 권한 검증](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/10)
- [#11 [Fix] Refresh Token Rotation의 원자성 및 세션 무효화 정책 보완](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/11)
- [#12 [Fix] 결제의 주문 연결·중복 요청·취소 실패 정합성 보완](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/12)
- [#13 [Fix] JWT 필터 실패 응답·토큰 로그·운영 Refresh 쿠키 보완](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/13)
- [#14 [Fix] 서버 재시작 후 식사 알림 대상 누락 방지](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/14)
- [#15 [Fix] AI 식단 응답 형식 검증 및 외부 호출·DB 저장 경계 분리](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/15)
- [#16 [Refactor] 주간 식단 생성의 반복 조회 제거 및 빈 음식 목록 처리](https://github.com/ryusemin/LikeLion-12th-Hackathon/issues/16)
