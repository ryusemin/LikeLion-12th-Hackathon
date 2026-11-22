#!/usr/bin/env bash
# 실제 Docker나 HTTP에 접근하지 않고 비정상 상태에서의 실패를 확인한다.
set -euo pipefail

runtime_script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/verify-runtime.sh"
fake_bin="$(mktemp -d)"
trap 'rm -rf "$fake_bin"' EXIT

# 가짜 docker: compose config·ps만 허용하고, 그 뒤의 명령이 호출되면 실패를 기록한다.
cat > "$fake_bin/docker" <<'EOF'
#!/usr/bin/env bash
if [ "$1 $2" = 'compose config' ]; then exit 0; fi
if [ "$1 $2" != 'compose ps' ]; then
    echo "상태 확인 이후 명령을 실행하면 안 됩니다: $*" >> "$FAKE_CALL_LOG"
    exit 1
fi
for service in frontend backend mysql redis; do
    [ "$service" = "$MISSING_SERVICE" ] && continue
    health=healthy
    [ "$service" = "$UNHEALTHY_SERVICE" ] && health=unhealthy
    printf '%s\trunning\t%s\n' "$service" "$health"
done
EOF
# 가짜 curl: 상태 검사에서 실패해야 하므로 HTTP 검사까지 오면 실패를 기록한다.
cat > "$fake_bin/curl" <<'EOF'
#!/usr/bin/env bash
echo "HTTP 검사 전에 실패해야 합니다: $*" >> "$FAKE_CALL_LOG"
exit 1
EOF
chmod +x "$fake_bin/docker" "$fake_bin/curl"

for case_name in missing unhealthy; do
    call_log="$fake_bin/$case_name.log"
    : > "$call_log"
    missing=''
    unhealthy=''
    if [ "$case_name" = missing ]; then missing=backend; else unhealthy=backend; fi

    if output="$(PATH="$fake_bin:$PATH" FAKE_CALL_LOG="$call_log" MISSING_SERVICE="$missing" \
        UNHEALTHY_SERVICE="$unhealthy" bash "$runtime_script" 2>&1)"; then
        echo "예상한 실패가 아닙니다 ($case_name): 검사가 통과했습니다." >&2
        exit 1
    fi
    case "$output" in
        *healthy*backend*) ;;
        *) echo "예상한 실패가 아닙니다 ($case_name): $output" >&2; exit 1 ;;
    esac
    if [ -s "$call_log" ]; then
        echo "예상한 실패가 아닙니다 ($case_name): $(cat "$call_log")" >&2
        exit 1
    fi
    echo "통과: $case_name 상태에서 HTTP 검사 전에 실패"
done
