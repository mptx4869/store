import api from './api';

function buildOrderError(error) {
  const { status, backendMessage } = error.customError || {};
  const fallback = 'Unable to perform operation on order.  Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 400) return new Error('Invalid data.');
  if (status === 401) return new Error('Please log in.');
  if (status === 403) return new Error('You do not have permission to manage orders.');
  if (status === 404) return new Error('Order not found.');
  if (status === 409) {
    if (backendMessage?.includes('final state')) {
      return new Error('Cannot change status of completed order.');
    }
    if (backendMessage?.includes('transition')) {
      return new Error('Cannot transition to this status.');
    }
    return new Error('Data conflict.');
  }

  return new Error(fallback);
}

function mapOrderItem(item) {
  return {
    bookId: item.bookId,
    title: item.title ?? '',
    sku: item.sku ?? '',
    quantity: item.quantity ?? 0,
    unitPrice: item.unitPrice ?? 0,
    lineTotal: item.lineTotal ?? 0,
  };
}

function mapOrder(order) {
  return {
    orderId: order.orderId,
    status: order.status ?? 'PLACED',
    currency: order.currency ?? 'USD',
    totalAmount: order.totalAmount ?? 0,
    placedAt: order.placedAt ?? null,
    cartId: order.cartId ?? null,
    shippingAddress: order.shippingAddress ?? '',
    shippingPhone: order.shippingPhone ?? '',
    billingAddress: order.billingAddress ?? '',
    billingPhone: order.billingPhone ?? '',
    items: Array.isArray(order.items) ? order.items.map(mapOrderItem) : [],
  };
}

function mapOrderPage(data) {
  return {
    content: Array.isArray(data?.content) ? data.content.map(mapOrder) : [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    size: data?.pageable?.pageSize ?? data?.size ?? 20,
    number: data?.pageable?.pageNumber ?? data?.number ?? 0,
  };
}

const adminOrderService = {
  /**
   * GET /admin/orders
   */
  async getOrders({ status = null, page = 0, size = 20, sort = 'placedAt,desc' } = {}) {
    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
      });

      if (status) {
        params.append('status', status);
      }

      const data = await api.get(`/admin/orders?${params}`);
      return mapOrderPage(data);
    } catch (error) {
      throw buildOrderError(error);
    }
  },

  /**
   * GET /admin/orders/{orderId}
   */
  async getOrderById(orderId) {
    try {
      const data = await api.get(`/admin/orders/${orderId}`);
      return mapOrder(data);
    } catch (error) {
      throw buildOrderError(error);
    }
  },

  /**
   * PATCH /admin/orders/{orderId}/status
   */
  async updateOrderStatus(orderId, status) {
    try {
      const data = await api.patch(`/admin/orders/${orderId}/status`, { status });
      return mapOrder(data);
    } catch (error) {
      throw buildOrderError(error);
    }
  },

  /**
   * GET /admin/orders/analytics/revenue
   */
  async getRevenueAnalytics(startDate, endDate, groupBy = 'DAY') {
    try {
      const params = new URLSearchParams({
        startDate,
        endDate,
        groupBy,
      });
      const data = await api.get(`/admin/orders/analytics/revenue?${params}`);
      return data;
    } catch (error) {
      throw buildOrderError(error);
    }
  },
};

export default adminOrderService;