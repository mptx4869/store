import { useEffect, useState } from 'react';
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
  const defaultCursor = { lastId: null, lastCreatedAt: null };

  const [role, setRole] = useState(''); // '', 'ADMIN', 'CUSTOMER'
  const [status, setStatus] = useState(''); // '', 'ACTIVE', 'INACTIVE'
  const [username, setUsername] = useState('');
  const [usernameInput, setUsernameInput] = useState('');

  const [pageIndex, setPageIndex] = useState(0);
  const [cursorStack, setCursorStack] = useState([defaultCursor]);
  const [hasNext, setHasNext] = useState(false);
  const [size] = useState(20);
  const [users, setUsers] = useState([]);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const resetPagination = () => {
    setPageIndex(0);
    setCursorStack([defaultCursor]);
  };

  const load = async () => {
    setIsLoading(true);
    setError('');
    try {
      const cursor = cursorStack[pageIndex] || defaultCursor;
      const res = await adminUserService.getUsersCursor({
        size,
        lastId: cursor.lastId ?? undefined,
        lastCreatedAt: cursor.lastCreatedAt ?? undefined,
        role: role || undefined,
        status: status || undefined,
        username: username || undefined,
      });
      setUsers(res.content);
      const canGoNext = !!res.hasNext && res.nextLastId && res.nextLastCreatedAt;
      setHasNext(canGoNext);
      setCursorStack((prev) => {
        const next = prev.slice(0, pageIndex + 1);
        if (canGoNext) {
          next[pageIndex + 1] = {
            lastId: res.nextLastId,
            lastCreatedAt: res.nextLastCreatedAt,
          };
        }
        return next;
      });
    } catch (err) {
      setError(err.message || 'Could not load user list');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageIndex, role, status, username]);

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
      setUsers((prev) => prev.map((x) => (x.id === u.id ? { ...x, status: updated.status } : x)));
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
      setUsers((prev) => prev.map((x) => (x.id === u.id ? { ...x, role: updated.role } : x)));
    } catch (err) {
      toast.error(err.message || 'Could not update role.');
    }
  };

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    const trimmed = usernameInput.trim();
    setUsername(trimmed);
    setUsernameInput(trimmed);
    resetPagination();
  };

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
          Showing: <span className="font-semibold text-gray-800">{users.length}</span>
        </div>
      </div>

      {/* Filters */}
      <form className="grid sm:grid-cols-5 gap-3 mt-6" onSubmit={handleSearchSubmit}>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
          <input
            type="text"
            value={usernameInput}
            onChange={(e) => setUsernameInput(e.target.value)}
            className="input-field w-full"
            placeholder="Search username"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
          <select
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              resetPagination();
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
              resetPagination();
            }}
            className="input-field w-full"
          >
            <option value="">All</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
        </div>

        <div className="flex items-end">
          <button className="btn-primary w-full" type="submit">
            Search
          </button>
        </div>

        <div className="flex items-end">
          <button
            className="btn-secondary w-full"
            type="button"
            onClick={() => {
              setRole('');
              setStatus('');
              setUsername('');
              setUsernameInput('');
              resetPagination();
            }}
          >
            Reset filters
          </button>
        </div>
      </form>

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
                {users.map((u) => (
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

                {users.length === 0 && (
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
        {!isLoading && !error && (pageIndex > 0 || hasNext) && (
          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-gray-500">
              Page {pageIndex + 1}
            </p>

            <div className="flex gap-2">
              <button
                className="btn-secondary"
                disabled={pageIndex === 0}
                onClick={() => setPageIndex((p) => Math.max(0, p - 1))}
              >
                Prev
              </button>
              <button
                className="btn-secondary"
                disabled={!hasNext}
                onClick={() => setPageIndex((p) => p + 1)}
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