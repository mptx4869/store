import { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import adminBookService from '../../services/adminBookService';
import adminCategoryService from '../../services/adminCategoryService';
import { ImageUpload } from '../../components/admin';
import { useToast } from '../../context/ToastContext';
import { ArrowLeft, Save } from 'lucide-react';

function AdminBookEditPage() {
  const { bookId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [formData, setFormData] = useState({
    title: '',
    subtitle: '',
    description: '',
    language:  '',
    pages: '',
    publishedDate: '',
    imageUrl: null,
    basePrice: '',
    categoryIds: [],
  });

  const [categories, setCategories] = useState([]);
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [isLoadingBook, setIsLoadingBook] = useState(true);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadBook();
    loadCategories();
  }, [bookId]);

  const loadBook = async () => {
    setIsLoadingBook(true);
    setError('');
    try {
      const book = await adminBookService.getBookById(bookId);
      
      setFormData({
        title: book.title,
        subtitle: book.subtitle || '',
        description: book.description || '',
        language: book. language || '',
        pages: book.pages ?  String(book.pages) : '',
        publishedDate: book.publishedDate || '',
        imageUrl: book. imageUrl,
        basePrice: String(book.basePrice),
        categoryIds: book.categories. map((c) => c.id),
      });

      setSelectedCategories(book.categories. map((c) => c.id));
    } catch (err) {
      setError(err. message || 'Could not load book information');
      toast.error(err.message);
    } finally {
      setIsLoadingBook(false);
    }
  };

  const loadCategories = async () => {
    setIsLoadingCategories(true);
    try {
      const result = await adminCategoryService.getCategories({
        page: 0,
        size: 100,
        sortBy: 'name',
        sortDirection: 'ASC',
      });
      setCategories(result.content);
    } catch (err) {
      toast.error('Could not load category list');
      console.error(err);
    } finally {
      setIsLoadingCategories(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleCategoryToggle = (categoryId) => {
    setSelectedCategories((prev) => {
      if (prev.includes(categoryId)) {
        return prev.filter((id) => id !== categoryId);
      } else {
        return [...prev, categoryId];
      }
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validation
    if (!formData.title.trim()) {
      toast.error('Please enter book title');
      return;
    }

    if (! formData.basePrice || parseFloat(formData.basePrice) <= 0) {
      toast.error('Base price must be greater than 0');
      return;
    }

    setIsSubmitting(true);

    try {
      const payload = {
        title: formData.title.trim(),
        subtitle: formData.subtitle.trim() || null,
        description: formData.description.trim() || null,
        language:  formData.language.trim() || null,
        pages: formData.pages ?  parseInt(formData.pages) : null,
        publishedDate:  formData.publishedDate || null,
        imageUrl: formData.imageUrl || null,
        basePrice: parseFloat(formData.basePrice),
        categoryIds: selectedCategories,
      };

      await adminBookService.updateBook(bookId, payload);
      toast.success('Book updated successfully!');
      navigate(`/admin/books/${bookId}`);
    } catch (err) {
      setError(err.message || 'Could not update book');
      toast.error(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoadingBook) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading book information...</div>
      </div>
    );
  }

  if (error && ! formData.title) {
    return (
      <div className="space-y-4">
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          {error}
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
    <div className="max-w-5xl mx-auto space-y-6">
      {/* Back button */}
      <Link
        to={`/admin/books/${bookId}`}
        className="inline-flex items-center gap-2 text-blue-600 hover: text-blue-700"
      >
        <ArrowLeft className="w-5 h-5" />
        Back to book details
      </Link>

      {/* Form */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h1 className="text-3xl font-bold text-gray-800 mb-6">Edit Book</h1>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Image Upload */}
          <div>
            <ImageUpload
              onUploadSuccess={(url) =>
                setFormData((prev) => ({ ...prev, imageUrl: url }))
              }
              currentImageUrl={formData.imageUrl}
              onRemove={() => setFormData((prev) => ({ ...prev, imageUrl: null }))}
            />
          </div>

          {/* Basic Info */}
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-800 border-b pb-2">
              Basic Information
            </h2>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Title <span className="text-red-600">*</span>
              </label>
              <input
                type="text"
                name="title"
                value={formData.title}
                onChange={handleChange}
                required
                maxLength={255}
                className="input-field w-full"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Subtitle
              </label>
              <input
                type="text"
                name="subtitle"
                value={formData.subtitle}
                onChange={handleChange}
                maxLength={255}
                className="input-field w-full"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                rows={6}
                className="input-field w-full resize-none"
              />
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Language
                </label>
                <input
                  type="text"
                  name="language"
                  value={formData.language}
                  onChange={handleChange}
                  className="input-field w-full"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Pages
                </label>
                <input
                  type="number"
                  name="pages"
                  value={formData.pages}
                  onChange={handleChange}
                  min="1"
                  className="input-field w-full"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Published Date
                </label>
                <input
                  type="date"
                  name="publishedDate"
                  value={formData.publishedDate}
                  onChange={handleChange}
                  className="input-field w-full"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Base Price <span className="text-red-600">*</span>
                </label>
                <input
                  type="number"
                  name="basePrice"
                  value={formData.basePrice}
                  onChange={handleChange}
                  required
                  min="0"
                  step="0.01"
                  className="input-field w-full"
                />
              </div>
            </div>
          </div>

          {/* Categories */}
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-800 border-b pb-2">
              Categories
            </h2>

            {isLoadingCategories ?  (
              <p className="text-gray-500">Loading categories...</p>
            ) : categories.length === 0 ? (
              <p className="text-gray-500">
                No categories yet. {' '}
                <Link to="/admin/categories/new" className="text-blue-600 hover:underline">
                  Create new category
                </Link>
              </p>
            ) : (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                {categories. map((cat) => (
                  <label
                    key={cat.id}
                    className={`flex items-center gap-2 p-3 border rounded-lg cursor-pointer transition-colors ${
                      selectedCategories. includes(cat.id)
                        ? 'bg-blue-50 border-blue-500'
                        : 'hover:bg-blue-50'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={selectedCategories.includes(cat.id)}
                      onChange={() => handleCategoryToggle(cat.id)}
                      className="w-4 h-4"
                    />
                    <span className="text-sm text-gray-700">{cat.name}</span>
                  </label>
                ))}
              </div>
            )}

            {selectedCategories.length > 0 && (
              <p className="text-sm text-gray-600">
                Selected {selectedCategories.length} categories
              </p>
            )}
          </div>

          {/* SKU Notice */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-800">
              <strong>Note:</strong> Cannot edit SKUs on this page. 
              To manage SKUs, please go to the book detail page after saving.
            </p>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-4 pt-4 border-t">
            <Link to={`/admin/books/${bookId}`} className="btn-secondary">
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary inline-flex items-center gap-2"
            >
              <Save className="w-4 h-4" />
              {isSubmitting ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AdminBookEditPage;