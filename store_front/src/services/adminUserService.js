import api from './api';

function buildAdminUserError(error) {
  const { status, backendMessage, code } = error.customError || {};
  const fallback = 'Unable to process user management request. Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 401) return new Error('Invalid login session. Please log in again.');
  if (status === 403) return new Error('You do not have ADMIN permission to perform this action.');
  if (status === 404) return new Error('User not found.');
  if (status === 400) return new Error('Invalid data. Please check again.');

  if (code) return new Error(fallback);
  return new Error(fallback);
}

function mapUserListItem(u) {
  return {
    id: u.id,
    username: u.username,
    email: u.email,
    role: u.role,
    status: u.status,
    createdAt: u.createdAt,
    totalOrders: u.totalOrders ?? 0,
  };
}

function mapUserDetail(u) {
  return {
    id: u.id,
    username: u.username,
    email: u.email,
    role: u.role,
    status: u.status,
    createdAt: u.createdAt,
    updatedAt: u.updatedAt,
    totalOrders: u.totalOrders ?? 0,
    completedOrders: u.completedOrders ?? 0,
    cancelledOrders: u.cancelledOrders ?? 0,
    totalSpent: u.totalSpent ?? '0',
    recentOrders: Array.isArray(u.recentOrders) ? u.recentOrders : [],
  };
}

const adminUserService = {
  async getUsers({ page = 0, size = 20, sort = 'createdAt,desc', role, status } = {}) {
    try {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(size));
      params.set('sort', String(sort));
      if (role) params.set('role', role);
      if (status) params.set('status', status);

      const data = await api.get(`/admin/users?${params.toString()}`);

      return {
        content: Array.isArray(data?.content) ? data.content.map(mapUserListItem) : [],
        pageable: data?.pageable,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
        last: !!data?.last,
        first: !!data?.first,
        size: data?.size ?? size,
        number: data?.number ?? page,
        numberOfElements: data?.numberOfElements ?? 0,
        empty: !!data?.empty,
      };
    } catch (error) {
      throw buildAdminUserError(error);
    }
  },

  async getUserById(userId) {
    try {
      const data = await api.get(`/admin/users/${userId}`);
      return mapUserDetail(data);
    } catch (error) {
      throw buildAdminUserError(error);
    }
  },

  async updateUserStatus(userId, status) {
    try {
      const data = await api.patch(`/admin/users/${userId}/status`, { status });
      return mapUserListItem(data);
    } catch (error) {
      throw buildAdminUserError(error);
    }
  },

  async updateUserRole(userId, role) {
    try {
      const data = await api.patch(`/admin/users/${userId}/role`, { role });
      return mapUserListItem(data);
    } catch (error) {
      throw buildAdminUserError(error);
    }
  },
};

export default adminUserService;