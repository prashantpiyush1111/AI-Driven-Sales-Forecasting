// Persists the login session (JWT + basic user info) in localStorage.
// All access is wrapped in try/catch: storage can be unavailable (private mode, quota).

const STORAGE_KEY = "salesai.session";

export const AUTH_LOGOUT_EVENT = "salesai:auth-logout";

export function loadSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    return session && session.token ? session : null;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } catch {
    // Ignore: the user simply has to log in again after a reload.
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore.
  }
}

export function getToken() {
  return loadSession()?.token ?? null;
}
