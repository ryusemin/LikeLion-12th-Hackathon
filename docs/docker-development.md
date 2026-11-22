# Docker로 로컬 개발하기

프론트·백엔드·MySQL·Redis를 루트 `compose.yaml`로 함께 관리한다. 로컬 개발 전용이며, 로그인 연동이나 실제 메일·AI·결제 설정은 이번 구성에 포함하지 않는다.

## 처음 실행

Docker Desktop을 실행하고 아래 명령을 저장소 루트의 PowerShell에서 실행한다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/init-docker-env.ps1
docker compose up -d --build --wait --wait-timeout 240
```

macOS에서는 저장소 루트의 터미널(zsh)에서 실행한다. 초기화 스크립트만 다르고 Compose 명령은 같다.

```bash
bash scripts/init-docker-env.sh
docker compose up -d --build --wait --wait-timeout 240
```

첫 빌드는 Linux용 Node·JDK 이미지와 의존성을 내려받으며, 백엔드 테스트 및 프론트 lint·테스트도 실행한다. 루트 `.env`는 초기화 스크립트가 DB 비밀번호와 JWT 키를 난수로 생성하는 개인 파일이며 Git에서 제외된다. 기존 파일이 있으면 유지한다. `docker compose config`는 비밀값을 펼칠 수 있으므로 확인할 때는 `docker compose config --quiet`를 사용한다.

## 접속 주소

| 서비스 | PC에서 접근 | 컨테이너 내부 연결 |
| --- | --- | --- |
| 프론트 | http://127.0.0.1:3000 | frontend:3000 |
| 백엔드 Swagger | http://127.0.0.1:8080/docs | backend:8080 |
| MySQL | 127.0.0.1:3307, DB `nimn_docker`, 사용자 `nimn` | mysql:3306 |
| Redis | 127.0.0.1:6380 | redis:6379 |

기존 Windows MySQL(3306) 및 이전 Redis(6379)와 별개다. 기존 데이터를 옮기거나 삭제하지 않는다. 브라우저는 Compose 서비스명을 해석하지 못하므로 프론트의 API 주소는 `127.0.0.1:8080`을 유지한다. 백엔드에서만 DB·Redis 서비스명을 사용한다.

프론트·백엔드의 기존 로컬 프로세스가 3000·8080을 사용 중이면 해당 프로젝트 프로세스를 먼저 종료한다. 관련 없는 프로세스나 Windows MySQL 서비스를 중지하지 않는다.

## 변경 적용과 종료

```powershell
# 실행 상태와 로그
docker compose ps
docker compose logs --tail 50 backend frontend

# 프론트 src/public 변경은 개발 서버가 감지한다.
# 프론트 패키지·환경변수 또는 백엔드 코드를 바꾸면 재빌드한다.
docker compose up -d --build

# 백엔드만 재빌드
docker compose up -d --build backend

# 중지 / 다시 시작
docker compose stop
docker compose start

# 컨테이너·네트워크 정리. 데이터 볼륨은 유지한다.
docker compose down
```

MySQL과 Redis는 별도 이름 있는 볼륨에 데이터를 보관한다. `down -v`는 데이터를 삭제하므로 일반 종료 명령에 사용하지 않는다. 이미 초기화한 MySQL 볼륨이 있으면 `.env` 비밀번호만 바꿔도 DB 계정 비밀번호가 변경되지는 않는다.

프론트의 `src/public`만 읽기 전용으로 마운트하며, Windows의 `node_modules`는 사용하지 않는다. 배포용 프론트 빌드와 기존 EC2 배포 워크플로는 변경하지 않는다. 백엔드 healthcheck는 포트 기동 검사이고, 실제 명세·Redis 응답은 별도로 검증한다.

Docker 프론트는 `Frontend/.env.development`를 Compose의 `env_file`로 전달한다. 호스트 실행에서 사용하는 `.env.development.local` 등의 개인 파일은 컨테이너에 자동 적용되지 않는다. Docker 주소를 바꾸려면 해당 개발 env를 수정하고 `docker compose up -d --build frontend`로 반영한다.
