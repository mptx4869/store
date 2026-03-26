import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import adminBookService from '../../services/adminBookService';
import { ImageDisplay } from '../../components/admin';
import { useToast } from '../../context/ToastContext';
import { Plus, Edit, Trash2, Eye, BookOpen, RotateCcw } from 'lucide-react';
import { formatCurrency } from '../../utils/format';

function AdminBooksPage() {
  const navigate = useNavigate();
  const toast = useToast();

  const [books, setBooks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // Pagination state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [size] = useState(10);

  // Sort & filter state
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('DESC');
  const [includeDeleted, setIncludeDeleted] = useState(false);

  useEffect(() => {
    loadBooks();
  }, [page, sortBy, sortDirection, includeDeleted]);

  const loadBooks = async () => {
    setIsLoading(true);
    setError('');
    try {
      const result = await adminBookService.getBooks({
        page,
        size,
        sortBy,
        sortDirection,
        includeDeleted,
      });

      setBooks(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err) {
      setError(err.message || 'Could not load book list');
      toast.error(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (bookId, bookTitle, isDeleted) => {
    if (isDeleted) {
      toast.error('Book was already deleted');
      return;
    }

    const confirmed = window.confirm(
      `Confirm delete book "${bookTitle}"?\n\nThis is a soft delete and can be restored later. `
    );

    if (!confirmed) return;

    try {
      await adminBookService.deleteBook(bookId, false); // soft delete
      toast.success(`Deleted book "${bookTitle}"`);
      loadBooks(); // Refresh list
    } catch (err) {
      toast.error(err. message || 'Could not delete book');
    }
  };

  const handleRestore = async (bookId, bookTitle) => {
    const confirmed = window.confirm(`Restore book "${bookTitle}"? `);
    if (!confirmed) return;

    try {
      await adminBookService.restoreBook(bookId);
      toast.success(`Restored book "${bookTitle}"`);
      loadBooks();
    } catch (err) {
      toast.error(err.message || 'Could not restore book');
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
    return sortDirection === 'ASC' ? '↑' :  '↓';
  };

  if (isLoading && books.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading book list...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-3">
            <BookOpen className="w-8 h-8" />
            Book Management
          </h1>
          <p className="text-gray-600 mt-1">
            Total:  {totalElements} books
          </p>
        </div>

        <Link
          to="/admin/books/new"
          className="btn-primary inline-flex items-center gap-2"
        >
          <Plus className="w-5 h-5" />
          Add New Book
        </Link>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm p-4">
        <label className="inline-flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={includeDeleted}
            onChange={(e) => {
              setIncludeDeleted(e.target.checked);
              setPage(0);
            }}
            className="w-4 h-4"
          />
          <span className="text-sm text-gray-700">Show deleted books</span>
        </label>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          {error}
        </div>
      )}

      {/* Table */}
      {books.length === 0 ? (
        <div className="bg-white rounded-xl shadow-sm p-8 text-center">
          <BookOpen className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-600">No books yet</p>
          <Link
            to="/admin/books/new"
            className="btn-primary inline-flex items-center gap-2 mt-4"
          >
            <Plus className="w-5 h-5" />
            Add First Book
          </Link>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Image
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('id')}
                  >
                    ID {getSortIcon('id')}
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('title')}
                  >
                    Title {getSortIcon('title')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Category
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('basePrice')}
                  >
                    Base Price {getSortIcon('basePrice')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    SKUs
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('createdAt')}
                  >
                    Created Date {getSortIcon('createdAt')}
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {books.map((book) => (
                  <tr
                    key={book.id}
                    className={`hover:bg-gray-50 ${
                      book.deletedAt ? 'bg-red-50 opacity-60' : ''
                    }`}
                  >
                    {/* Image */}
                    <td className="px-6 py-4">
                      <ImageDisplay
                        imageUrl={book.imageUrl}
                        alt={book.title}
                        size="small"
                      />
                    </td>

                    {/* ID */}
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {book.id}
                    </td>

                    {/* Title */}
                    <td className="px-6 py-4">
                      <Link
                        to={`/admin/books/${book.id}`}
                        className="text-sm font-medium text-blue-600 hover:text-blue-700 line-clamp-2"
                      >
                        {book.title}
                      </Link>
                      {book.deletedAt && (
                        <span className="inline-block mt-1 px-2 py-0.5 text-xs bg-red-200 text-red-800 rounded">
                          Deleted
                        </span>
                      )}
                    </td>

                    {/* Categories */}
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap gap-1">
                        {book. categories.length === 0 ? (
                          <span className="text-xs text-gray-400">—</span>
                        ) : (
                          book.categories. map((cat) => (
                            <Link
                              key={cat.id}
                              to={`/admin/categories/${cat.id}`}
                              className="inline-block px-2 py-0.5 text-xs bg-blue-100 text-blue-800 rounded hover:bg-blue-200"
                            >
                              {cat.name}
                            </Link>
                          ))
                        )}
                      </div>
                    </td>

                    {/* Base Price */}
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatCurrency(book.basePrice)}
                    </td>

                    {/* SKUs */}
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {book.skus.length} SKU(s)
                    </td>

                    {/* Created At */}
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {book.createdAt
                        ?  new Date(book.createdAt).toLocaleDateString('vi-VN')
                        : '—'}
                    </td>

                    {/* Actions */}
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/admin/books/${book.id}`}
                          className="text-blue-600 hover: text-blue-700"
                          title="View details"
                        >
                          <Eye className="w-4 h-4" />
                        </Link>

                        {! book.deletedAt && (
                          <>
                            <Link
                              to={`/admin/books/${book.id}/edit`}
                              className="text-yellow-600 hover:text-yellow-700"
                              title="Edit"
                            >
                              <Edit className="w-4 h-4" />
                            </Link>
                            <button
                              onClick={() =>
                                handleDelete(book.id, book.title, !! book.deletedAt)
                              }
                              className="text-red-600 hover:text-red-700"
                              title="Delete"
                              disabled={isLoading}
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </>
                        )}

                        {book.deletedAt && (
                          <button
                            onClick={() => handleRestore(book.id, book.title)}
                            className="text-green-600 hover:text-green-700"
                            title="Restore"
                            disabled={isLoading}
                          >
                            <RotateCcw className="w-4 h-4" />
                          </button>
                        )}
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

export default AdminBooksPage;