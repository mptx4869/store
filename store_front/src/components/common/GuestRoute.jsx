import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context';
import { Loader } from 'lucide-react';

function GuestRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  // Đang kiểm tra trạng thái đăng nhập
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader className="w-10 h-10 text-blue-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-500">Đang tải...</p>
        </div>
      </div>
    );
  }

  // Đã đăng nhập, chuyển về trang trước đó hoặc trang chủ
  if (isAuthenticated) {
    const from = location.state?.from?.pathname || '/';
    return <Navigate to={from} replace />;
  }

  // Chưa đăng nhập, hiển thị nội dung (login/register)
  return children;
}

export default GuestRoute;