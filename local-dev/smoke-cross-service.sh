#!/usr/bin/env bash
# Phase 16 cross-service smoke -- Sten.
#
# Runs the full W1 -> W2 -> W3 trace to the extent teammate services are
# available. Sten-owned steps (W1 hops 4/5 against Restaurant + Menu)
# MUST pass; teammate-owned steps (Order, Payment, Delivery, Notification)
# are probed opportunistically and report SKIP when their URLs are absent.
#
# Phase 16 also adds the optional `menu.item-availability-changed` event
# producer (see decision 0040). This script toggles availability to
# exercise the emit-point and greps the dedicated `menu-events` logger out
# of the menu-service container logs if docker is reachable.
#
# Exit codes:
#   0  -- Sten-owned steps passed; teammate steps passed or skipped.
#   1  -- A Sten-owned step failed.
#   2  -- A teammate-owned step was configured (URL set) but failed.
#
# Env vars (all optional):
#   RESTAURANT_BASE   default http://localhost:8081
#   MENU_BASE         default http://localhost:8082
#   JWT_SECRET        default matches services/local-dev/.env.example
#   JWT_ISSUER        default quickbite-user-service
#   GATEWAY_BASE      default http://localhost:8080  (Alfa-Kilo gateway; optional probe)
#   USER_BASE         default unset                  (Alfa-Kilo User Service; optional probe)
#   ORDER_BASE        default unset                  (Alfa-Kilo Order Service; optional probe)
#   PAYMENT_BASE      default unset                  (Elephant-Yankee; optional probe)
#   DELIVERY_BASE     default unset                  (Elephant-Yankee; optional probe)
#   NOTIFICATION_BASE default unset                  (Mike-Alfa; optional probe)
#   MENU_CONTAINER    default quickbite-menu-service (docker container name for log capture)
#   EVIDENCE_DIR      default services/local-dev/evidence
#
# Usage:
#   services/local-dev/smoke-cross-service.sh
#
# Requirements: bash, curl, openssl, python3. docker CLI if you want the
# `menu-events` log line captured automatically.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

RESTAURANT_BASE="${RESTAURANT_BASE:-http://localhost:8081}"
MENU_BASE="${MENU_BASE:-http://localhost:8082}"
JWT_SECRET="${JWT_SECRET:-dGVzdC1zZWNyZXQtZm9yLWRldi1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZC0xMjM0NTY=}"
JWT_ISSUER="${JWT_ISSUER:-quickbite-user-service}"
GATEWAY_BASE="${GATEWAY_BASE:-http://localhost:8080}"
USER_BASE="${USER_BASE:-}"
ORDER_BASE="${ORDER_BASE:-}"
PAYMENT_BASE="${PAYMENT_BASE:-}"
DELIVERY_BASE="${DELIVERY_BASE:-}"
NOTIFICATION_BASE="${NOTIFICATION_BASE:-}"
MENU_CONTAINER="${MENU_CONTAINER:-quickbite-menu-service}"
EVIDENCE_DIR="${EVIDENCE_DIR:-${SCRIPT_DIR}/evidence}"

CUSTOMER_USER_ID="00000000-0000-0000-0000-0000000000c1"
OWNER_USER_ID="00000000-0000-0000-0000-000000000099"

SIERRA_FAILED=0
TEAMMATE_FAILED=0

red()    { printf '\033[31m%s\033[0m\n' "$*" >&2; }
green()  { printf '\033[32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[33m%s\033[0m\n' "$*"; }
info()   { printf '  %s\n' "$*"; }

fail_sierra() {
    red "SIERRA-FAIL: $*"
    SIERRA_FAILED=1
}

fail_team() {
    red "TEAMMATE-FAIL: $*"
    TEAMMATE_FAILED=1
}

skip() {
    yellow "SKIP: $*"
}

require_tool() {
    command -v "$1" >/dev/null 2>&1 || { red "missing required tool: $1"; exit 1; }
}

require_tool curl
require_tool openssl
require_tool python3
# docker is optional; absence degrades to "no log capture"
HAS_DOCKER=0
if command -v docker >/dev/null 2>&1; then HAS_DOCKER=1; fi

mkdir -p "$EVIDENCE_DIR"
RUN_TAG="$(date -u +%Y%m%dT%H%M%SZ)"
TRACE_FILE="${EVIDENCE_DIR}/cross-service-smoke_${RUN_TAG}.log"
touch "$TRACE_FILE" || {
    red "cannot write trace file: $TRACE_FILE"
    exit 1
}

