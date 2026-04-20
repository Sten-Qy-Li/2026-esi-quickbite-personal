# 0010 -- Auth and Gateway Contract

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `97f2d2b`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 1, §5.1, and Appendix F.5.

## Context

Sierra-Lima owns Restaurant Service and Menu Service. Starting in
Phase 3 both services need Spring Security configuration that expects
a specific JWT shape, a specific set of protected vs. public routes,
and a specific gateway path prefix. Alfa-Kilo owns the User Service
(token issuer) and the API Gateway.

The master plan §9 Phase 1 "Team dependency" instructs Sierra-Lima to
apply the §5.1 working defaults when Alfa-Kilo is not in the session,
so those defaults are applied here and recorded as the contract -- no
further team ratification required. If Alfa-Kilo later pushes back on
a specific item, that item is superseded by a later-numbered decision;
this document stays as the historical record. Sierra-Lima's code base
is written so that a change to any item below is at most a
security-filter or env-var change -- never a DTO or business-logic
change.

## Decision

### 1. Public routes

Only two endpoints are fully public (no token required). Both are
owned by User Service.

| Method | Path | Purpose | Owner |
|--------|------|---------|-------|
| `POST` | `/api/users` | Self-registration | User Service (Alfa-Kilo) |
| `POST` | `/api/auth/login` | Token issuance | User Service (Alfa-Kilo) |

No Sierra-Lima endpoint is in the fully-public set -- the browse
routes below are public only in the sense that they do not require
a token, but they still run through the gateway and therefore receive
any token the client already holds.

### 2. Default protected-route rule

Every endpoint that is not listed in §1 **requires a valid bearer
token** and, at minimum, the `Customer` role. Stricter role
requirements are applied per-endpoint (see §8).

### 3. Gateway path map

Copied verbatim from the master plan Appendix F.5. Sierra-Lima's
prefixes are highlighted.

| Gateway prefix | Target service | Owner |
|----------------|----------------|-------|
| `/api/auth/**`, `/api/users/**` | User Service | Alfa-Kilo |
| `/api/orders/**` | Order Service | Alfa-Kilo |
| **`/api/restaurants/**`** | **Restaurant Service** | **Sierra-Lima** |
| **`/api/menu-items/**`, `/api/restaurants/*/menu-items/**`** | **Menu Service** | **Sierra-Lima** |
| `/api/payments/**` | Payment Service | Elephant-Yankee |
| `/api/deliveries/**`, `/api/drivers/**` | Delivery Service | Elephant-Yankee |
| `/api/notifications/**` | Notification Service | Mike-Alfa |

The gateway strips the `/api` prefix when forwarding, so inside
Sierra-Lima's services the controllers map to `/restaurants/**` and
`/menu-items/**` -- not `/api/...`.

### 4. Token propagation model

1. **Client → Gateway** carries `Authorization: Bearer <token>`.
2. **Gateway** validates the token (signature, `exp`, `iss`), picks a
   route by prefix, and forwards the request. It propagates the
   `Authorization` header as-is, and optionally adds `X-User-Id`
   (matching the `userId` claim) for convenience.
3. **Each downstream service** validates the token locally using the
   shared HS256 secret (`JWT_SECRET`). No shared session, no gateway
   trust shortcut -- every service enforces auth on its own.
4. **Service-to-service REST calls** (for example Order →
   Restaurant, Order → Menu in W1) also carry a bearer token. Two
   modes are permitted:
   - **Token relay.** The caller forwards the original end-user
     token. Simplest, works for W1's chain-from-a-user trigger.
   - **Service token.** A dedicated token signed with the same
     secret, carrying `tokenType: "SERVICE"` and `serviceName:
     "order-service"`. Used for background jobs or compensating
     actions where no end-user token is available.
5. **Gateway never mints tokens.** Only User Service does. Services
   that need a service token mint it themselves using the shared
   `JWT_SECRET`, with a short TTL (≤ 5 min) and `tokenType: SERVICE`.

A diagram sketch (text form):

```
Client ──HTTPS──▶ Gateway ──HTTP──▶ Order Service
                    │                 │
                    │                 ├──HTTP──▶ Restaurant Service
                    │                 │          (validates JWT locally)
                    │                 │
                    │                 └──HTTP──▶ Menu Service
                    │                            (validates JWT locally)
                    │
                    └ (validates JWT, forwards Authorization + X-User-Id)
```

### 5. Identity context Sierra-Lima services consume

From the validated JWT, Sierra-Lima's services populate
`SecurityContext` with:

