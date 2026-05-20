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

  // Pagination state (cursor)
  const defaultCursor = { lastId: null, lastCreatedAt: null };
  const [pageIndex, setPageIndex] = useState(0);
  const [cursorStack, setCursorStack] = useState([defaultCursor]);
  const [hasNext, setHasNext] = useState(false);
  const [size] = useState(20);

  // Filter state
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [deletedOnly, setDeletedOnly] = useState(false);
  const [searchId, setSearchId] = useState('');
  const [searchTitle, setSearchTitle] = useState('');
  const [searchIdInput, setSearchIdInput] = useState('');
  const [searchTitleInput, setSearchTitleInput] = useState('');

  useEffect(() => {
    loadBooks();
  }, [pageIndex, includeDeleted, deletedOnly, searchId, searchTitle]);

  const resetPagination = () => {
    setPageIndex(0);
    setCursorStack([defaultCursor]);
  };

  const loadBooks = async () => {
    setIsLoading(true);
    setError('');
    try {
      const trimmedId = searchId.trim();
      const trimmedTitle = searchTitle.trim();
      const parsedId = trimmedId ? Number(trimmedId) : undefined;
      const idFilter = Number.isNaN(parsedId) ? undefined : parsedId;
      const titleFilter = trimmedTitle || undefined;

      const cursor = cursorStack[pageIndex] || defaultCursor;

      const result = await adminBookService.getBooksCursor({
        size,
        lastId: cursor.lastId ?? undefined,
        lastCreatedAt: cursor.lastCreatedAt ?? undefined,
        includeDeleted,
        deletedOnly,
        id: idFilter,
        title: titleFilter,
      });

      setBooks(result.content);
      const canGoNext = !!result.hasNext && result.nextLastId && result.nextLastCreatedAt;
      setHasNext(canGoNext);
      setCursorStack((prev) => {
        const next = prev.slice(0, pageIndex + 1);
        if (canGoNext) {
          next[pageIndex + 1] = {
            lastId: result.nextLastId,
            lastCreatedAt: result.nextLastCreatedAt,
          };
        }
        return next;
      });
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

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    const trimmedId = searchIdInput.trim();
    const trimmedTitle = searchTitleInput.trim();
    setSearchId(trimmedId);
    setSearchTitle(trimmedTitle);
    setSearchIdInput(trimmedId);
    setSearchTitleInput(trimmedTitle);
    resetPagination();
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
            Showing: {books.length} books (newest first)
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
      <div className="bg-white rounded-xl shadow-sm p-4 space-y-4">
        <form className="grid sm:grid-cols-4 gap-3" onSubmit={handleSearchSubmit}>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Book ID</label>
            <input
              type="number"
              value={searchIdInput}
              onChange={(e) => setSearchIdInput(e.target.value)}
              className="input-field w-full"
              placeholder="Exact ID"
            />
          </div>
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
            <input
              type="text"
              value={searchTitleInput}
              onChange={(e) => setSearchTitleInput(e.target.value)}
              className="input-field w-full"
              placeholder="Search by title"
            />
          </div>
          <div className="flex items-end">
            <button className="btn-primary w-full" type="submit">
              Search
            </button>
          </div>
        </form>

        <div className="flex flex-wrap gap-6">
          <label className="inline-flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={includeDeleted}
              onChange={(e) => {
                setIncludeDeleted(e.target.checked);
                if (e.target.checked) setDeletedOnly(false);
                resetPagination();
              }}
              className="w-4 h-4"
            />
            <span className="text-sm text-gray-700">Show deleted books</span>
          </label>

          <label className="inline-flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={deletedOnly}
              onChange={(e) => {
                setDeletedOnly(e.target.checked);
                if (e.target.checked) setIncludeDeleted(false);
                resetPagination();
              }}
              className="w-4 h-4"
            />
            <span className="text-sm text-gray-700">Only deleted books</span>
          </label>
        </div>
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
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Title
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Category
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Base Price
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    SKUs
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Created Date
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
          {(pageIndex > 0 || hasNext) && (
            <div className="bg-gray-50 px-6 py-4 flex items-center justify-between border-t">
              <div className="text-sm text-gray-700">
                Page {pageIndex + 1}
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => setPageIndex((p) => Math.max(0, p - 1))}
                  disabled={pageIndex === 0}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPageIndex((p) => p + 1)}
                  disabled={!hasNext}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  Next
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