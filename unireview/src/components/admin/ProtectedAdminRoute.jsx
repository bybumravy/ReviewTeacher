import { Navigate, Outlet } from 'react-router-dom';
import { getAdminToken } from '../../api/adminApi';

export default function ProtectedAdminRoute() {
  const token = getAdminToken();
  if (!token) {
    return <Navigate to="/admin/login" replace />;
  }
  return <Outlet />;
}