| Claim | Type | Used for |
|-------|------|----------|
| `userId` | UUID | Auditor identity, ownership checks (Restaurant.ownerId, MenuItem's owning restaurant), audit log `created_by` / `updated_by` |
| `role` | enum: `Customer` \| `Driver` \| `RestaurantOwner` \| `Admin` | Method-level `@PreAuthorize` checks |
| `tokenType` | enum: `USER` \| `SERVICE` (optional; defaults to `USER`) | Relaxes end-user role checks for internal endpoints (`/validate`, `/availability`) |
| `serviceName` | String (optional, only on `SERVICE` tokens) | Audit / debugging only |

The gateway-forwarded `X-User-Id` header is **not** trusted as the
identity source. It is a convenience only -- the JWT is the source of
truth inside the service.

### 6. Browse-route protection (resolves Q6 from `0004-open-questions.md`)

`GET /restaurants`, `GET /restaurants/{id}`,
`GET /restaurants/{rid}/menu-items`, and `GET /menu-items/{id}` are
**public** (no token required). Rationale: the W1 flow does not browse;
Order Service calls `availability` and `validate` directly. Making
browse public also simplifies the demo screen recording (no login
wall before showing the catalogue) and matches master plan §5.1
default 1.

If the team tightens these to `Customer` later, the change is
localised to `SecurityFilterChain` in each service and needs no DTO
or controller change.

### 7. JWT claims shape

Sierra-Lima's code writes against this exact shape. The dev JWT
signing secret is Base64 HS256 (see
[`0005-non-goals.md`](./0005-non-goals.md) N8).

```json
{
  "iss": "quickbite-user-service",
  "sub": "dev-user-001",
  "userId": "00000000-0000-0000-0000-000000000001",
  "role": "RestaurantOwner",
  "tokenType": "USER",
  "iat": 1746432000,
  "exp": 1746435600
}
```

Notes:

- `sub` is the User Service's external identifier for humans (e.g.
  `"dev-user-001"` or an email). Sierra-Lima does not read `sub`.
- `userId` is the UUID Sierra-Lima uses for ownership checks.
- `exp` - `iat` is 1 hour in dev. Production TTL is a User Service
  concern.
- `tokenType: "SERVICE"` tokens additionally carry
  `"serviceName": "order-service"`.

A dev utility in Phase 3 (`ee.ut.esi.quickbite.restaurant.security.JwtDevMint`)
mints a token matching this shape for local Postman runs, so
Sierra-Lima can exercise the auth path before Alfa-Kilo's User Service
is running.

### 8. Route-protection matrix for Sierra-Lima

Every Sierra-Lima endpoint, with its required auth posture.

**Restaurant Service** (Appendix F.1):

| Method | Path | Required auth |
|--------|------|---------------|
| `POST` | `/restaurants` | `RestaurantOwner` or `Admin` |
| `GET` | `/restaurants/{id}` | Public |
| `PUT` | `/restaurants/{id}` | `RestaurantOwner` (must own this restaurant) or `Admin` |
| `PATCH` | `/restaurants/{id}/status` | `RestaurantOwner` (must own this restaurant) or `Admin` |
| `GET` | `/restaurants` | Public |
| `GET` | `/restaurants/{id}/availability` | Any valid token (`Customer`, `RestaurantOwner`, `Driver`, `Admin`, or `SERVICE`) |

**Menu Service** (Appendix F.2):

| Method | Path | Required auth |
|--------|------|---------------|
| `POST` | `/restaurants/{rid}/menu-items` | `RestaurantOwner` (must own `rid`) or `Admin` |
| `GET` | `/restaurants/{rid}/menu-items` | Public |
| `GET` | `/menu-items/{id}` | Public |
| `PUT` | `/menu-items/{id}` | `RestaurantOwner` (must own the owning restaurant) or `Admin` |
| `DELETE` | `/menu-items/{id}` | `RestaurantOwner` (must own the owning restaurant) or `Admin` |
| `POST` | `/menu-items/validate` | Any valid token (`Customer`, `RestaurantOwner`, `Driver`, `Admin`, or `SERVICE`) |

Ownership is enforced at the service layer by comparing the
authenticated `userId` claim against `Restaurant.ownerId` (Restaurant
Service) or against the owning restaurant's `ownerId` resolved via a
Menu → Restaurant REST call (Menu Service). Cross-service ownership
lookups are cached per-request so that one Menu write triggers at
most one Restaurant call.

## Team alignment note

Items 1, 3, 4, and 7 must match how User Service and Gateway are
actually built; any mismatch surfaces as a supersession. Items 2, 5,
6, and 8 are Sierra-Lima-internal interpretations and need no team
sign-off.

## Consequences

- Phase 3 (Restaurant foundation) and Phase 5 (Menu domain) may assume
  the JWT shape in §7 when configuring `JwtAuthFilter` and
  `SecurityFilterChain`.
- Phase 4 and Phase 6 REST contracts enforce the role matrix in §8.
- Phase 7 (auth hardening) implements the ownership checks described
  in §5 and §8.
- Phase 10 (W1 integration) assumes service-token propagation per §4
  when Order Service calls `availability` and `validate`.
- Q6 in `0004-open-questions.md` is **resolved by this decision**.
  Other open questions remain open.

## Supersedes

None. This is the first auth-contract decision.
