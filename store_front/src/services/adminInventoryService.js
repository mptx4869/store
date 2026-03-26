import api from './api';

function buildInventoryError(error) {
  const { status, backendMessage, code } = error. customError || {};
  const fallback = 'Unable to perform operation on inventory.  Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 400) return new Error('Invalid data.');
  if (status === 401) return new Error('Please log in.');
  if (status === 403) return new Error('You do not have permission to manage inventory.');
  if (status === 404) return new Error('SKU not found in inventory.');
  if (status === 409) {
    if (backendMessage?. includes('reserved')) {
      return new Error('Cannot reduce inventory below reserved quantity.');
    }
    return new Error('Data conflict.');
  }

  return new Error(fallback);
}

function mapInventory(inv) {
  return {
    skuId: inv.skuId,
    sku: inv.sku ??  '',
    bookId: inv.bookId,
    bookTitle: inv.bookTitle ??  '',
    format: inv.format ?? '',
    totalStock: inv.totalStock ??  0,
    reservedStock: inv.reservedStock ?? 0,
    availableStock: inv.availableStock ?? 0,
    status:  inv.status ?? 'UNKNOWN', // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    lastUpdated: inv.lastUpdated ?? null,
  };
}

function mapInventoryPage(data) {
  return {
    content: Array.isArray(data?. content) ? data.content.map(mapInventory) : [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    size: data?.pageable?.pageSize ?? data?.size ?? 20,
    number: data?.pageable?.pageNumber ?? data?.number ?? 0,
  };
}

const adminInventoryService = {
  /**
   * GET /admin/inventory? page=0&size=20&sort=lastUpdated&direction=DESC
   */
  async getInventory({ page = 0, size = 20, sort = 'lastUpdated', direction = 'DESC' } = {}) {
    try {
      const params = new URLSearchParams({
        page:  String(page),
        size: String(size),
        sort,
        direction,
      });

      const data = await api.get(`/admin/inventory?${params}`);
      return mapInventoryPage(data);
    } catch (error) {
      throw buildInventoryError(error);
    }
  },

  /**
   * GET /admin/inventory/low-stock
   */
  async getLowStock() {
    try {
      const data = await api.get('/admin/inventory/low-stock');
      return Array.isArray(data) ? data. map(mapInventory) : [];
    } catch (error) {
      throw buildInventoryError(error);
    }
  },

  /**
   * PUT /admin/inventory/{skuId}
   * @param {number} skuId
   * @param {object} payload - { stock?, reserved?, action? }
   *   - stock: number (quantity to add or set)
   *   - reserved: number (reserved quantity to set)
   *   - action: "ADD" | "SET" (default: "SET")
   */
  async updateStock(skuId, payload) {
    try {
      const data = await api.put(`/admin/inventory/${skuId}`, payload);
      return mapInventory(data);
    } catch (error) {
      throw buildInventoryError(error);
    }
  },
};

export default adminInventoryService;