import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import adminCategoryService from '../../services/adminCategoryService';
import { useToast } from '../../context/ToastContext';
import { ArrowLeft, Edit, Trash2, BookOpen, FolderTree } from 'lucide-react';

function AdminCategoryDetailPage() {
  const { categoryId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [category, setCategory] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    loadCategory();
  }, [categoryId]);

  const loadCategory = async () => {
    setIsLoading(true);
    setError('');
    try {
      const data = await adminCategoryService.getCategoryById(categoryId);
      setCategory(data);
    } catch (err) {
      setError(err.message || 'Could not load category information');
      toast.error(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (category. bookCount > 0) {
      toast.error(
        `Cannot delete category with ${category.bookCount} books. Please delete books first.`
      );
      return;
    }

    const confirmed = window.confirm(
      `Confirm delete category "${category.name}"?\n\nThis action cannot be undone.`
    );

    if (!confirmed) return;

    setIsDeleting(true);
    try {
      await adminCategoryService.deleteCategory(categoryId);
      toast.success(`Deleted category "${category.name}"`);
      navigate('/admin/categories');
    } catch (err) {
      toast.error(err.message || 'Could not delete category');
    } finally {
      setIsDeleting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading category information...</div>
      </div>
    );
  }

  if (error || !category) {
    return (
      <div className="space-y-4">
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          {error || 'Category not found'}
        </div>
        <Link
          to="/admin/categories"
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
        to="/admin/categories"
        className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700"
      >
        <ArrowLeft className="w-5 h-5" />
        Back to category list
      </Link>

      {/* Header */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-start justify-between">
          <div className="flex items-start gap-4">
            <div className="p-3 bg-blue-100 rounded-lg">
              <FolderTree className="w-8 h-8 text-blue-600" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-gray-800">{category.name}</h1>
              <p className="text-gray-600 mt-1">{category.description || 'No description'}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Link
              to={`/admin/categories/${categoryId}/edit`}
              className="btn-secondary inline-flex items-center gap-2"
            >
              <Edit className="w-4 h-4" />
              Edit
            </Link>
            <button
              onClick={handleDelete}
              disabled={isDeleting || category.bookCount > 0}
              className="btn-danger inline-flex items-center gap-2 disabled:opacity-50"
              title={
                category.bookCount > 0
                  ? 'Cannot delete category with books'
                  : 'Delete category'
              }
            >
              <Trash2 className="w-4 h-4" />
              {isDeleting ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </div>

        {/* Metadata */}
        <div className="grid grid-cols-3 gap-6 mt-6 pt-6 border-t">
          <div>
            <p className="text-sm text-gray-500">ID</p>
            <p className="text-lg font-semibold text-gray-800">{category.id}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Book Count</p>
            <p className="text-lg font-semibold text-gray-800">{category.bookCount}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Created Date</p>
            <p className="text-lg font-semibold text-gray-800">
              {category.createdAt
                ? new Date(category.createdAt).toLocaleDateString('en-US')
                : '—'}
            </p>
          </div>
        </div>
      </div>

      {/* Books in this category */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h2 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
          <BookOpen className="w-6 h-6" />
          Books in this category
        </h2>

        {! category.books || category.books. length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            No books in this category yet
          </div>
        ) : (
          <div className="space-y-2">
            {category.books. map((book) => (
              <Link
                key={book.id}
                to={`/admin/books/${book.id}`}
                className="block p-4 border border-gray-200 rounded-lg hover:bg-blue-50 hover:border-blue-300 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <BookOpen className="w-5 h-5 text-gray-400" />
                  <div className="flex-1">
                    <p className="font-medium text-gray-800">{book.title}</p>
                    <p className="text-sm text-gray-500">ID: {book.id}</p>
                  </div>
                  <span className="text-blue-600 text-sm">View details →</span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default AdminCategoryDetailPage;