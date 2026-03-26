import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import adminUserService from '../../services/adminUserService';
import { useToast } from '../../context/ToastContext';

function formatDateTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString();
}

function AdminUserDetailPage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const load = async () => {
    setIsLoading(true);
    setError('');
    try {
      const data = await adminUserService.getUserById(userId);
      setUser(data);
    } catch (err) {
      setError(err.message || 'Could not load user information');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const handleToggleStatus = async () => {
    if (!user) return;

    const nextStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const ok = window.confirm(
      nextStatus === 'INACTIVE'
        ? `Confirm deactivate account "${user.username}"?`
        : `Confirm reactivate account "${user.username}"?`
    );
    if (!ok) return;

    setBusy(true);
    try {
      const updated = await adminUserService.updateUserStatus(user.id, nextStatus);
      toast.success(`Updated user "${updated.username}" status to ${updated.status}.`);
      setUser((prev) => ({ ...prev, status: updated.status }));
    } catch (err) {
      toast.error(err.message || 'Could not update user status.');
    } finally {
      setBusy(false);
    }
  };

  const handleToggleRole = async () => {
    if (!user) return;

    const nextRole = user.role === 'ADMIN' ? 'CUSTOMER' : 'ADMIN';
    const ok = window.confirm(
      nextRole === 'ADMIN'
        ? `Confirm grant ADMIN role to "${user.username}"?`
        : `Confirm revoke ADMIN role from "${user.username}" (change to CUSTOMER)?`
    );
    if (!ok) return;

    setBusy(true);
    try {
      const updated = await adminUserService.updateUserRole(user.id, nextRole);
      toast.success(`Updated user "${updated.username}" role to ${updated.role}.`);
      setUser((prev) => ({ ...prev, role: updated.role }));
    } catch (err) {
      toast.error(err.message || 'Could not update user role.');
    } finally {
      setBusy(false);
    }
  };

  if (isLoading) {
    return <div className="text-gray-600">Loading user information...</div>;
  }

  if (error && !user) {
    return (
      <div>
        <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl p-4 mb-4">
          {error}
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>
          Back
        </button>
      </div>
    );
  }

  if (!user) return null;

  return (
    <div>
      <div className="flex items-start justify-between gap-4">
        <div>
          <Link to="/admin/users" className="text-blue-600 hover:text-blue-700 text-sm">
            ← Back to list
          </Link>
          <h1 className="text-2xl font-bold text-gray-800 mt-2">
            User: {user.username}
          </h1>
          <p className="text-sm text-gray-500 mt-1">ID: {user.id}</p>
        </div>

        <div className="flex gap-2">
          <button
            className="btn-secondary"
            disabled={busy}
            onClick={handleToggleRole}
          >
            {user.role === 'ADMIN' ? 'Change to CUSTOMER' : 'Grant ADMIN'}
          </button>
          <button
            className="px-4 py-2 rounded-lg font-semibold disabled:opacity-50 border transition-colors
              border-red-200 text-red-700 hover:bg-red-50"
            disabled={busy}
            onClick={handleToggleStatus}
          >
            {user.status === 'ACTIVE' ? 'Deactivate' : 'Reactivate'}
          </button>
        </div>
      </div>

      <div className="grid lg:grid-cols-2 gap-6 mt-6">
        <div className="border rounded-xl p-4">
          <h2 className="font-semibold text-gray-800 mb-3">Information</h2>
          <div className="text-sm text-gray-700 space-y-2">
            <p><span className="text-gray-500">Email:</span> {user.email}</p>
            <p><span className="text-gray-500">Role:</span> {user.role}</p>
            <p><span className="text-gray-500">Status:</span> {user.status}</p>
            <p><span className="text-gray-500">Created:</span> {formatDateTime(user.createdAt)}</p>
            <p><span className="text-gray-500">Updated:</span> {formatDateTime(user.updatedAt)}</p>
          </div>
        </div>

        <div className="border rounded-xl p-4">
          <h2 className="font-semibold text-gray-800 mb-3">Statistics</h2>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-gray-50 rounded-lg p-3">
              <p className="text-gray-500">Total orders</p>
              <p className="text-lg font-bold text-gray-800">{user.totalOrders}</p>
            </div>
            <div className="bg-gray-50 rounded-lg p-3">
              <p className="text-gray-500">Completed</p>
              <p className="text-lg font-bold text-gray-800">{user.completedOrders}</p>
            </div>
            <div className="bg-gray-50 rounded-lg p-3">
              <p className="text-gray-500">Cancelled</p>
              <p className="text-lg font-bold text-gray-800">{user.cancelledOrders}</p>
            </div>
            <div className="bg-gray-50 rounded-lg p-3">
              <p className="text-gray-500">Total spent</p>
              <p className="text-lg font-bold text-gray-800">{user.totalSpent}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-6 border rounded-xl p-4">
        <h2 className="font-semibold text-gray-800 mb-3">Recent orders</h2>
        {user.recentOrders?.length ? (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="text-gray-500">
                <tr className="text-left">
                  <th className="py-2 pr-4">Order ID</th>
                  <th className="py-2 pr-4">Status</th>
                  <th className="py-2 pr-4">Total</th>
                  <th className="py-2 pr-4">Created</th>
                </tr>
              </thead>
              <tbody className="text-gray-800">
                {user.recentOrders.map((o) => (
                  <tr key={o.orderId} className="border-t">
                    <td className="py-2 pr-4">#{o.orderId}</td>
                    <td className="py-2 pr-4">{o.status}</td>
                    <td className="py-2 pr-4">{o.total}</td>
                    <td className="py-2 pr-4">{formatDateTime(o.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-sm text-gray-500">No recent orders.</p>
        )}
      </div>
    </div>
  );
}

export default AdminUserDetailPage;