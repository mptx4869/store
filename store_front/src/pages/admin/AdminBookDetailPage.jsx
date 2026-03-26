import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import adminBookService from '../../services/adminBookService';
import { ImageDisplay } from '../../components/admin';
import SkuFormModal from '../../components/admin/SkuFormModal';
import { useToast } from '../../context/ToastContext';
import { 
  ArrowLeft, 
  Edit, 
  Trash2, 
  RotateCcw, 
  BookOpen, 
  Tag,
  Plus,
  Star,
  StarOff
} from 'lucide-react';
import { formatCurrency } from '../../utils/format';

function AdminBookDetailPage() {
  const { bookId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [book, setBook] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);

  // SKU Modal state
  const [isSkuModalOpen, setIsSkuModalOpen] = useState(false);
  const [editingSku, setEditingSku] = useState(null);
  const [isSkuEdit, setIsSkuEdit] = useState(false);

  useEffect(() => {
    loadBook();
  }, [bookId]);

  const loadBook = async () => {
    setIsLoading(true);
    setError('');
    try {
      const data = await adminBookService.getBookById(bookId);
      setBook(data);
    } catch (err) {
      setError(err.message || 'Could not load book information');
      toast.error(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (book.deletedAt) {
      toast.error('Book has already been deleted');
      return;
    }

    const confirmed = window.confirm(
      `Confirm delete book "${book.title}"?\n\nThis is a soft delete, can be restored later. `
    );

    if (!confirmed) return;

    setIsDeleting(true);
    try {
      await adminBookService. deleteBook(bookId, false);
      toast.success(`Deleted book "${book.title}"`);
      loadBook();
    } catch (err) {
      toast.error(err.message || 'Could not delete book');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleRestore = async () => {
    const confirmed = window.confirm(`Restore book "${book.title}"? `);
    if (!confirmed) return;

    setIsDeleting(true);
    try {
      await adminBookService.restoreBook(bookId);
      toast.success(`Restored book "${book.title}"`);
      loadBook();
    } catch (err) {
      toast.error(err.message || 'Could not restore book');
    } finally {
      setIsDeleting(false);
    }
  };

  // SKU Handlers
  const handleAddSku = () => {
    setIsSkuEdit(false);
    setEditingSku(null);
    setIsSkuModalOpen(true);
  };

  const handleEditSku = (sku) => {
    setIsSkuEdit(true);
    setEditingSku(sku);
    setIsSkuModalOpen(true);
  };

  const handleSkuSubmit = async (payload) => {
    try {
      if (isSkuEdit && editingSku) {
        await adminBookService.updateSku(bookId, editingSku.id, payload);
        toast.success('SKU updated successfully');
      } else {
        await adminBookService.addSku(bookId, payload);
        toast.success('SKU added successfully');
      }
      loadBook(); // Refresh book data
    } catch (err) {
      toast.error(err. message || 'Could not save SKU');
      throw err; // Let modal handle the error
    }
  };

  const handleSetDefaultSku = async (skuId) => {
    const confirmed = window.confirm('Set this SKU as default?');
    if (!confirmed) return;

    try {
      await adminBookService. setDefaultSku(bookId, skuId);
      toast.success('Set default SKU successfully');
      loadBook();
    } catch (err) {
      toast.error(err.message || 'Could not set default SKU');
    }
  };

  const handleDeleteSku = async (skuId, skuCode) => {
    const confirmed = window.confirm(
      `Confirm delete SKU "${skuCode}"?\n\nThis action cannot be undone.`
    );
    if (!confirmed) return;

    try {
      await adminBookService.deleteSku(bookId, skuId);
      toast.success(`Deleted SKU "${skuCode}"`);
      loadBook();
    } catch (err) {
      toast.error(err.message || 'Could not delete SKU');
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading book information...</div>
      </div>
    );
  }

  if (error || ! book) {
    return (
      <div className="space-y-4">
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          {error || 'Book not found'}
        </div>
        <Link
          to="/admin/books"
          className="btn-secondary inline-flex items-center gap-2"
        >
          <ArrowLeft className="w-5 h-5" />
          Back to list
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Back button */}
      <Link
        to="/admin/books"
        className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700"
      >
        <ArrowLeft className="w-5 h-5" />
        Back to book list
      </Link>

      {/* Deleted Banner */}
      {book.deletedAt && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4 flex items-center justify-between">
          <span>
            This book was deleted on {new Date(book.deletedAt).toLocaleString('en-US')}
          </span>
          <button
            onClick={handleRestore}
            disabled={isDeleting}
            className="btn-secondary inline-flex items-center gap-2"
          >
            <RotateCcw className="w-4 h-4" />
            Restore
          </button>
        </div>
      )}

      {/* Header with Image */}
      <div className="grid lg:grid-cols-3 gap-6">
        {/* Left:  Image */}
        <div>
          <ImageDisplay
            imageUrl={book.imageUrl}
            alt={book.title}
            size="large"
          />
        </div>

        {/* Right: Info */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-start justify-between mb-4">
            <div className="flex-1">
              <h1 className="text-3xl font-bold text-gray-800">{book.title}</h1>
              {book.subtitle && (
                <p className="text-lg text-gray-600 mt-2">{book.subtitle}</p>
              )}
            </div>

            <div className="flex items-center gap-2">
              {! book.deletedAt && (
                <>
                  <Link
                    to={`/admin/books/${bookId}/edit`}
                    className="btn-secondary inline-flex items-center gap-2"
                  >
                    <Edit className="w-4 h-4" />
                    Edit
                  </Link>
                  <button
                    onClick={handleDelete}
                    disabled={isDeleting}
                    className="btn-danger inline-flex items-center gap-2"
                  >
                    <Trash2 className="w-4 h-4" />
                    {isDeleting ? 'Deleting...' : 'Delete'}
                  </button>
                </>
              )}
            </div>
          </div>

          {/* Categories */}
          {book.categories. length > 0 && (
            <div className="mb-6 pb-6 border-b">
              <h3 className="text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                <Tag className="w-4 h-4" />
                Categories
              </h3>
              <div className="flex flex-wrap gap-2">
                {book.categories.map((cat) => (
                  <Link
                    key={cat.id}
                    to={`/admin/categories/${cat.id}`}
                    className="inline-block px-3 py-1 text-sm bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200"
                  >
                    {cat.name}
                  </Link>
                ))}
              </div>
            </div>
          )}

          {/* Metadata Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-6 pb-6 border-b">
            <div>
              <p className="text-sm text-gray-500">ID</p>
              <p className="text-lg font-semibold text-gray-800">{book.id}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Base Price</p>
              <p className="text-lg font-semibold text-red-600">
                {formatCurrency(book.basePrice)}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Language</p>
              <p className="text-lg font-semibold text-gray-800">
                {book.language || '—'}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Pages</p>
              <p className="text-lg font-semibold text-gray-800">
                {book.pages || '—'}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Published Date</p>
              <p className="text-lg font-semibold text-gray-800">
                {book.publishedDate || '—'}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Created At</p>
              <p className="text-lg font-semibold text-gray-800">
                {book.createdAt
                  ? new Date(book. createdAt).toLocaleDateString('en-US')
                  : '—'}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Last Updated</p>
              <p className="text-lg font-semibold text-gray-800">
                {book.updatedAt
                  ? new Date(book.updatedAt).toLocaleDateString('en-US')
                  : '—'}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500">SKUs</p>
              <p className="text-lg font-semibold text-gray-800">
                {book.skus. length}
              </p>
            </div>
          </div>

          {/* Description */}
          <div>
            <h3 className="text-lg font-semibold text-gray-800 mb-2">Description</h3>
            <p className="text-gray-700 whitespace-pre-line">
              {book.description || 'No description yet'}
            </p>
          </div>
        </div>
      </div>

      {/* SKUs Section */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-800">
            SKUs ({book.skus.length})
          </h2>
          {! book.deletedAt && (
            <button
              onClick={handleAddSku}
              className="btn-primary inline-flex items-center gap-2 text-sm"
            >
              <Plus className="w-4 h-4" />
              Add SKU
            </button>
          )}
        </div>

        {book.skus. length === 0 ? (
          <div className="text-center py-8">
            <p className="text-gray-500 mb-4">No SKUs yet</p>
            {!book.deletedAt && (
              <button
                onClick={handleAddSku}
                className="btn-primary inline-flex items-center gap-2"
              >
                <Plus className="w-4 h-4" />
                Add first SKU
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    SKU
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Format
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Price
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Stock
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Reserved
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Available
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Default
                  </th>
                  {! book.deletedAt && (
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                      Actions
                    </th>
                  )}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {book.skus. map((sku) => (
                  <tr key={sku.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-mono text-gray-800">
                      {sku. sku}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      {sku. format}
                    </td>
                    <td className="px-4 py-3 text-sm font-semibold text-gray-800">
                      {formatCurrency(sku.price)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      {sku.stock}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      {sku.reserved}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      {sku.available}
                    </td>
                    <td className="px-4 py-3">
                      {sku.isDefault ?  (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs bg-green-100 text-green-800 rounded">
                          <Star className="w-3 h-3" />
                          Default
                        </span>
                      ) : (
                        ! book.deletedAt && (
                          <button
                            onClick={() => handleSetDefaultSku(sku.id)}
                            className="text-xs text-gray-500 hover:text-green-600 inline-flex items-center gap-1"
                          >
                            <StarOff className="w-3 h-3" />
                            Set default
                          </button>
                        )
                      )}
                    </td>
                    {!book.deletedAt && (
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleEditSku(sku)}
                            className="text-yellow-600 hover:text-yellow-700"
                            title="Edit"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDeleteSku(sku.id, sku. sku)}
                            className="text-red-600 hover: text-red-700"
                            title="Delete"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* SKU Form Modal */}
      <SkuFormModal
        isOpen={isSkuModalOpen}
        onClose={() => setIsSkuModalOpen(false)}
        onSubmit={handleSkuSubmit}
        sku={editingSku}
        isEdit={isSkuEdit}
      />
    </div>
  );
}

export default AdminBookDetailPage;