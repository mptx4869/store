import api from './api';

function buildBookError(error) {
  const { status, backendMessage, code } = error.customError || {};
  const fallback = 'Unable to perform operation on book. Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 400) return new Error('Invalid data.');
  if (status === 401) return new Error('Please log in.');
  if (status === 403) return new Error('You do not have permission to manage books.');
  if (status === 404) return new Error('Book not found.');
  if (status === 409) {
    if (backendMessage?. includes('SKU')) return new Error('SKU code already exists.');
    if (backendMessage?.includes('default')) return new Error('Must have at least 1 default SKU.');
    if (backendMessage?.includes('last SKU')) return new Error('Cannot delete the last SKU.');
    return new Error('Data conflict.');
  }

  return new Error(fallback);
}

function mapSku(sku) {
  return {
    id: sku.id,
    sku: sku.sku ??  '',
    format: sku.format ?? '',
    price: sku.price ?? 0,
    stock: sku.stock ?? 0,
    reserved: sku.reserved ?? 0,
    available: sku. available ?? 0,
    isDefault: !! sku.isDefault,
    weightGrams: sku.weightGrams ?? null,
    lengthMm: sku.lengthMm ?? null,
    widthMm: sku.widthMm ?? null,
    heightMm: sku.heightMm ?? null,
  };
}

function mapCategory(cat) {
  return {
    id: cat.id,
    name: cat.name ?? '',
  };
}

function mapBook(book) {
  return {
    id: book.id,
    title: book.title ?? '',
    subtitle: book.subtitle ?? '',
    description: book.description ?? '',
    language: book.language ?? '',
    pages: book.pages ?? 0,
    publishedDate: book.publishedDate ?? '',
    imageUrl: book.imageUrl ?? null,
    basePrice: book.basePrice ?? 0,
    defaultSkuId: book.defaultSkuId ?? null,
    createdAt: book.createdAt ?? null,
    updatedAt: book.updatedAt ?? null,
    deletedAt: book.deletedAt ?? null,
    categories: Array.isArray(book.categories) ? book.categories.map(mapCategory) : [],
    skus: Array.isArray(book.skus) ? book.skus.map(mapSku) : [],
  };
}

function mapBookPage(data) {
  return {
    content: Array.isArray(data?.content) ? data.content.map(mapBook) : [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?. totalPages ?? 0,
    size: data?.pageable?.pageSize ?? data?.size ?? 10,
    number: data?.pageable?.pageNumber ?? data?.number ?? 0,
  };
}

const adminBookService = {
  /**
   * GET /admin/books
   */
  async getBooks({
    page = 0,
    size = 10,
    sortBy = 'createdAt',
    sortDirection = 'DESC',
    includeDeleted = false,
  } = {}) {
    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sortBy,
        sortDirection,
        includeDeleted:  String(includeDeleted),
      });

      const data = await api.get(`/admin/books?${params}`);
      return mapBookPage(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * GET /admin/books/{id}
   */
  async getBookById(bookId) {
    try {
      const data = await api.get(`/admin/books/${bookId}`);
      return mapBook(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * POST /admin/books
   * @param {object} payload
   *   - title, subtitle, description, language, pages, publishedDate
   *   - imageUrl:  string | null
   *   - basePrice: number
   *   - categoryIds: number[]
   *   - skus: array of { sku, format, priceOverride, initialStock, weightGrams, .. ., isDefault }
   */
  async createBook(payload) {
    try {
      const data = await api.post('/admin/books', payload);
      return mapBook(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * PUT /admin/books/{id}
   * @param {object} payload
   *   - title, subtitle, description, language, pages, publishedDate
   *   - imageUrl: string | null
   *   - basePrice: number
   *   - categoryIds:  number[]
   */
  async updateBook(bookId, payload) {
    try {
      const data = await api. put(`/admin/books/${bookId}`, payload);
      return mapBook(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * DELETE /admin/books/{id}? hard=false
   */
  async deleteBook(bookId, hard = false) {
    try {
      await api.delete(`/admin/books/${bookId}?hard=${hard}`);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * PATCH /admin/books/{id}/restore
   */
  async restoreBook(bookId) {
    try {
      const data = await api.patch(`/admin/books/${bookId}/restore`);
      return mapBook(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  // ==================== SKU Management ====================

  /**
   * POST /admin/books/{bookId}/skus
   * @param {object} payload
   *   - sku, format, priceOverride, initialStock, weightGrams, lengthMm, widthMm, heightMm, isDefault
   */
  async addSku(bookId, payload) {
    try {
      const data = await api.post(`/admin/books/${bookId}/skus`, payload);
      return mapBook(data); // Response is AdminBookResponse
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * PUT /admin/books/{bookId}/skus/{skuId}
   * @param {object} payload
   *   - sku, format, priceOverride, weightGrams, lengthMm, widthMm, heightMm
   *   - NOTE: Cannot update isDefault via this endpoint
   */
  async updateSku(bookId, skuId, payload) {
    try {
      const data = await api. put(`/admin/books/${bookId}/skus/${skuId}`, payload);
      return mapBook(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * PATCH /admin/books/{bookId}/skus/{skuId}/set-default
   */
  async setDefaultSku(bookId, skuId) {
    try {
      const data = await api.patch(`/admin/books/${bookId}/skus/${skuId}/set-default`);
      return mapBook(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * DELETE /admin/books/{bookId}/skus/{skuId}
   */
  async deleteSku(bookId, skuId) {
    try {
      const data = await api.delete(`/admin/books/${bookId}/skus/${skuId}`);
      return mapBook(data); // Response 200 OK with AdminBookResponse
    } catch (error) {
      throw buildBookError(error);
    }
  },
};

export default adminBookService;