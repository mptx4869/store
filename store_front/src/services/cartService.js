import api from './api';

function buildCartError(error) {
  const { status, backendMessage, code } = error.customError || {};
  const fallback = 'Unable to perform cart operation.  Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 401) return new Error('Please log in to use cart.');
  if (status === 404) return new Error('Product not found in cart.');
  if (status === 400) return new Error('Invalid data.');

  if (code) return new Error(fallback);
  return new Error(fallback);
}

function mapCartItem(item) {
  return {
    itemId: item.itemId,
    bookId: item.bookId,
    skuId: item.skuId ??  null,
    title: item.title ??  '',
    sku: item. sku ?? '',
    format: item.format ?? '',
    // Prefer explicit image field, then fall back to API imageUrl/bookImageUrl
    image: item.image ?? item.imageUrl ?? item.bookImageUrl ?? null,
    unitPrice: item.price ?? 0,                    // ← FIX: API returns "price" not "unitPrice"
    quantity: item.quantity ?? 1,
    lineTotal: (item.price ?? 0) * (item.quantity ?? 1),  // ← FIX: must calculate
    originalPrice: item.originalPrice ?? item.price,
    priceChanged: !!item.priceChanged,
    priceDiff: item. priceDiff ?? 0,
  };
}

function mapCartSummary(data) {
  return {
    items: Array.isArray(data?. items) ? data.items.map(mapCartItem) : [],
    totalPrice: data?.totalAmount ?? 0,           // ← FIX: API returns "totalAmount" not "totalPrice"
  };
}

const cartService = {
  async getCart() {
    try {
      const data = await api.get('/cart');
      return mapCartSummary(data);
    } catch (error) {
      throw buildCartError(error);
    }
  },

  async addToCart(skuId, quantity = 1) {
    try {
      const data = await api.post('/cart/items', { skuId, quantity });
      return mapCartSummary(data);
    } catch (error) {
      throw buildCartError(error);
    }
  },

  async updateQuantity(itemId, quantity) {
    try {
      const data = await api.patch(`/cart/items/${itemId}`, { quantity });  // ← FIX:  PATCH not PUT
      return mapCartSummary(data);
    } catch (error) {
      throw buildCartError(error);
    }
  },

  async removeFromCart(itemId) {
    try {
      const data = await api.delete(`/cart/items/${itemId}`);
      return mapCartSummary(data);
    } catch (error) {
      throw buildCartError(error);
    }
  },

  async clearCart() {
    try {
      const data = await api.delete('/cart');  // ← FIX: DELETE /cart not /cart/clear
      return mapCartSummary(data);
    } catch (error) {
      throw buildCartError(error);
    }
  },
};

export default cartService;