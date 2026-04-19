/**
 * Token storage helpers. localStorage is the source of truth for the
 * bearer token; everything else (router guard, API client, nav bar)
 * reads through these helpers so swapping storage later touches one
 * file.
 */

const TOKEN_KEY = 'quickbite.jwt';

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch (_e) {
    return null;
  }
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function isAuthenticated() {
  return Boolean(getToken());
}

/**
 * Decode the payload of a JWT without verifying its signature. Used
 * only to surface the user's role / id in the UI; never for trust
 * decisions (the server re-validates on every request).
 */
export function readClaims() {
  const token = getToken();
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = payload + '='.repeat((4 - payload.length % 4) % 4);
    return JSON.parse(atob(padded));
  } catch (_e) {
    return null;
  }
}

export function readRole() {
  const claims = readClaims();
  if (!claims) return null;
  return claims.role || (Array.isArray(claims.roles) ? claims.roles[0] : null);
}

export function readUserId() {
  const claims = readClaims();
  if (!claims) return null;
  return claims.userId || claims.sub || null;
}

export function canManageRestaurants() {
  const role = readRole();
  return role === 'RestaurantOwner' || role === 'Admin';
}
