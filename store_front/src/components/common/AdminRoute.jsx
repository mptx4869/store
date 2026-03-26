import { useEffect, useRef } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

function normalizeRole(role) {
  if (!role) return '';
  // accept ADMIN, ROLE_ADMIN, "admin", " ADMIN "
  const r = String(role).trim().toUpperCase();
  return r.startsWith('ROLE_') ? r.slice('ROLE_'.length) : r;
}

function isAdminUser(user) {
  const role = normalizeRole(user?.role);
  return role === 'ADMIN';
}

/**
 * AdminRoute
 * - If not logged in: redirect /login (with from) + toast (once)
 * - If logged in but not ADMIN: redirect / + toast (once)
 */
function AdminRoute({ children }) {
  const { isAuthenticated, user, isLoading } = useAuth();
  const toast = useToast();
  const location = useLocation();

  // Dedupe toast by "situation" to avoid spam
  const lastNoticeRef = useRef('');

  useEffect(() => {
    if (isLoading) return;

    // reset dedupe khi đã đủ điều kiện admin
    if (isAuthenticated && isAdminUser(user)) {
      lastNoticeRef.current = '';
      return;
    }

    if (!isAuthenticated) {
      const key = 'NEED_LOGIN_ADMIN';
      if (lastNoticeRef.current !== key) {
        lastNoticeRef.current = key;
        toast.info('Vui lòng đăng nhập bằng tài khoản quản trị để tiếp tục.', {
          title: 'Yêu cầu đăng nhập',
        });
      }
      return;
    }

    if (isAuthenticated && !isAdminUser(user)) {
      const key = 'ACCESS_DENIED_ADMIN';
      if (lastNoticeRef.current !== key) {
        lastNoticeRef.current = key;
        toast.error('Bạn không có quyền truy cập khu vực quản trị.', {
          title: 'Truy cập bị từ chối',
        });
      }
    }
  }, [isAuthenticated, isLoading, toast, user]);

  if (isLoading) return null;

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location,
          message: 'Bạn cần đăng nhập với quyền ADMIN để truy cập trang quản trị.',
        }}
      />
    );
  }

  if (!isAdminUser(user)) {
    return <Navigate to="/" replace />;
  }

  return children;
}

export default AdminRoute;