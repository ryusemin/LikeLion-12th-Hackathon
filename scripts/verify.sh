#!/usr/bin/env bash
# macOS·Linux용 검증 진입점. Windows의 verify.ps1과 같은 대상·순서로 검사한다.
# 사용법: bash scripts/verify.sh [all|backend|frontend|browser|runtime]
set -euo pipefail

target="$(printf '%s' "${1:-all}" | tr '[:upper:]' '[:lower:]')"
case "$target" in
    all | backend | frontend | browser | runtime) ;;
    *) echo "알 수 없는 대상입니다: $1 (all, backend, frontend, browser, runtime)" >&2; exit 2 ;;
esac

script_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(dirname "$script_root")"
tools_root="$repository_root/.local-tools"

fail() {
    echo "$1" >&2
    exit 1
}

# 하위 디렉터리 중 이름 패턴에 맞는 첫 항목을 찾는다. 없으면 빈 문자열이다.
first_directory() {
    for candidate in "$tools_root"/$1; do
        if [ -d "$candidate" ]; then
            printf '%s' "$candidate"
            return
        fi
    done
}

if [ "$target" = all ]; then
    bash "$script_root/tests/verify-runtime.test.sh"
fi
if [ "$target" = runtime ]; then
    bash "$script_root/verify-runtime.sh"
fi

if [ "$target" = all ] || [ "$target" = backend ]; then
    local_jdk="$(first_directory 'jdk-17*')"
    if [ -n "$local_jdk" ]; then
        # macOS용 JDK 압축본은 Contents/Home 아래에 실제 JDK가 있다.
        if [ -d "$local_jdk/Contents/Home" ]; then local_jdk="$local_jdk/Contents/Home"; fi
        export JAVA_HOME="$local_jdk"
    elif [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
        # macOS에 JDK 17이 설치돼 있으면 기본 JDK가 다른 버전이어도 17을 선택한다.
        JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
        if [ -n "$JAVA_HOME" ]; then export JAVA_HOME; else unset JAVA_HOME; fi
    fi
    compiler='javac'
    if [ -n "${JAVA_HOME:-}" ]; then compiler="$JAVA_HOME/bin/javac"; fi
    compiler_version="$("$compiler" -version 2>&1 || true)"
    case "$compiler_version" in
        'javac 17.'*) ;;
        *) fail 'JDK 17 is required. Set JAVA_HOME or prepare the JDK described in docs/development.md.' ;;
    esac
    export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$tools_root/gradle}"

    (cd "$repository_root/Backend" && ./gradlew test bootJar --no-daemon --console=plain) \
        || fail 'Backend verification failed.'
fi

if [ "$target" = all ] || [ "$target" = frontend ] || [ "$target" = browser ]; then
    local_node="$(first_directory 'node-v24.*-darwin-*')"
    if [ -z "$local_node" ]; then local_node="$(first_directory 'node-v24.*-linux-*')"; fi
    if [ -n "$local_node" ]; then export PATH="$local_node/bin:$PATH"; fi

    expected_major="$(tr -d '[:space:]' < "$repository_root/Frontend/.nvmrc")"
    node_version="$(node --version 2>/dev/null || true)"
    case "$node_version" in
        "v$expected_major".*) ;;
        *) fail "Node $expected_major is required (current: ${node_version:-none}). Select the runtime described in docs/development.md." ;;
    esac
    npm_version="$(npm --version 2>/dev/null || true)"
    case "$npm_version" in
        11.*) ;;
        *) fail 'npm 11 is required.' ;;
    esac
    [ -e "$repository_root/Frontend/node_modules/.bin/react-scripts" ] \
        || fail 'Frontend dependencies are missing. Run npm ci in Frontend first.'
    export npm_config_cache="${npm_config_cache:-$tools_root/npm-cache}"

    cd "$repository_root/Frontend"
    if [ "$target" = all ] || [ "$target" = frontend ]; then
        CI=true npm run lint || fail 'Frontend lint failed.'
        CI=true npm run test:ci || fail 'Frontend tests failed.'
        # 앞 단계에서 ESLint 오류를 검사하므로 빌드에서는 중복 검사를 생략한다.
        CI=true DISABLE_ESLINT_PLUGIN=true npm run build || fail 'Frontend build failed.'
    fi
    if [ "$target" = all ] || [ "$target" = browser ]; then
        # 로컬·CI 모두 현재 소스로 전용 검증 서버를 새로 띄운다. CI 값은 호출한 환경을 그대로 쓴다.
        npm run test:e2e || fail 'Browser tests failed.'
    fi
fi
