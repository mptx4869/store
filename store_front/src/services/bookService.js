import api from './api';

function buildBookError(error) {
  const { status, backendMessage } = error. customError || {};
  const fallback = 'Unable to load book information.  Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 404) return new Error('Book not found.');
  if (status === 500) return new Error('System error. Please try again later.');

  return new Error(fallback);
}

function mapSku(s) {
  return {
    id: s.id,
    sku: s.sku,
    format: s.format,
    price: s.price,
    inStock: !! s.inStock,
    availableStock: s.availableStock ??  0,
    isDefault: !!s.isDefault,
    weightGrams: s.weightGrams ??  null,
    lengthMm: s.lengthMm ?? null,
    widthMm: s.widthMm ?? null,
    heightMm: s.heightMm ?? null,
  };
}

function mapBook(b) {
  return {
    id: b.id,
    title: b.title,
    subtitle: b.subtitle ??  '',
    description: b.description ?? '',
    language: b. language ?? '',
    pages: b.pages ?? 0,
    publishedDate:  b.publishedDate ?? '',
    price: b.price,
    sku: b.sku,
    // Map imageUrl -> image to be compatible with BookCard
    image: b.imageUrl ?? null,
    imageUrl: b.imageUrl ?? null,
    skus: Array.isArray(b.skus) ? b.skus.map(mapSku) : [],
  };
}

function mapBookPage(data) {
  return {
    content: Array.isArray(data?.content) ? data.content.map(mapBook) : [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    size: data?.pageable?.pageSize ?? data?.size ?? 0,
    number: data?.pageable?.pageNumber ?? data?.number ?? 0,
    first: data?.first ?? false,
    last: data?.last ?? false,
  };
}

const bookService = {
  async getBooks() {
    try {
      const data = await api.get('/books');
      return Array.isArray(data) ? data.map(mapBook) : [];
    } catch (error) {
      throw buildBookError(error);
    }
  },

  async getBookById(id) {
    try {
      const data = await api.get(`/books/${id}`);
      return mapBook(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  async searchBooks(keyword, page = 0, size = 12, sortBy = 'createdAt', sortDirection = 'DESC') {
    try {
      const params = new URLSearchParams({
        keyword,
        page: String(page),
        size: String(size),
        sortBy,
        sortDirection,
      });
      const data = await api.get(`/books/search?${params}`);
      return mapBookPage(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },

  /**
   * GET /books/new
   * @param {Object} options
   * @param {number} options.page
   * @param {number} options.size
   * @param {string} options.sortBy - createdAt | publishedDate | title
   * @param {string} options.sortDirection - ASC | DESC
   */
  async getNewBooks({ page = 0, size = 6, sortBy = 'createdAt', sortDirection = 'DESC' } = {}) {
    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sortBy,
        sortDirection,
      });

      const data = await api.get(`/books/new?${params}`);
      return mapBookPage(data);
    } catch (error) {
      throw buildBookError(error);
    }
  },
};

export default bookService;