import { useState } from 'react';
import ChangePasswordForm from '../components/common/ChangePasswordForm';
import { useAuth } from '../context/AuthContext';
import authService from '../services/authService';

function ChangePasswordPage() {
  const { logout } = useAuth();
  const [isLoading, setIsLoading] = useState(false);

  const handleChangePassword = async ({ oldPassword, newPassword }) => {
    setIsLoading(true);
    try {
      await authService.changePassword(oldPassword, newPassword);
      // Logout after password change successful
      await logout();
      window.location.href = '/login?changed=1';
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6 text-center">Change Password</h1>
      <ChangePasswordForm onSubmit={handleChangePassword} isLoading={isLoading} />
      <p className="text-sm text-gray-500 mt-6 text-center">
        After changing your password successfully, you will be logged out and need to log in again.
      </p>
    </div>
  );
}

export default ChangePasswordPage;
