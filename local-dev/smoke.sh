#!/usr/bin/env bash
# Sierra-Lima Checkpoint #1 smoke test -- Phase 11 §5.
# Exercises the happy-path demo against a running Restaurant + Menu
# stack and exits 0 only if every step passes.
#
# Steps (each one hits the real HTTP surface):
#   1. Mint a dev RestaurantOwner token (HS256, matches JwtDevMint.java).
#   2. Mint a dev Customer token (used for availability and batch validate).
#   3. POST /restaurants             -> create a fresh restaurant, capture id.
#   4. POST /restaurants/{rid}/menu-items -> add a menu item, capture id.
#   5. PATCH /restaurants/{rid}/status    -> toggle to closed, then back to open.
#   6. GET  /restaurants/{rid}/availability -> assert acceptsOrders=true.
#   7. POST /menu-items/validate     -> assert allValid=true for the new line.
#
# Requirements: bash, curl, openssl (HMAC + base64), python3 (JSON parse).
# Run the stack first (services/local-dev/docker-compose.yml must be up).

set -euo pipefail

RESTAURANT_BASE="${RESTAURANT_BASE:-http://localhost:8081}"
MENU_BASE="${MENU_BASE:-http://localhost:8082}"
JWT_SECRET="${JWT_SECRET:-dGVzdC1zZWNyZXQtZm9yLWRldi1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZC0xMjM0NTY=}"
JWT_ISSUER="${JWT_ISSUER:-quickbite-user-service}"

CUSTOMER_USER_ID="00000000-0000-0000-0000-0000000000c1"
OWNER_USER_ID="00000000-0000-0000-0000-000000000099"

