#!/usr/bin/env bash
#
# Local sandbox lifecycle for the plugin under development.
#
#   sandbox.sh up        Install + start TeamCity, drive its first-boot wizard
#                        non-interactively, mint an admin token, create a sample
#                        project that exercises the runner this template ships.
#                        Idempotent — safe to re-run.
#   sandbox.sh down      Stop server + agent (gradle stopTeamcity).
#   sandbox.sh reset     Down + wipe build/data. The TC home is preserved
#                        because re-extracting it is slow.
#   sandbox.sh log       Tail server log. Pass `agent` for the agent log.
#   sandbox.sh status    Print where the sandbox is: installed? running? authed?
#   sandbox.sh tc <args> Invoke the `teamcity` CLI authenticated against this
#                        sandbox. Requires the CLI on $PATH.
#
# Token + URL are stored in .sandbox/admin.env (gitignored). The script never
# prints the token to stdout.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

URL="http://localhost:8111"
TC_VERSION=$(awk -F= '/^teamcityVersion/ {gsub(/[ \t]/,"",$2); print $2}' gradle.properties)
HOME_DIR="$ROOT/server/build/servers/TeamCity-$TC_VERSION"
DATA_DIR="$ROOT/server/build/data/teamcity"
LOG_DIR="$HOME_DIR/logs"
SBOX_DIR="$ROOT/.sandbox"
TOKEN_FILE="$SBOX_DIR/admin.env"

mkdir -p "$SBOX_DIR"

die() { echo "sandbox: $*" >&2; exit 1; }

cmd="${1:-help}"
shift || true

# ─── helpers ──────────────────────────────────────────────────────────────────

require() {
    command -v "$1" >/dev/null 2>&1 || die "missing dependency: $1"
}

server_responding() {
    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' "$URL/login.html" 2>/dev/null || echo 000)
    [[ "$code" =~ ^(200|302|401|503)$ ]]
}

current_stage() {
    grep -aoE 'Current stage: [^[:cntrl:]]*' "$LOG_DIR/teamcity-server.log" 2>/dev/null \
        | tail -1 | sed 's/^Current stage: //'
}

wait_until() {
    local check="$1" timeout="${2:-180}" elapsed=0
    until eval "$check"; do
        ((elapsed >= timeout)) && die "timed out after ${timeout}s waiting: $check"
        sleep 2
        elapsed=$((elapsed + 2))
    done
}

post_mnt() {
    curl -s -b "$SBOX_DIR/cookies" -c "$SBOX_DIR/cookies" -X POST "$URL/mnt/do/$1" "${@:2}" -o /dev/null -w '%{http_code}'
}

run_wizard() {
    curl -s -c "$SBOX_DIR/cookies" "$URL/mnt/index.html" -o /dev/null
    local stage prev=""
    while true; do
        stage=$(current_stage)
        case "$stage" in
            "$prev"|"")
                sleep 2
                continue
                ;;
            *Confirming*first*start*)
                post_mnt goNewInstallation -d 'restore=false' >/dev/null
                ;;
            *Setting*up*database*connection*|*"database connection"*)
                post_mnt goNewDatabase -d 'dbType=HSQLDB2' >/dev/null
                ;;
            *license*agreement*|*License*Agreement*|*License*agreement*)
                post_mnt acceptLicenseAgreement >/dev/null
                ;;
            *Initializing*|*Creating*new*database*|*Initial*data*)
                ;;
            *System*is*ready*)
                return 0
                ;;
            *startup*error*)
                die "TeamCity reached an error stage: $stage. See $LOG_DIR/teamcity-server.log"
                ;;
            *)
                # Unknown stage — keep waiting; new TC versions may add or rename stages.
                ;;
        esac
        prev=$stage
        sleep 2
    done
}

scrape_super_token() {
    grep -aoE 'Super user authentication token: *[0-9]+' "$LOG_DIR/teamcity-server.log" 2>/dev/null \
        | tail -1 | grep -oE '[0-9]+$'
}

