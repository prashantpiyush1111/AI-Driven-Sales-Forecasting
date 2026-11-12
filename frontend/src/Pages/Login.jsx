import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";

import useAuth from "../auth/useAuth";
import { getErrorMessage } from "../services/apiError";
import "../styles/Auth.css";

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).+$/;

// Mirrors the backend rules so users get instant feedback (the server still validates).
function validate(mode, { username, email, password }) {
  if (!username.trim()) return "Username is required";
  if (!password) return "Password is required";

  if (mode === "register") {
    if (username.trim().length < 3 || username.trim().length > 50) {
      return "Username must be between 3 and 50 characters";
    }
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
      return "Enter a valid email address";
    }
    if (password.length < 8 || password.length > 72) {
      return "Password must be between 8 and 72 characters";
    }
    if (!PASSWORD_RULE.test(password)) {
      return "Password must contain at least one letter and one digit";
    }
  }
  return null;
}

function Login() {
  const { isAuthenticated, loading, login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = location.state?.from?.pathname || "/";

  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ username: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (!loading && isAuthenticated) {
    return <Navigate to={redirectTo} replace />;
  }

  const isRegister = mode === "register";

  const handleChange = (event) => {
    setForm((prev) => ({ ...prev, [event.target.name]: event.target.value }));
  };

  const switchMode = () => {
    setMode(isRegister ? "login" : "register");
    setError("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const problem = validate(mode, form);
    if (problem) {
      setError(problem);
      return;
    }

    setSubmitting(true);
    setError("");
    try {
      if (isRegister) {
        await register(form.username.trim(), form.email.trim(), form.password);
      } else {
        await login(form.username.trim(), form.password);
      }
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, isRegister ? "Registration failed" : "Login failed"));
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit} noValidate>
        <div className="auth-logo">SalesAI</div>
        <h1>{isRegister ? "Create account" : "Sign in"}</h1>
        <p className="auth-subtitle">
          {isRegister
            ? "Register to view sales analytics and forecasts."
            : "Welcome back. Sign in to continue."}
        </p>

        {error && (
          <div className="auth-error" role="alert">
            {error}
          </div>
        )}

        <label htmlFor="username">Username</label>
        <input
          id="username"
          name="username"
          type="text"
          autoComplete="username"
          value={form.username}
          onChange={handleChange}
          disabled={submitting}
          autoFocus
        />

        {isRegister && (
          <>
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={handleChange}
              disabled={submitting}
            />
          </>
        )}

        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete={isRegister ? "new-password" : "current-password"}
          value={form.password}
          onChange={handleChange}
          disabled={submitting}
        />
        {isRegister && (
          <small className="auth-hint">At least 8 characters, with a letter and a digit.</small>
        )}

        <button type="submit" className="auth-submit" disabled={submitting}>
          {submitting ? "Please wait..." : isRegister ? "Create account" : "Sign in"}
        </button>

        <p className="auth-switch">
          {isRegister ? "Already have an account?" : "New here?"}{" "}
          <button type="button" className="auth-link" onClick={switchMode} disabled={submitting}>
            {isRegister ? "Sign in" : "Create an account"}
          </button>
        </p>
      </form>
    </div>
  );
}

export default Login;
