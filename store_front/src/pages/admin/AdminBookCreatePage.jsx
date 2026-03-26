import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import adminBookService from '../../services/adminBookService';
import adminCategoryService from '../../services/adminCategoryService';
import { ImageUpload } from '../../components/admin';
import { useToast } from '../../context/ToastContext';
import { ArrowLeft, Save, Plus, Trash2 } from 'lucide-react';

function AdminBookCreatePage() {
  const navigate = useNavigate();
  const toast = useToast();

  const [formData, setFormData] = useState({
    title: '',
    subtitle: '',
    description: '',
    language: 'English',
    pages: '',
    publishedDate: '',
    imageUrl: null,
    basePrice: '',
    categoryIds: [],
  });

  const [skus, setSkus] = useState([
    {
      sku: '',
      format: 'Paperback',
      priceOverride: '',
      initialStock: '0',
      weightGrams: '',
      lengthMm: '',
      widthMm: '',
      heightMm: '',
      isDefault: true,
    },
  ]);

  const [categories, setCategories] = useState([]);
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    setIsLoadingCategories(true);
    try {
      const result = await adminCategoryService.getCategories({
        page: 0,
        size: 100, // Load all categories
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

  const handleSkuChange = (index, field, value) => {
    setSkus((prev) =>
      prev.map((sku, i) =>
        i === index ? { ...sku, [field]: value } : sku
      )
    );
  };

  const handleSetDefaultSku = (index) => {
    setSkus((prev) =>
      prev.map((sku, i) => ({
        ...sku,
        isDefault: i === index,
      }))
    );
  };

  const handleAddSku = () => {
    setSkus((prev) => [
      ...prev,
      {
        sku: '',
        format: 'Paperback',
        priceOverride: '',
        initialStock: '0',
        weightGrams: '',
        lengthMm: '',
        widthMm: '',
        heightMm: '',
        isDefault: false,
      },
    ]);
  };

  const handleRemoveSku = (index) => {
    if (skus.length === 1) {
      toast.error('Must have at least 1 SKU');
      return;
    }
    setSkus((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validation
    if (!formData.title.trim()) {
      toast.error('Please enter book title');
      return;
    }

    if (!formData. basePrice || parseFloat(formData.basePrice) <= 0) {
      toast.error('Base price must be greater than 0');
      return;
    }

    if (skus.length === 0) {
      toast.error('Must have at least 1 SKU');
      return;
    }

    const hasDefaultSku = skus.some((sku) => sku.isDefault);
    if (!hasDefaultSku) {
      toast.error('Must have 1 default SKU');
      return;
    }

    for (let i = 0; i < skus.length; i++) {
      if (!skus[i]. sku.trim()) {
        toast.error(`SKU #${i + 1}: SKU code cannot be empty`);
        return;
      }
      if (! skus[i].format.trim()) {
        toast.error(`SKU #${i + 1}: Format cannot be empty`);
        return;
      }
    }

    setIsSubmitting(true);

    try {
      const payload = {
        title: formData.title. trim(),
        subtitle: formData.subtitle.trim() || null,
        description: formData.description.trim() || null,
        language: formData.language.trim() || null,
        pages: formData. pages ?  parseInt(formData.pages) : null,
        publishedDate: formData.publishedDate || null,
        imageUrl: formData.imageUrl || null,
        basePrice: parseFloat(formData.basePrice),
        categoryIds: selectedCategories,
        skus: skus.map((sku) => ({
          sku: sku.sku. trim(),
          format: sku.format.trim(),
          priceOverride: sku.priceOverride ?  parseFloat(sku.priceOverride) : null,
          initialStock: sku.initialStock ? parseInt(sku.initialStock) : 0,
          weightGrams:  sku.weightGrams ? parseInt(sku.weightGrams) : null,
          lengthMm: sku.lengthMm ? parseInt(sku.lengthMm) : null,
          widthMm: sku.widthMm ? parseInt(sku.widthMm) : null,
          heightMm: sku.heightMm ?  parseInt(sku.heightMm) : null,
          isDefault: sku.isDefault,
        })),
      };

      const created = await adminBookService.createBook(payload);
      toast.success('Book created successfully!');
      navigate(`/admin/books/${created.id}`);
    } catch (err) {
      setError(err.message || 'Could not create book');
      toast.error(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      {/* Back button */}
      <Link
        to="/admin/books"
        className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700"
      >
        <ArrowLeft className="w-5 h-5" />
        Back to book list
      </Link>

      {/* Form */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h1 className="text-3xl font-bold text-gray-800 mb-6">Add New Book</h1>

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

            {isLoadingCategories ? (
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
                    className="flex items-center gap-2 p-3 border rounded-lg cursor-pointer hover: bg-blue-50 transition-colors"
                  >
                    <input
                      type="checkbox"
                      checked={selectedCategories.includes(cat.id)}
                      onChange={() => handleCategoryToggle(cat.id)}
                      className="w-4 h-4"
                    />
                    <span className="text-sm text-gray-700">{cat. name}</span>
                  </label>
                ))}
              </div>
            )}
          </div>

          {/* SKUs */}
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b pb-2">
              <h2 className="text-xl font-semibold text-gray-800">
                SKUs <span className="text-red-600">*</span>
              </h2>
              <button
                type="button"
                onClick={handleAddSku}
                className="btn-secondary inline-flex items-center gap-2 text-sm"
              >
                <Plus className="w-4 h-4" />
                Add SKU
              </button>
            </div>

            {skus.map((sku, index) => (
              <div
                key={index}
                className="p-4 border rounded-lg bg-gray-50 space-y-3"
              >
                <div className="flex items-center justify-between mb-2">
                  <h3 className="font-semibold text-gray-800">SKU #{index + 1}</h3>
                  <div className="flex items-center gap-2">
                    <label className="flex items-center gap-2 text-sm">
                      <input
                        type="radio"
                        checked={sku.isDefault}
                        onChange={() => handleSetDefaultSku(index)}
                        className="w-4 h-4"
                      />
                      <span className="text-gray-700">Default</span>
                    </label>
                    {skus.length > 1 && (
                      <button
                        type="button"
                        onClick={() => handleRemoveSku(index)}
                        className="text-red-600 hover:text-red-700"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      SKU Code <span className="text-red-600">*</span>
                    </label>
                    <input
                      type="text"
                      value={sku. sku}
                      onChange={(e) =>
                        handleSkuChange(index, 'sku', e.target.value)
                      }
                      placeholder="BOOK-001-PB"
                      required
                      className="input-field w-full text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Format <span className="text-red-600">*</span>
                    </label>
                    <input
                      type="text"
                      value={sku. format}
                      onChange={(e) =>
                        handleSkuChange(index, 'format', e.target.value)
                      }
                      placeholder="Paperback"
                      required
                      className="input-field w-full text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Custom Price (optional)
                    </label>
                    <input
                      type="number"
                      value={sku. priceOverride}
                      onChange={(e) =>
                        handleSkuChange(index, 'priceOverride', e.target.value)
                      }
                      placeholder="Leave empty = use base price"
                      min="0"
                      step="0.01"
                      className="input-field w-full text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Initial Stock
                    </label>
                    <input
                      type="number"
                      value={sku.initialStock}
                      onChange={(e) =>
                        handleSkuChange(index, 'initialStock', e.target.value)
                      }
                      min="0"
                      className="input-field w-full text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Weight (g)
                    </label>
                    <input
                      type="number"
                      value={sku.weightGrams}
                      onChange={(e) =>
                        handleSkuChange(index, 'weightGrams', e.target.value)
                      }
                      min="0"
                      className="input-field w-full text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Length (mm)
                    </label>
                    <input
                      type="number"
                      value={sku.lengthMm}
                      onChange={(e) =>
                        handleSkuChange(index, 'lengthMm', e.target.value)
                      }
                      min="0"
                      className="input-field w-full text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Width (mm)
                    </label>
                    <input
                      type="number"
                      value={sku. widthMm}
                      onChange={(e) =>
                        handleSkuChange(index, 'widthMm', e. target.value)
                      }
                      min="0"
                      className="input-field w-full text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Height (mm)
                    </label>
                    <input
                      type="number"
                      value={sku.heightMm}
                      onChange={(e) =>
                        handleSkuChange(index, 'heightMm', e. target.value)
                      }
                      min="0"
                      className="input-field w-full text-sm"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-4 pt-4 border-t">
            <Link to="/admin/books" className="btn-secondary">
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary inline-flex items-center gap-2"
            >
              <Save className="w-4 h-4" />
              {isSubmitting ? 'Creating...' : 'Create Book'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AdminBookCreatePage;