mint_admin_token() {
    local super tok_json token
    super=$(scrape_super_token)
    [ -n "$super" ] || die "couldn't find super-user token in $LOG_DIR/teamcity-server.log"

    # Create admin/admin if not present.
    if [[ "$(curl -s -u ":$super" -o /dev/null -w '%{http_code}' "$URL/app/rest/users/username:admin")" != "200" ]]; then
        curl -fsS -u ":$super" -X POST "$URL/app/rest/users" \
            -H 'Content-Type: application/json' -H 'Accept: application/json' \
            -d '{"username":"admin","password":"admin","roles":{"role":[{"roleId":"SYSTEM_ADMIN","scope":"g"}]}}' \
            >/dev/null
    fi

    # Mint a personal access token (admin minting their own).
    tok_json=$(curl -fsS -u 'admin:admin' -X POST "$URL/app/rest/users/username:admin/tokens/sandbox-$(date +%s)" \
        -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{}')
    token=$(printf '%s' "$tok_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["value"])')
    [ -n "$token" ] || die "failed to mint admin token: $tok_json"

    umask 077
    cat >"$TOKEN_FILE" <<EOF
TEAMCITY_URL=$URL
TEAMCITY_TOKEN=$token
EOF
}

admin_curl() {
    [ -f "$TOKEN_FILE" ] || die "no token; run 'sandbox.sh up' first"
    # shellcheck disable=SC1090
    source "$TOKEN_FILE"
    curl -fsS -u ":$TEAMCITY_TOKEN" "$@"
}

ensure_sample_project() {
    if admin_curl -o /dev/null "$URL/app/rest/projects/id:SandboxDemo" 2>/dev/null; then
        return
    fi
    admin_curl -X POST "$URL/app/rest/projects" \
        -H 'Content-Type: application/json' -H 'Accept: application/json' \
        -d '{"id":"SandboxDemo","name":"SandboxDemo","parentProject":{"locator":"id:_Root"}}' \
        >/dev/null
    admin_curl -X POST "$URL/app/rest/buildTypes" \
        -H 'Content-Type: application/json' -H 'Accept: application/json' \
        -d '{
            "id": "SandboxDemo_Run",
            "name": "Run sample-runner",
            "project": {"id": "SandboxDemo"},
            "settings": {"property": [{"name":"checkoutMode","value":"ON_AGENT"}]},
            "steps": {"step": [{
                "id": "RUNNER_1",
                "name": "Sample",
                "type": "sample-runner",
                "properties": {"property": [
                    {"name":"sample.message","value":"Hello from sandbox.sh"},
                    {"name":"sample.repeat","value":"3"}
                ]}
            }]}
        }' >/dev/null
}

ensure_agent_authorized() {
    local list
    list=$(admin_curl -H 'Accept: application/json' "$URL/app/rest/agents?locator=authorized:any" 2>/dev/null || true)
    printf '%s' "$list" | python3 - <<'PY' >/dev/null 2>&1 || return 0
import json, sys
data = json.loads(sys.stdin.read() or '{"agent":[]}')
unauthorized = [a for a in data.get('agent', []) if not a.get('authorized', True)]
if not unauthorized:
    raise SystemExit(0)
raise SystemExit(1)
PY
    if [[ $? -ne 0 ]]; then
        admin_curl -X PUT "$URL/app/rest/agents/locator:authorized:false/authorized" \
            -H 'Content-Type: text/plain' --data 'true' >/dev/null 2>&1 || true
    fi
}

# ─── commands ─────────────────────────────────────────────────────────────────

cmd_up() {
    require curl
    require python3
    echo "sandbox: building plugin..."
    ./gradlew :server:serverPlugin -q

    echo "sandbox: installing TeamCity (first run only)..."
    ./gradlew :server:installTeamcity -q

    echo "sandbox: starting server + agent..."
    ./gradlew :server:startTeamcity -q

    echo "sandbox: waiting for first-boot wizard..."
    wait_until 'server_responding' 120
    wait_until '[ -f "$LOG_DIR/teamcity-server.log" ]' 60
    run_wizard

    if [ ! -f "$TOKEN_FILE" ]; then
        echo "sandbox: minting admin token..."
        mint_admin_token
    fi

    echo "sandbox: provisioning sample project..."
    ensure_sample_project
    sleep 2
    ensure_agent_authorized

    echo
    echo "sandbox is up:"
    echo "  http://localhost:8111  (admin / admin, or use 'sandbox.sh tc')"
    echo "  build configuration:   http://localhost:8111/buildConfiguration/SandboxDemo_Run"
}

cmd_down() {
    ./gradlew :server:stopTeamcity -q --continue 2>/dev/null || true
    echo "sandbox: stopped"
}

cmd_reset() {
    cmd_down
    rm -rf "$DATA_DIR" "$SBOX_DIR/cookies" "$TOKEN_FILE"
    echo "sandbox: data wiped (TC home preserved)"
}

cmd_log() {
    local target="${1:-server}" file
    case "$target" in
        server) file="$LOG_DIR/teamcity-server.log" ;;
        agent)  file="$HOME_DIR/buildAgent/logs/teamcity-agent.log" ;;
        *)      die "log target must be 'server' or 'agent'" ;;
    esac
    [ -f "$file" ] || die "log file does not exist: $file"
    tail -F "$file"
}

cmd_status() {
    printf '%-20s %s\n' 'TC home:' "$([ -d "$HOME_DIR" ] && echo "$HOME_DIR" || echo '(not installed)')"
    printf '%-20s %s\n' 'TC data:' "$([ -d "$DATA_DIR" ] && echo "$DATA_DIR" || echo '(empty)')"
    printf '%-20s %s\n' 'Server responding:' "$(server_responding && echo yes || echo no)"
    printf '%-20s %s\n' 'Admin token:' "$([ -f "$TOKEN_FILE" ] && echo "$TOKEN_FILE" || echo '(none)')"
    if [ -f "$LOG_DIR/teamcity-server.log" ]; then
        printf '%-20s %s\n' 'Last stage:' "$(current_stage)"
    fi
}

cmd_tc() {
    require teamcity
    [ -f "$TOKEN_FILE" ] || die "no token; run 'sandbox.sh up' first"
    # shellcheck disable=SC1090
    set -a; source "$TOKEN_FILE"; set +a
    teamcity "$@"
}

cmd_help() {
    sed -n '3,21p' "${BASH_SOURCE[0]}" | sed 's/^#\s\?//'
}

case "$cmd" in
    up)        cmd_up      ;;
    down)      cmd_down    ;;
    reset)     cmd_reset   ;;
    log)       cmd_log "$@" ;;
    status)    cmd_status  ;;
    tc)        cmd_tc "$@" ;;
    help|"")   cmd_help    ;;
    *)         die "unknown command: $cmd  (try 'sandbox.sh help')" ;;
esac
