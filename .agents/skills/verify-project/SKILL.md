---
name: verify-project
description: Use when implementing or checking substantive changes in this repository, especially before claiming completion or preparing a commit. Skip for simple lookups.
---

# 프로젝트 변경 검증

저장소 루트를 기준으로 `docs/verification-workflow.md`를 읽고 적용한다. 공통 지침은 루트와 해당 영역의 AGENTS.md를 따른다.

구현 전에 범위·제외 범위·관찰 가능한 완료 기준·확인 수단을 정한다. 큰 작업은 `docs/templates/task-verification.md`로 기록하고, 이미 정한 기준은 다시 만들지 않는다.

변경 영역에 맞춰 `scripts/verify.ps1`(Windows) 또는 `scripts/verify.sh`(macOS)의 Backend·Frontend·Browser·Runtime 대상을 선택한다. All은 코드와 대체 API 기반 화면 검사이며 실제 인증 연동을 증명하지 않는다. 실행 중인 이미지가 현재 변경을 포함하는지도 따로 확인한다.

검사 명령과 실제 결과를 남기고 완료 기준과 비교한다. 실패·미실행·환경 문제를 통과로 표현하지 않는다. 영향이 큰 변경은 독립 리뷰가 가능하면 활용하고, 불가능하면 직접 검토 사실을 남긴다. 리뷰 지적을 수정한 뒤에는 관련 검사를 다시 수행한다.

이 Skill은 사용자 전역 설정 변경이나 운영 API 호출·push·배포 권한을 추가하지 않는다. 단순 문서 수정에 전체 실행 검사를 강요하지 않는다.