trace() {
    local ts
    ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf '%s %s\n' "$ts" "$*" | tee -a "$TRACE_FILE"
}

trace "== Phase 16 cross-service smoke =="
trace "Restaurant        : ${RESTAURANT_BASE}"
trace "Menu              : ${MENU_BASE}"
trace "Gateway (probe)   : ${GATEWAY_BASE}"
trace "User (probe)      : ${USER_BASE:-<unset>}"
trace "Order (probe)     : ${ORDER_BASE:-<unset>}"
trace "Payment (probe)   : ${PAYMENT_BASE:-<unset>}"
trace "Delivery (probe)  : ${DELIVERY_BASE:-<unset>}"
trace "Notification      : ${NOTIFICATION_BASE:-<unset>}"
trace "Docker available  : $([[ $HAS_DOCKER -eq 1 ]] && echo yes || echo no)"
trace "Trace file        : ${TRACE_FILE}"

# -------- JWT helpers (same HS256 format as smoke.sh) --------

b64url_encode_str() {
    printf '%s' "$1" | openssl base64 -A | tr '+/' '-_' | tr -d '='
}
b64url_encode_bin() {
    openssl base64 -A | tr '+/' '-_' | tr -d '='
}

mint_token() {
    local role="$1" user_id="$2" subject="$3"
    local iat exp header payload h_b64 p_b64 signing_input key_hex sig
    iat=$(date +%s)
    exp=$((iat + 3600))
    header='{"alg":"HS256","typ":"JWT"}'
    payload=$(printf '{"iss":"%s","sub":"%s","userId":"%s","role":"%s","tokenType":"USER","iat":%s,"exp":%s}' \
        "$JWT_ISSUER" "$subject" "$user_id" "$role" "$iat" "$exp")
    h_b64=$(b64url_encode_str "$header")
    p_b64=$(b64url_encode_str "$payload")
    signing_input="${h_b64}.${p_b64}"
    key_hex=$(printf '%s' "$JWT_SECRET" | openssl base64 -d -A \
        | od -An -tx1 | tr -d ' \n')
    sig=$(printf '%s' "$signing_input" \
        | openssl dgst -sha256 -mac HMAC -macopt "hexkey:${key_hex}" -binary \
        | b64url_encode_bin)
    printf '%s.%s' "$signing_input" "$sig"
}

json_path() {
    python3 -c 'import json,sys
d = json.load(sys.stdin)
for k in sys.argv[1].split("."):
    d = d[int(k)] if k.lstrip("-").isdigit() else d[k]
print(d)' "$1"
}

http_status() {
    curl -sS -o /dev/null -w '%{http_code}' "$@" 2>/dev/null || true
}

probe_reachable() {
    local url="$1"
    local code
    code="$(http_status --max-time 2 "$url" || true)"
    if [[ -z "$code" || "$code" == "000" ]]; then
        return 1
    fi
    return 0
}

# -------- Step 1: mint tokens --------

