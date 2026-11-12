import { useCallback, useEffect, useMemo, useState } from "react";

import AuthContext from "./AuthContext";
import { AUTH_LOGOUT_EVENT, clearSession, loadSession, saveSession } from "./session";
import * as authService from "../services/authService";

function AuthProvider({ children }) {
  const [session, setSession] = useState(() => loadSession());
  // If a token is stored, verify it once on startup before rendering protected pages.
  const [loading, setLoading] = useState(() => Boolean(loadSession()));

  const logout = useCallback(() => {
    clearSession();
    setSession(null);
  }, []);

  // The axios interceptor fires this when the server rejects our token (expired/invalid).
  useEffect(() => {
    window.addEventListener(AUTH_LOGOUT_EVENT, logout);
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, logout);
  }, [logout]);

  useEffect(() => {
    const stored = loadSession();
    if (!stored) return undefined;

    let cancelled = false;
    authService
      .getProfile()
      .then((profile) => {
        if (cancelled) return;
        const next = { token: stored.token, user: profile };
        saveSession(next);
        setSession(next);
      })
      .catch((error) => {
        // Only an explicit 401 invalidates the session. If the backend is just
        // unreachable, keep the stored session so a reload does not log the user out.
        if (!cancelled && error.response?.status === 401) logout();
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [logout]);

  const startSession = useCallback((data) => {
    const next = {
      token: data.token,
      user: { id: data.userId, username: data.username, role: data.role },
    };
    saveSession(next);
    setSession(next);
    return next.user;
  }, []);

  const login = useCallback(
    async (username, password) => startSession(await authService.login(username, password)),
    [startSession]
  );

  const register = useCallback(
    async (username, email, password) =>
      startSession(await authService.register(username, email, password)),
    [startSession]
  );

  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      isAuthenticated: Boolean(session?.token),
      isAdmin: session?.user?.role === "ADMIN",
      loading,
      login,
      register,
      logout,
    }),
    [session, loading, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthProvider;
