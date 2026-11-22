#!/usr/bin/env bash
# macOS·Linux용 Docker 실행 상태 검사. Windows의 verify-runtime.ps1과 같은 항목을 확인한다.
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"

fail() {
    echo "$1" >&2
    exit 1
}

docker compose config --quiet || fail 'Docker Compose 설정을 확인해주세요.'
# JSON 파서 없이 읽도록 서비스·상태·health를 탭으로 구분해 받는다.
services="$(docker compose ps --all --format '{{.Service}}	{{.State}}	{{.Health}}')" \
    || fail 'Docker Compose 상태를 조회하지 못했습니다.'

for name in frontend backend mysql redis; do
    instances="$(printf '%s\n' "$services" | awk -F '\t' -v name="$name" '$1 == name' || true)"
    count="$(printf '%s' "$instances" | grep -c . || true)"
    if [ "$count" -ne 1 ] || [ "$instances" != "$name	running	healthy" ]; then
        fail "서비스가 실행 중이거나 healthy 상태가 아닙니다: $name. docs/docker-development.md를 확인해주세요."
    fi
    echo "$name : running / healthy"
done

response_path="$(mktemp)"
trap 'rm -f "$response_path"' EXIT
for url in 'http://127.0.0.1:3000/' 'http://127.0.0.1:8080/swagger-ui/index.html' 'http://127.0.0.1:8080/api-docs'; do
    status="$(curl -sS --max-time 15 --max-redirs 0 -o "$response_path" -w '%{http_code}' "$url" || true)"
    [ "$status" = '200' ] || fail "HTTP 검사 실패: $url"
    case "$url" in
        */api-docs)
            grep -Eq '"openapi"[[:space:]]*:[[:space:]]*"3\.' "$response_path" \
                && grep -Eq '"paths"[[:space:]]*:[[:space:]]*\{[[:space:]]*"' "$response_path" \
                || fail 'OpenAPI 응답 형식이 올바르지 않습니다.'
            ;;
    esac
    echo "HTTP 200 : $url"
done

pong="$(docker compose exec -T redis redis-cli ping || true)"
[ "$(printf '%s' "$pong" | tr -d '\r\n ')" = 'PONG' ] || fail 'Redis PING 검사 실패'
echo 'Redis : PONG'
echo '실행 상태 검사 통과. 현재 소스와 이미지의 일치 및 로그인·업무 기능의 성공을 증명하지는 않습니다.'
