# Docker Development Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 데이터를 보존하며 네 서비스를 Docker Compose로 실행한다.

**Architecture:** 루트 Compose와 서비스별 개발 Dockerfile을 추가한다. 프론트 소스는 마운트하고 백엔드는 검증 후 JAR 이미지로 실행한다. MySQL·Redis는 전용 볼륨을 사용한다.

**Tech Stack:** Docker Compose, Java 17, Node 24, MySQL 8.4, Redis 7.4.

**Spec:** `docs/docker-development.md`

## 제약과 완료 기준

- 기존 Windows MySQL과 이전 컨테이너 데이터는 변경하지 않는다.
- 실제 비밀값과 개인 env는 이미지·Git·출력에서 제외한다.
- 인증 연동·운영 배포·별도 검증 Skill 구성은 제외한다.
- 네 서비스 healthy, 프론트·Swagger·명세 HTTP 200, Redis PONG을 확인한다.
- 로컬 main 통합은 승인 범위이며 원격 push는 하지 않는다.

## 작업

- [x] 1. Compose·Dockerfile·컨텍스트 제외 규칙·env 초기화를 추가하고 설정을 검증한다.
- [x] 2. Linux 이미지 빌드의 기존 테스트와 실제 네 서비스 기동을 검증한다.
- [x] 3. 데이터 보존·프론트 변경 감지·재시작과 안내 문서를 확인한다.

## 실행 기록

- 결정: Docker DB는 3307, Redis는 6380을 사용해 기존 서비스와 충돌을 피한다.
- 결정: 프론트 src/public만 마운트해 Linux 의존성과 Windows 의존성을 섞지 않는다.
- 검증: env 초기화 재실행 시 기존 파일 해시가 유지되고, 서로 다른 32바이트 난수 3개가 생성됐다. 실제 값은 출력하지 않았다.
- 검증: Linux 이미지 빌드의 백엔드 테스트 14개(실패·스킵 0개)와 bootJar, 프론트 lint/테스트 6개가 통과했다. 기존 lint 경고 32개는 유지한다.
- 검증: 네 서비스 healthy, 프론트·Swagger·명세 HTTP 200, Redis PONG, MySQL의 nimn 계정 접속을 확인했다.
- 검증: Redis 임시 키가 서비스 재시작 후 유지됐고 키를 정리했다. 프론트 임시 주석의 자동 재컴파일과 원복도 확인했다.
- 리뷰: 별도 읽기 전용 리뷰에서 차단 사항 없음. Docker에서 호스트용 개인 env가 자동 적용되지 않는 점을 문서에 명시했다.
