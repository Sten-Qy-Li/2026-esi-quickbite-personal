# 0033 -- Inter-Service Bearer-Token Propagation Lock

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `5bd6f45`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 9 Task 6; extends
  [`0010-auth-contract.md`](./0010-auth-contract.md) §4.

## Context

`0010` §4 specified that inter-service REST calls in W1 carry a bearer
token, and listed two permitted modes -- token relay and service token.
Phase 9 Task 6 asks the team to confirm which mode is used by default
on each hop, what services accept, and how Sierra-Lima reacts when a
hop arrives without a token or with a malformed one.

Without this lock, Order Service might forward the end-user token in
normal cases but silently drop it in compensating or scheduled paths
-- which would trigger `401` from Sierra-Lima and mask as a business
failure in the end-to-end test suite. This document pins the posture
end-to-end so the failure mode cannot happen.

As with the other Phase 9 decisions, the plan's unilateral-fallback
clause is invoked. Alfa-Kilo or Elephant-Yankee can supersede the
advisory rows with a later-numbered decision.

## Decision

### 1. Default posture per W1 hop

Reusing the hop numbering from [`0030`](./0030-w1-synchronous-contract-lock.md) §1.

| Hop | Caller | Callee | Default mode | Fallback mode |
|-----|--------|--------|--------------|----------------|
| 1-2 | Client / Gateway | Order | End-user token (issued by User Service) | -- |
| 3 | Order | User | **Relay** end-user token | Service token with `serviceName:"order-service"` |
| 4 | Order | Restaurant | **Relay** end-user token | Service token |
| 5 | Order | Menu | **Relay** end-user token | Service token |
| 7 | Order | Payment | **Relay** end-user token | Service token |
| 8 | Order | Delivery | **Relay** end-user token | Service token |
| 8c | Order | Payment (refund) | **Service token** (no end-user present during compensation) | -- |

Default = **Token relay** for every user-triggered W1 hop. Order
Service forwards the exact `Authorization` header it received. It does
not mint a service token on the hot path -- simpler, testable, and
matches how `0010` §4 bullets 1-3 describe the flow.

Service tokens are used on compensations and future scheduled work
where no end-user context is available.

### 2. Gateway rules

Copied from [`0010`](./0010-auth-contract.md) §4 and pinned here:

- Gateway validates the token on every request it forwards.
- Gateway forwards the `Authorization` header as-is; it does **not**
  strip, rewrite, or re-mint.
- Gateway MAY add `X-User-Id` for downstream convenience. Sierra-Lima's
  services do not trust `X-User-Id`; the JWT is the identity source.
- Gateway never mints tokens. Only User Service does.

### 3. Service-token minting (caller-side)

When a caller must produce a service token:

- Signed with the same `JWT_SECRET` HS256 key every service already
  shares.
- `iss` set to `"quickbite-<caller-service>-service"` (e.g.
  `"quickbite-order-service"`).
- `tokenType` claim is `"SERVICE"`.
- `serviceName` claim is the short caller id (e.g. `"order-service"`).
- TTL **≤ 5 minutes** (`exp - iat <= 300`). Minted just-in-time; do not
  cache across calls.
- `role` claim is **omitted** on service tokens. The receiving service
  switches on `tokenType` before role checks.

Order Service's caller helper in the team repo is the recommended
location for a single `mintServiceToken()` utility so that the mint
logic is not duplicated per hop.

### 4. Service-token acceptance (Sierra-Lima services)

Sierra-Lima's `JwtAuthFilter` already populates the `SecurityContext`
from a valid token (see `security/JwtAuthFilter.java` on both
services). The route-protection matrix in `0010` §8 allows any valid
token on the two internal endpoints -- including a `SERVICE` token.

The effective acceptance rule Sierra-Lima enforces:

| Endpoint | End-user `Customer` token | `RestaurantOwner` token | `Admin` token | `Driver` token | `SERVICE` token | No token / bad token |
|----------|---------------------------|-------------------------|---------------|----------------|------------------|----------------------|
| `GET /restaurants/{id}/availability` | accept | accept | accept | accept | accept | `401` |
| `POST /menu-items/validate` | accept | accept | accept | accept | accept | `401` |
| Any mutation endpoint on either service | `403` unless `Admin` | accept (ownership) | accept | `403` | **`403`** | `401` |

Note the bolded cell: `SERVICE` tokens do not automatically unlock
mutation endpoints. A service wanting to mutate must either carry a
user token with the right role or hold an `Admin`-equivalent service
token (not minted in the A3 scope). This keeps the blast radius of a
leaked service token small.

### 5. TTL and clock skew

- Service tokens: `exp - iat <= 300 s` (5 min).
- End-user tokens: dev default `exp - iat == 3600 s` (1 h) per
  `0010` §7. Production is a User Service concern.
- All services honour a **±30 s clock skew** when validating `exp` /
  `nbf` / `iat`. Sierra-Lima's `JwtAuthFilter` uses
  `JwtParserBuilder.setAllowedClockSkewSeconds(30)`.
- Services MUST NOT accept tokens with `nbf > now + 30 s` or
  `exp < now - 30 s`.

### 6. Rejection posture

Every Sierra-Lima endpoint returns `401` on any of these, with the
canonical error envelope per `0020` §7:

- `Authorization` header absent and the route requires a token.
- Header present but not `Bearer <token>`.
- Signature verification failure (wrong / missing secret).
- `iss` claim mismatch.
- `exp` in the past (outside skew).
- `nbf` or `iat` in the future (outside skew).
- Unparseable token.

A `403` is returned only after a successful parse when the role check
fails (per `0010` §8 / §4 here).

### 7. Token logging

- Sierra-Lima's services MUST NOT log full JWTs.
- They MAY log `userId`, `role`, `tokenType`, `iss` on handled
  requests (INFO) and on `401`/`403` (DEBUG).
- The JWT header / signature portions MUST be redacted from any
  diagnostic dump.

### 8. Postman / dev-tool posture

- The Phase 7 Postman collection already mints fresh HS256 tokens in
  a pre-request script; that path is unchanged.
- The local dev `JwtDevMint` utility supports both `USER` and
  `SERVICE` tokens. Phase 10 integration tests MAY use it to mint a
  `SERVICE` token when stubbing Order -> Sierra-Lima calls.
- No secrets, `.env.local`, or service-token payloads land in git.
  `.gitignore` protects `.env.local`; `0005-non-goals.md` N8 protects
  the shared secret policy.

## Team alignment note

Authoritative on the Sierra-Lima side: §4, §5, §6, §7, §8. These are
how Restaurant Service and Menu Service already behave at `5bd6f45`;
no code change is needed in Phase 9 beyond the DTO reshape tracked by
`0030`.

Advisory: §1 (other callsigns' posture), §3 (Order Service's mint
helper). Alfa-Kilo's or Elephant-Yankee's later decision supersedes.

## Consequences

- Order Service in the team repo writes a `WebClient` with a
  `ClientRequestFilter` that copies the inbound `Authorization` header
  on every outbound call. A second, compensation-only filter mints a
  service token when no inbound header is available.
- Phase 10 integration tests exercise both modes (user-token relay
  and service-token mint) against Sierra-Lima's real endpoints.
- Phase 15 authorisation hardening adds ownership-on-SERVICE-token
  rules only if a concrete need appears; today no such rule is
  needed because §4's mutation-endpoint cell already returns `403`.
- No change to the JWT shape itself (still per `0010` §7).

## Supersedes

None directly. Pins and extends `0010-auth-contract.md` §4 with the
per-hop default mode, the service-token TTL ceiling, the rejection
posture, and the logging rules.
