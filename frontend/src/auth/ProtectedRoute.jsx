import { Navigate, useLocation } from "react-router-dom";

import useAuth from "./useAuth";

// Wrap a route's element. Pass role="ADMIN" to restrict it to administrators.
function ProtectedRoute({ children, role }) {
  const { isAuthenticated, user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="auth-loading">Loading...</div>;
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (role && user?.role !== role) {
    return <Navigate to="/" replace />;
  }
  return children;
}

export default ProtectedRoute;
