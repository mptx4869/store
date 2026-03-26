import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import adminCategoryService from '../../services/adminCategoryService';
import { useToast } from '../../context/ToastContext';
import { ArrowLeft, Save } from 'lucide-react';

function AdminCategoryFormPage() {
  const { categoryId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const isEditMode = !!categoryId;

  const [formData, setFormData] = useState({
    name: '',
    description: '',
  });

  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingData, setIsLoadingData] = useState(isEditMode);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isEditMode) {
      loadCategory();
    }
  }, [categoryId]);

  const loadCategory = async () => {
    setIsLoadingData(true);
    setError('');
    try {
      const data = await adminCategoryService.getCategoryById(categoryId);
      setFormData({
        name: data.name,
        description: data.description || '',
      });
    } catch (err) {
      setError(err.message || 'Unable to load category information');
      toast.error(err.message);
    } finally {
      setIsLoadingData(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!formData.name.trim()) {
      toast.error('Please enter category name');
      return;
    }

    if (formData.name.length > 100) {
      toast.error('Category name must not exceed 100 characters');
      return;
    }

    if (formData.description. length > 1000) {
      toast.error('Description must not exceed 1000 characters');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const payload = {
        name: formData.name. trim(),
        description: formData.description.trim() || null,
      };

      if (isEditMode) {
        await adminCategoryService.updateCategory(categoryId, payload);
        toast.success('Category updated successfully');
        navigate(`/admin/categories/${categoryId}`);
      } else {
        const created = await adminCategoryService.createCategory(payload);
        toast.success('Category created successfully');
        navigate(`/admin/categories/${created.id}`);
      }
    } catch (err) {
      setError(err.message || 'Unable to save category');
      toast.error(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoadingData) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading category information...</div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Back button */}
      <Link
        to={isEditMode ? `/admin/categories/${categoryId}` : '/admin/categories'}
        className="inline-flex items-center gap-2 text-blue-600 hover: text-blue-700"
      >
        <ArrowLeft className="w-5 h-5" />
        {isEditMode ? 'Back to category detail' :  'Back to list'}
      </Link>

      {/* Form */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h1 className="text-3xl font-bold text-gray-800 mb-6">
          {isEditMode ? 'Edit Category' : 'Create New Category'}
        </h1>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Category Name <span className="text-red-600">*</span>
            </label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="Example: Fiction, Programming, Science..."
              maxLength={100}
              required
              className="input-field w-full"
              disabled={isLoading}
            />
            <p className="text-xs text-gray-500 mt-1">
              {formData.name.length}/100 characters
            </p>
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description (optional)
            </label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="Description about this category..."
              maxLength={1000}
              rows={5}
              className="input-field w-full resize-none"
              disabled={isLoading}
            />
            <p className="text-xs text-gray-500 mt-1">
              {formData.description.length}/1000 characters
            </p>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-4 pt-4 border-t">
            <Link
              to={isEditMode ? `/admin/categories/${categoryId}` : '/admin/categories'}
              className="btn-secondary"
            >
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary inline-flex items-center gap-2"
            >
              <Save className="w-4 h-4" />
              {isLoading
                ? 'Saving...'
                : isEditMode
                ? 'Update'
                : 'Create Category'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AdminCategoryFormPage;