trace ""
trace "[STEP 1] Mint dev tokens (owner + customer)"
OWNER_TOKEN=$(mint_token "RestaurantOwner" "${OWNER_USER_ID}" "cross-smoke-owner")
CUSTOMER_TOKEN=$(mint_token "Customer" "${CUSTOMER_USER_ID}" "cross-smoke-customer")
if [[ ${#OWNER_TOKEN} -lt 100 || ${#CUSTOMER_TOKEN} -lt 100 ]]; then
    fail_sierra "token mint produced suspiciously short output"
fi
info "owner token minted"
info "customer token minted"

# -------- Step 2: Sten W1 portion --------

trace ""
trace "[STEP 2] Sten W1 hops (create restaurant, add menu item, availability, validate)"

UNIQUE_SUFFIX=$(date +%s)
CREATE_RESP=$(mktemp)
CREATE_CODE=$(curl -sS -o "$CREATE_RESP" -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -d "{\"name\":\"X-Smoke Cafe ${UNIQUE_SUFFIX}\",\"address\":\"Main 1\",\"city\":\"Tartu\",\"latitude\":58.38,\"longitude\":26.72,\"operatingHours\":\"00:00-23:59\"}" \
    "${RESTAURANT_BASE}/restaurants" 2>/dev/null || true)
if [[ "$CREATE_CODE" != "201" ]]; then
    fail_sierra "POST /restaurants expected 201, got ${CREATE_CODE}"
    trace "body: $(cat "$CREATE_RESP")"
else
    RESTAURANT_ID=$(json_path restaurantId < "$CREATE_RESP")
    info "restaurant created: ${RESTAURANT_ID}"
    trace "restaurantId=${RESTAURANT_ID}"
fi

MENU_ITEM_ID=""
if [[ -n "${RESTAURANT_ID:-}" ]]; then
    ITEM_RESP=$(mktemp)
    ITEM_CODE=$(curl -sS -o "$ITEM_RESP" -w '%{http_code}' \
        -H 'Content-Type: application/json' \
        -H "Authorization: Bearer ${OWNER_TOKEN}" \
        -d '{"name":"X-Smoke Burger","description":"Phase 16 cross-service fixture.","priceAmount":"9.99","priceCurrency":"EUR","category":"Main","isAvailable":true}' \
        "${MENU_BASE}/restaurants/${RESTAURANT_ID}/menu-items" 2>/dev/null || true)
    if [[ "$ITEM_CODE" != "201" ]]; then
        fail_sierra "POST /restaurants/{rid}/menu-items expected 201, got ${ITEM_CODE}"
        trace "body: $(cat "$ITEM_RESP")"
    else
        MENU_ITEM_ID=$(json_path menuItemId < "$ITEM_RESP")
        info "menu item created: ${MENU_ITEM_ID}"
        trace "menuItemId=${MENU_ITEM_ID}"
    fi

    # New restaurants are created with isOpen=false per F.3; flip to open
    # so the availability probe below reflects a "ready to serve" state,
    # matching the equivalent step in smoke.sh.
    OPEN_CODE=$(curl -sS -o /dev/null -w '%{http_code}' \
        -X PATCH -H 'Content-Type: application/json' \
        -H "Authorization: Bearer ${OWNER_TOKEN}" \
        -d '{"isOpen": true}' \
        "${RESTAURANT_BASE}/restaurants/${RESTAURANT_ID}/status" 2>/dev/null || true)
    if [[ "$OPEN_CODE" != "200" ]]; then
        fail_sierra "PATCH /restaurants/{rid}/status expected 200, got ${OPEN_CODE}"
    else
        info "restaurant toggled open"
    fi

    AVAIL_RESP=$(mktemp)
    AVAIL_CODE=$(curl -sS -o "$AVAIL_RESP" -w '%{http_code}' \
        -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
        "${RESTAURANT_BASE}/restaurants/${RESTAURANT_ID}/availability" 2>/dev/null || true)
    if [[ "$AVAIL_CODE" != "200" ]]; then
        fail_sierra "GET availability expected 200, got ${AVAIL_CODE}"
    else
        ACCEPTS=$(json_path acceptsOrders < "$AVAIL_RESP")
        if [[ "$ACCEPTS" != "True" && "$ACCEPTS" != "true" ]]; then
            fail_sierra "GET availability acceptsOrders was ${ACCEPTS}, expected true"
        else
            info "availability acceptsOrders=true"
        fi
    fi
fi

if [[ -n "${MENU_ITEM_ID}" ]]; then
    VALIDATE_RESP=$(mktemp)
    VALIDATE_CODE=$(curl -sS -o "$VALIDATE_RESP" -w '%{http_code}' \
        -X POST \
        -H 'Content-Type: application/json' \
        -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
        -d "{\"items\":[{\"menuItemId\":\"${MENU_ITEM_ID}\",\"quantity\":2}]}" \
        "${MENU_BASE}/menu-items/validate" 2>/dev/null || true)
    if [[ "$VALIDATE_CODE" != "200" ]]; then
        fail_sierra "POST /menu-items/validate expected 200, got ${VALIDATE_CODE}"
    else
        ALL_VALID=$(json_path allValid < "$VALIDATE_RESP")
        if [[ "$ALL_VALID" != "True" && "$ALL_VALID" != "true" ]]; then
            fail_sierra "allValid was ${ALL_VALID}, expected true"
        else
            info "batch validate allValid=true"
        fi
    fi
fi

# -------- Step 3: menu.item-availability-changed exercise (Phase 16 stretch) --------

trace ""
trace "[STEP 3] Toggle menu item availability to exercise the menu-events publisher"

if [[ -n "${MENU_ITEM_ID}" ]]; then
    TOGGLE_OFF=$(curl -sS -o /dev/null -w '%{http_code}' \
        -X PUT -H 'Content-Type: application/json' \
        -H "Authorization: Bearer ${OWNER_TOKEN}" \
        -d '{"name":"X-Smoke Burger","description":"Phase 16 cross-service fixture.","priceAmount":"9.99","priceCurrency":"EUR","category":"Main","isAvailable":false}' \
        "${MENU_BASE}/menu-items/${MENU_ITEM_ID}" 2>/dev/null || true)
    if [[ "$TOGGLE_OFF" != "200" ]]; then
        fail_sierra "PUT availability=false expected 200, got ${TOGGLE_OFF}"
    else
        info "availability toggled true -> false"
    fi

    TOGGLE_ON=$(curl -sS -o /dev/null -w '%{http_code}' \
        -X PUT -H 'Content-Type: application/json' \
        -H "Authorization: Bearer ${OWNER_TOKEN}" \
        -d '{"name":"X-Smoke Burger","description":"Phase 16 cross-service fixture.","priceAmount":"9.99","priceCurrency":"EUR","category":"Main","isAvailable":true}' \
        "${MENU_BASE}/menu-items/${MENU_ITEM_ID}" 2>/dev/null || true)
    if [[ "$TOGGLE_ON" != "200" ]]; then
        fail_sierra "PUT availability=true expected 200, got ${TOGGLE_ON}"
    else
        info "availability toggled false -> true"
    fi

    # Capture the menu-events log lines if docker is reachable.
    if [[ $HAS_DOCKER -eq 1 ]]; then
        if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${MENU_CONTAINER}$"; then
            LOG_SLICE="${EVIDENCE_DIR}/menu-events_${RUN_TAG}.log"
            if docker logs --since 2m "$MENU_CONTAINER" 2>&1 | grep "topic=menu-events" > "$LOG_SLICE"; then
                COUNT=$(wc -l < "$LOG_SLICE" | tr -d ' ')
                if [[ "$COUNT" -ge 2 ]]; then
                    info "captured ${COUNT} menu-events log lines -> ${LOG_SLICE}"
                    trace "menu-events evidence: ${LOG_SLICE} (${COUNT} lines)"
                else
                    fail_sierra "expected >=2 menu-events log lines after toggles, found ${COUNT}"
                fi
            else
                fail_sierra "failed to read docker logs from ${MENU_CONTAINER}"
            fi
        else
            skip "docker container '${MENU_CONTAINER}' not running -- menu-events log capture unavailable"
            trace "menu-events log capture skipped (container not running)"
        fi
    else
        skip "docker CLI unavailable -- menu-events log capture by-hand only"
        trace "menu-events log capture skipped (no docker CLI)"
    fi
else
    skip "no menu item created -- skipping availability toggle exercise"
fi

# -------- Step 4: teammate gateway probes --------

trace ""
trace "[STEP 4] Teammate service probes (optional)"

if probe_reachable "${GATEWAY_BASE}/actuator/health"; then
    info "gateway reachable at ${GATEWAY_BASE}"
    trace "gateway: reachable"
else
    skip "gateway not reachable at ${GATEWAY_BASE} -- W1 through-gateway probe skipped"
    trace "gateway: not reachable"
fi

probe_optional() {
    local name="$1" base="$2" path="$3"
    if [[ -z "$base" ]]; then
        skip "${name}: URL unset -- skipping probe"
        trace "${name}: unset"
        return
    fi
    if probe_reachable "${base}${path}"; then
        info "${name} reachable at ${base}"
        trace "${name}: reachable"
    else
        fail_team "${name} URL set (${base}) but ${base}${path} not reachable"
        trace "${name}: unreachable"
    fi
}

probe_optional "User Service"         "$USER_BASE"         "/actuator/health"
probe_optional "Order Service"        "$ORDER_BASE"        "/actuator/health"
probe_optional "Payment Service"      "$PAYMENT_BASE"      "/actuator/health"
probe_optional "Delivery Service"     "$DELIVERY_BASE"     "/actuator/health"
probe_optional "Notification Service" "$NOTIFICATION_BASE" "/actuator/health"

# -------- Step 5: summary + exit --------

trace ""
trace "[SUMMARY]"
trace "Sten failures = ${SIERRA_FAILED}"
trace "teammate failures    = ${TEAMMATE_FAILED}"
trace "trace file           = ${TRACE_FILE}"

echo
if [[ $SIERRA_FAILED -ne 0 ]]; then
    red "FAIL -- Sten-owned steps failed. See ${TRACE_FILE}."
    exit 1
fi
if [[ $TEAMMATE_FAILED -ne 0 ]]; then
    yellow "WARN -- Sten OK; teammate probe(s) failed. See ${TRACE_FILE}."
    exit 2
fi
green "OK -- cross-service smoke passed (teammate-owned parts SKIPped if unconfigured)."
exit 0
