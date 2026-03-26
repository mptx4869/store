import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import adminCategoryService from '../../services/adminCategoryService';
import { useToast } from '../../context/ToastContext';
import { Plus, Edit, Trash2, Eye, FolderTree } from 'lucide-react';

function AdminCategoriesPage() {
  const navigate = useNavigate();
  const toast = useToast();

  const [categories, setCategories] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // Pagination state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [size] = useState(10);

  // Sort state
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('DESC');

  useEffect(() => {
    loadCategories();
  }, [page, sortBy, sortDirection]);

  const loadCategories = async () => {
    setIsLoading(true);
    setError('');
    try {
      const result = await adminCategoryService.getCategories({
        page,
        size,
        sortBy,
        sortDirection,
      });

      setCategories(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err) {
      setError(err.message || 'Could not load category list');
      toast.error(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (categoryId, categoryName, bookCount) => {
    if (bookCount > 0) {
      toast.error(`Cannot delete category "${categoryName}" with ${bookCount} books.   Please delete books first. `);
      return;
    }

    const confirmed = window.confirm(
      `Confirm delete category "${categoryName}"?\n\nThis action cannot be undone. `
    );

    if (!confirmed) return;

    try {
      await adminCategoryService.deleteCategory(categoryId);
      toast.success(`Deleted category "${categoryName}"`);
      loadCategories();
    } catch (err) {
      toast.error(err.message || 'Could not delete category');
    }
  };

  const handleSort = (field) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === 'ASC' ? 'DESC' : 'ASC'));
    } else {
      setSortBy(field);
      setSortDirection('DESC');
    }
    setPage(0);
  };

  const getSortIcon = (field) => {
    if (sortBy !== field) return '↕️';
    return sortDirection === 'ASC' ? '↑' : '↓';
  };

  if (isLoading && categories.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading category list...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header - Reduce icon size and add flex-wrap */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex-1 min-w-0">
          <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
            <FolderTree className="w-7 h-7 flex-shrink-0" />
            <span>Category Management</span>
          </h1>
          <p className="text-gray-600 mt-1">
            Total:  {totalElements} categories
          </p>
        </div>

        <Link
          to="/admin/categories/new"
          className="btn-primary inline-flex items-center gap-2 flex-shrink-0"
        >
          <Plus className="w-5 h-5" />
          Create New Category
        </Link>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          {error}
        </div>
      )}

      {/* Table */}
      {categories.length === 0 ? (
        <div className="bg-white rounded-xl shadow-sm p-8 text-center">
          <FolderTree className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-600">No categories yet</p>
          <Link
            to="/admin/categories/new"
            className="btn-primary inline-flex items-center gap-2 mt-4"
          >
            <Plus className="w-5 h-5" />
            Create first category
          </Link>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('id')}
                  >
                    ID {getSortIcon('id')}
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('name')}
                  >
                    Category Name {getSortIcon('name')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Description
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('bookCount')}
                  >
                    Book Count {getSortIcon('bookCount')}
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('createdAt')}
                  >
                    Created Date {getSortIcon('createdAt')}
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {categories.map((category) => (
                  <tr key={category.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {category.id}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Link
                        to={`/admin/categories/${category.id}`}
                        className="text-sm font-medium text-blue-600 hover:text-blue-700"
                      >
                        {category.name}
                      </Link>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600 max-w-md truncate">
                      {category. description || '—'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        {category.bookCount}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {category.createdAt
                        ? new Date(category. createdAt).toLocaleDateString('vi-VN')
                        : '—'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/admin/categories/${category.id}`}
                          className="text-blue-600 hover: text-blue-700"
                          title="View details"
                        >
                          <Eye className="w-4 h-4" />
                        </Link>
                        <Link
                          to={`/admin/categories/${category.id}/edit`}
                          className="text-yellow-600 hover:text-yellow-700"
                          title="Edit"
                        >
                          <Edit className="w-4 h-4" />
                        </Link>
                        <button
                          onClick={() =>
                            handleDelete(category.id, category.name, category.bookCount)
                          }
                          className="text-red-600 hover:text-red-700"
                          title="Delete"
                          disabled={isLoading}
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="bg-gray-50 px-6 py-4 flex items-center justify-between border-t">
              <div className="text-sm text-gray-700">
                Page {page + 1} / {totalPages}
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => setPage(0)}
                  disabled={page === 0}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  First
                </button>
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  Next
                </button>
                <button
                  onClick={() => setPage(totalPages - 1)}
                  disabled={page >= totalPages - 1}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  Last
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default AdminCategoriesPage;