red()   { printf '\033[31m%s\033[0m\n' "$*" >&2; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
info()  { printf '  %s\n' "$*"; }

fail() {
    red "FAIL: $*"
    exit 1
}

require_tool() {
    command -v "$1" >/dev/null 2>&1 || fail "missing required tool: $1"
}

require_tool curl
require_tool openssl
require_tool python3

b64url_encode_str() {
    printf '%s' "$1" | openssl base64 -A | tr '+/' '-_' | tr -d '='
}

b64url_encode_bin() {
    openssl base64 -A | tr '+/' '-_' | tr -d '='
}

mint_token() {
    local role="$1" user_id="$2" subject="$3"
    local iat exp
    iat=$(date +%s)
    exp=$((iat + 3600))
    local header='{"alg":"HS256","typ":"JWT"}'
    local payload
    payload=$(printf '{"iss":"%s","sub":"%s","userId":"%s","role":"%s","tokenType":"USER","iat":%s,"exp":%s}' \
        "$JWT_ISSUER" "$subject" "$user_id" "$role" "$iat" "$exp")
    local h_b64 p_b64 signing_input key_hex sig
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

assert_status() {
    local expected="$1" actual="$2" label="$3"
    [[ "$actual" == "$expected" ]] || fail "$label: expected HTTP $expected, got $actual"
}

json_path() {
    python3 -c 'import json,sys;d=json.load(sys.stdin);p=sys.argv[1].split(".");x=d
for k in p:
    x = x[int(k)] if k.lstrip("-").isdigit() else x[k]
print(x)' "$1"
}

echo "== Sierra-Lima smoke test =="
echo "Restaurant Service : ${RESTAURANT_BASE}"
echo "Menu Service       : ${MENU_BASE}"
echo

# -------- Step 1-2: mint tokens --------
OWNER_TOKEN=$(mint_token "RestaurantOwner" "${OWNER_USER_ID}" "smoke-owner")
CUSTOMER_TOKEN=$(mint_token "Customer" "${CUSTOMER_USER_ID}" "smoke-customer")
[[ ${#OWNER_TOKEN}    -gt 100 ]] || fail "owner token looks too short"
[[ ${#CUSTOMER_TOKEN} -gt 100 ]] || fail "customer token looks too short"
info "owner token minted ($((${#OWNER_TOKEN} / 10 * 10))+ chars)"
info "customer token minted"

# -------- Step 3: create restaurant --------
UNIQUE_SUFFIX=$(date +%s)
CREATE_RESP=$(mktemp)
CREATE_CODE=$(curl -sS -o "$CREATE_RESP" -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -d "{\"name\":\"Smoke Cafe ${UNIQUE_SUFFIX}\",\"address\":\"Main 1\",\"city\":\"Tartu\",\"latitude\":58.38,\"longitude\":26.72,\"operatingHours\":\"00:00-23:59\"}" \
    "${RESTAURANT_BASE}/restaurants")
assert_status 201 "$CREATE_CODE" "POST /restaurants"
RESTAURANT_ID=$(json_path restaurantId < "$CREATE_RESP")
info "restaurant created: ${RESTAURANT_ID}"

# -------- Step 4: add menu item --------
ITEM_RESP=$(mktemp)
ITEM_CODE=$(curl -sS -o "$ITEM_RESP" -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -d '{"name":"Smoke Burger","description":"Smoke test fixture.","priceAmount":"9.99","priceCurrency":"EUR","category":"Main","isAvailable":true}' \
    "${MENU_BASE}/restaurants/${RESTAURANT_ID}/menu-items")
assert_status 201 "$ITEM_CODE" "POST /restaurants/{rid}/menu-items"
MENU_ITEM_ID=$(json_path menuItemId < "$ITEM_RESP")
info "menu item created: ${MENU_ITEM_ID}"

# -------- Step 5: toggle status closed -> open --------
CLOSE_CODE=$(curl -sS -o /dev/null -w '%{http_code}' \
    -X PATCH \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -d '{"isOpen": false}' \
    "${RESTAURANT_BASE}/restaurants/${RESTAURANT_ID}/status")
assert_status 200 "$CLOSE_CODE" "PATCH status false"
OPEN_RESP=$(mktemp)
OPEN_CODE=$(curl -sS -o "$OPEN_RESP" -w '%{http_code}' \
    -X PATCH \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -d '{"isOpen": true}' \
    "${RESTAURANT_BASE}/restaurants/${RESTAURANT_ID}/status")
assert_status 200 "$OPEN_CODE" "PATCH status true"
IS_OPEN=$(json_path isOpen < "$OPEN_RESP")
[[ "$IS_OPEN" == "True" ]] || fail "expected isOpen=True after toggle, got: ${IS_OPEN}"
info "status toggled closed -> open"

# -------- Step 6: availability --------
AVAIL_RESP=$(mktemp)
AVAIL_CODE=$(curl -sS -o "$AVAIL_RESP" -w '%{http_code}' \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
    "${RESTAURANT_BASE}/restaurants/${RESTAURANT_ID}/availability")
assert_status 200 "$AVAIL_CODE" "GET availability"
ACCEPTS=$(json_path acceptsOrders < "$AVAIL_RESP")
[[ "$ACCEPTS" == "True" ]] || fail "expected acceptsOrders=True, got: ${ACCEPTS}"
info "availability acceptsOrders=true"

# -------- Step 7: batch validate --------
VALIDATE_RESP=$(mktemp)
VALIDATE_CODE=$(curl -sS -o "$VALIDATE_RESP" -w '%{http_code}' \
    -X POST \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
    -d "{\"items\":[{\"menuItemId\":\"${MENU_ITEM_ID}\",\"quantity\":2}]}" \
    "${MENU_BASE}/menu-items/validate")
assert_status 200 "$VALIDATE_CODE" "POST /menu-items/validate"
ALL_VALID=$(json_path allValid < "$VALIDATE_RESP")
[[ "$ALL_VALID" == "True" ]] || fail "expected allValid=True, got: ${ALL_VALID}"
info "batch validate allValid=true"

echo
green "OK -- Sierra-Lima smoke test passed."
exit 0
