#!/usr/bin/env bash
# macOS·Linux용 루트 .env 생성 스크립트. Windows의 init-docker-env.ps1과 같은 동작이다.
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
environment_path="$repository_root/.env"
template_path="$repository_root/.env.example"
keys="DOCKER_MYSQL_PASSWORD DOCKER_MYSQL_ROOT_PASSWORD DOCKER_JWT_SECRET"

# 기존 개인 설정은 덮어쓰지 않습니다.
if [ -e "$environment_path" ]; then
    echo '기존 루트 .env를 유지합니다.'
    exit 0
fi

for key in $keys; do
    if ! grep -q "^$key=" "$template_path"; then
        echo "예제에 $key 항목이 없습니다." >&2
        exit 1
    fi
done

# 비밀값이 다른 사용자에게 읽히지 않도록 소유자 전용 권한으로 만듭니다.
umask 077
temporary_path="$(mktemp "$repository_root/.env.XXXXXX")"
trap 'rm -f "$temporary_path"' EXIT

# 비밀값을 명령 인자로 넘기지 않도록 셸 안에서 줄 단위로 교체합니다.
while IFS= read -r line || [ -n "$line" ]; do
    key="${line%%=*}"
    case " $keys " in
        *" $key "*)
            if [ "$key" != "$line" ]; then
                # 32바이트 난수를 Base64로 인코딩합니다. JWT 서명 키 길이 조건도 만족합니다.
                printf '%s=%s\n' "$key" "$(openssl rand -base64 32)"
                continue
            fi
            ;;
    esac
    printf '%s\n' "$line"
done < "$template_path" > "$temporary_path"

# 동시에 실행되더라도 기존 파일을 덮어쓰지 않도록 새 이름으로만 연결합니다.
if ! ln "$temporary_path" "$environment_path" 2>/dev/null; then
    echo '기존 루트 .env를 유지합니다.'
    exit 0
fi
echo 'Docker용 루트 .env를 생성했습니다. 비밀값은 출력하지 않습니다.'
