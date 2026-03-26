import api from './api';

function mapOrderItem(apiItem) {
  return {
    bookId: apiItem.bookId,
    title: apiItem.title,
    sku: apiItem.sku,
    quantity: apiItem.quantity,
    unitPrice: apiItem.unitPrice,
    lineTotal: apiItem.lineTotal,
  };
}

function mapOrder(apiOrder) {
  return {
    orderId: apiOrder.orderId,
    status: apiOrder.status,
    currency: apiOrder.currency,
    totalAmount: apiOrder.totalAmount,
    placedAt: apiOrder.placedAt,
    cartId: apiOrder.cartId,
    shippingAddress: apiOrder.shippingAddress,
    shippingPhone: apiOrder.shippingPhone,
    billingAddress: apiOrder.billingAddress,
    billingPhone: apiOrder.billingPhone,
    items: Array.isArray(apiOrder.items) ? apiOrder.items.map(mapOrderItem) : [],
  };
}

function buildOrderError(error) {
  const { status, backendMessage, code } = error.customError || {};
  const fallback = 'Unable to process order at this time. Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 401) return new Error('You need to log in to perform this action.');
  if (status === 403) return new Error('You do not have permission to perform this action.');
  if (status === 404) return new Error('Order not found or you do not have permission to access.');
  if (status === 409) {
    // Backend usually describes very specifically in message, but if missing then fallback:
    return new Error(
      'Cannot perform due to data conflict (empty cart, out of stock or invalid status).'
    );
  }
  if (status === 400) return new Error('Invalid data sent.');

  if (code) return new Error(fallback);
  return new Error(fallback);
}

const orderService = {
  /**
   * GET /orders
   * Response: OrderResponse[]
   */
  async getOrders() {
    try {
      const data = await api.get('/orders');
      return Array.isArray(data) ? data.map(mapOrder) : [];
    } catch (error) {
      throw buildOrderError(error);
    }
  },

  /**
   * GET /orders/{orderId}
   * Response: OrderResponse
   */
  async getOrderById(orderId) {
    try {
      const data = await api.get(`/orders/${orderId}`);
      return mapOrder(data);
    } catch (error) {
      throw buildOrderError(error);
    }
  },

  /**
   * POST /orders
   * Body is optional (can be omitted entirely)
   */
  async createOrder(payload) {
    try {
      // If payload undefined/null => call post without body
      const data = payload ? await api.post('/orders', payload) : await api.post('/orders');
      return mapOrder(data);
    } catch (error) {
      throw buildOrderError(error);
    }
  },

  /**
   * PATCH /orders/{orderId}/cancel
   */
  async cancelOrder(orderId) {
    try {
      const data = await api.patch(`/orders/${orderId}/cancel`);
      return mapOrder(data);
    } catch (error) {
      throw buildOrderError(error);
    }
  },
};

export default orderService;