import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import adminUserService from '../../services/adminUserService';
import { useToast } from '../../context/ToastContext';

function formatDateTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString();
}

function AdminUsersPage() {
  const toast = useToast();

  const [role, setRole] = useState(''); // '', 'ADMIN', 'CUSTOMER'
  const [status, setStatus] = useState(''); // '', 'ACTIVE', 'INACTIVE'

  const [page, setPage] = useState(0);
  const [size] = useState(20);

  const [data, setData] = useState({
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 20,
    first: true,
    last: true,
  });

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const sort = useMemo(() => 'createdAt,desc', []);

  const load = async () => {
    setIsLoading(true);
    setError('');
    try {
      const res = await adminUserService.getUsers({
        page,
        size,
        sort,
        role: role || undefined,
        status: status || undefined,
      });
      setData(res);
    } catch (err) {
      setError(err.message || 'Could not load user list');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, role, status]);

  const handleToggleStatus = async (u) => {
    const next = u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const ok = window.confirm(
      next === 'INACTIVE'
        ? `Confirm deactivate "${u.username}"?`
        : `Confirm reactivate "${u.username}"?`
    );
    if (!ok) return;

    try {
      const updated = await adminUserService.updateUserStatus(u.id, next);
      toast.success(`Updated status of "${updated.username}" to ${updated.status}.`);
      setData((prev) => ({
        ...prev,
        content: prev.content.map((x) => (x.id === u.id ? { ...x, status: updated.status } : x)),
      }));
    } catch (err) {
      toast.error(err.message || 'Could not update status.');
    }
  };

  const handleToggleRole = async (u) => {
    const next = u.role === 'ADMIN' ? 'CUSTOMER' : 'ADMIN';
    const ok = window.confirm(
      next === 'ADMIN'
        ? `Confirm grant ADMIN role to "${u.username}"?`
        : `Confirm revoke ADMIN role from "${u.username}" (to CUSTOMER)?`
    );
    if (!ok) return;

    try {
      const updated = await adminUserService.updateUserRole(u.id, next);
      toast.success(`Updated role of "${updated.username}" to ${updated.role}.`);
      setData((prev) => ({
        ...prev,
        content: prev.content.map((x) => (x.id === u.id ? { ...x, role: updated.role } : x)),
      }));
    } catch (err) {
      toast.error(err.message || 'Could not update role.');
    }
  };

  const resetToFirstPage = () => setPage(0);

  return (
    <div>
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Users</h1>
          <p className="text-sm text-gray-500 mt-1">
            User list (filter by role/status).
          </p>
        </div>

        <div className="text-sm text-gray-600">
          Total: <span className="font-semibold text-gray-800">{data.totalElements}</span>
        </div>
      </div>

      {/* Filters */}
      <div className="grid sm:grid-cols-3 gap-3 mt-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
          <select
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              resetToFirstPage();
            }}
            className="input-field w-full"
          >
            <option value="">All</option>
            <option value="ADMIN">ADMIN</option>
            <option value="CUSTOMER">CUSTOMER</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              resetToFirstPage();
            }}
            className="input-field w-full"
          >
            <option value="">All</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
        </div>

        <div className="flex items-end">
          <button
            className="btn-secondary w-full"
            onClick={() => {
              setRole('');
              setStatus('');
              setPage(0);
            }}
          >
            Reset filters
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="mt-6">
        {isLoading && <div className="text-gray-600">Loading users...</div>}

        {error && (
          <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl p-4">
            {error}
          </div>
        )}

        {!isLoading && !error && (
          <div className="overflow-x-auto border rounded-xl">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 text-gray-600">
                <tr className="text-left">
                  <th className="py-3 px-4">User</th>
                  <th className="py-3 px-4">Role</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Created</th>
                  <th className="py-3 px-4">Orders</th>
                  <th className="py-3 px-4">Actions</th>
                </tr>
              </thead>

              <tbody className="text-gray-800">
                {data.content.map((u) => (
                  <tr key={u.id} className="border-t">
                    <td className="py-3 px-4">
                      <Link
                        to={`/admin/users/${u.id}`}
                        className="font-semibold text-blue-600 hover:text-blue-700"
                      >
                        {u.username}
                      </Link>
                      <div className="text-xs text-gray-500">{u.email}</div>
                    </td>

                    <td className="py-3 px-4">{u.role}</td>
                    <td className="py-3 px-4">{u.status}</td>
                    <td className="py-3 px-4">{formatDateTime(u.createdAt)}</td>
                    <td className="py-3 px-4">{u.totalOrders}</td>

                    <td className="py-3 px-4">
                      <div className="flex flex-wrap gap-2">
                        <button
                          className="btn-secondary"
                          onClick={() => handleToggleRole(u)}
                        >
                          {u.role === 'ADMIN' ? 'Demote' : 'Promote'}
                        </button>
                        <button
                          className="px-3 py-2 rounded-lg border text-sm font-semibold transition-colors
                            border-red-200 text-red-700 hover:bg-red-50"
                          onClick={() => handleToggleStatus(u)}
                        >
                          {u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}

                {data.content.length === 0 && (
                  <tr className="border-t">
                    <td className="py-6 px-4 text-center text-gray-500" colSpan={6}>
                      No users match the current filters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {!isLoading && !error && data.totalPages > 1 && (
          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-gray-500">
              Page {data.number + 1} / {data.totalPages}
            </p>

            <div className="flex gap-2">
              <button
                className="btn-secondary"
                disabled={data.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Prev
              </button>
              <button
                className="btn-secondary"
                disabled={data.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default AdminUsersPage;