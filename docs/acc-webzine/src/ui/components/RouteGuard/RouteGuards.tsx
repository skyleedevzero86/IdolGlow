import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';

export function RequireAuth() {
  const { authReady, user } = useAuth();
  const location = useLocation();

  if (!authReady) {
    return null;
  }

  if (!user) {
    const redirect = encodeURIComponent(`${location.pathname}${location.search}`);
    return <Navigate to={`/?login=1&redirect=${redirect}`} replace />;
  }

  return <Outlet />;
}

export function RequireAdmin() {
  const { authReady, user } = useAuth();
  const location = useLocation();

  if (!authReady) {
    return null;
  }

  if (!user) {
    const redirect = encodeURIComponent(`${location.pathname}${location.search}`);
    return <Navigate to={`/?login=1&redirect=${redirect}`} replace />;
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
