import api from './api';

function buildCategoryError(error) {
  const { status, backendMessage, code } = error.customError || {};
  const fallback = 'Unable to perform operation on category. Please try again later.';

  if (backendMessage) return new Error(backendMessage);

  if (status === 400) return new Error('Invalid data.');
  if (status === 401) return new Error('Please log in.');
  if (status === 403) return new Error('You do not have permission to manage categories.');
  if (status === 404) return new Error('Category not found.');
  if (status === 409) {
    if (code === 'DUPLICATE_NAME') return new Error('Category name already exists.');
    if (backendMessage?. includes('books')) return new Error('Cannot delete category with books.  Please delete books first.');
    return new Error('Data conflict.');
  }

  return new Error(fallback);
}

function mapCategory(cat) {
  return {
    id: cat.id,
    name: cat.name ??  '',
    description: cat.description ??  '',
    bookCount: cat.bookCount ?? 0,
    createdAt: cat.createdAt ??  null,
    updatedAt: cat.updatedAt ?? null,
    books: Array.isArray(cat.books)
      ? cat.books.map(b => ({ id: b.id, title: b.title }))
      : null,
  };
}

function mapCategoryPage(data) {
  return {
    content: Array.isArray(data?. content) ? data.content.map(mapCategory) : [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?. totalPages ?? 0,
    size: data?.pageable?.pageSize ?? data?.size ?? 10,
    number: data?.pageable?.pageNumber ?? data?.number ?? 0,
  };
}

const adminCategoryService = {
  /**
   * GET /admin/categories
   */
  async getCategories({ page = 0, size = 10, sortBy = 'createdAt', sortDirection = 'DESC' } = {}) {
    try {
      const params = new URLSearchParams({
        page:  String(page),
        size: String(size),
        sortBy,
        sortDirection,
      });

      const data = await api.get(`/admin/categories?${params}`);
      return mapCategoryPage(data);
    } catch (error) {
      throw buildCategoryError(error);
    }
  },

  /**
   * GET /admin/categories/{id}
   */
  async getCategoryById(categoryId) {
    try {
      const data = await api.get(`/admin/categories/${categoryId}`);
      return mapCategory(data);
    } catch (error) {
      throw buildCategoryError(error);
    }
  },

  /**
   * POST /admin/categories
   */
  async createCategory(payload) {
    try {
      const data = await api.post('/admin/categories', payload);
      return mapCategory(data);
    } catch (error) {
      throw buildCategoryError(error);
    }
  },

  /**
   * PUT /admin/categories/{id}
   */
  async updateCategory(categoryId, payload) {
    try {
      const data = await api.put(`/admin/categories/${categoryId}`, payload);
      return mapCategory(data);
    } catch (error) {
      throw buildCategoryError(error);
    }
  },

  /**
   * DELETE /admin/categories/{id}
   */
  async deleteCategory(categoryId) {
    try {
      await api. delete(`/admin/categories/${categoryId}`);
    } catch (error) {
      throw buildCategoryError(error);
    }
  },
};

export default adminCategoryService;