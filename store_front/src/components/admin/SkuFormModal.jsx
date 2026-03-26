import { useState, useEffect } from 'react';
import { X, Save } from 'lucide-react';

function SkuFormModal({ isOpen, onClose, onSubmit, sku, isEdit }) {
  const [formData, setFormData] = useState({
    sku: '',
    format: 'Paperback',
    priceOverride: '',
    initialStock: '0',
    weightGrams: '',
    lengthMm: '',
    widthMm: '',
    heightMm: '',
    isDefault: false,
  });

  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isEdit && sku) {
      setFormData({
        sku: sku. sku || '',
        format: sku. format || 'Paperback',
        priceOverride: sku.price ?  String(sku.price) : '',
        initialStock: '0', // Not editable in edit mode
        weightGrams: sku.weightGrams ?  String(sku.weightGrams) : '',
        lengthMm: sku.lengthMm ? String(sku.lengthMm) : '',
        widthMm: sku. widthMm ? String(sku.widthMm) : '',
        heightMm: sku.heightMm ? String(sku.heightMm) : '',
        isDefault: sku.isDefault || false,
      });
    } else {
      // Reset for create mode
      setFormData({
        sku: '',
        format: 'Paperback',
        priceOverride: '',
        initialStock: '0',
        weightGrams: '',
        lengthMm: '',
        widthMm: '',
        heightMm: '',
        isDefault: false,
      });
    }
  }, [isEdit, sku, isOpen]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!formData.sku. trim()) {
      alert('Please enter SKU code');
      return;
    }

    if (! formData.format.trim()) {
      alert('Please enter format');
      return;
    }

    setIsSubmitting(true);

    try {
      const payload = {
        sku: formData.sku. trim(),
        format: formData.format.trim(),
        priceOverride: formData.priceOverride
          ? parseFloat(formData. priceOverride)
          : null,
        weightGrams: formData.weightGrams ? parseInt(formData.weightGrams) : null,
        lengthMm: formData.lengthMm ? parseInt(formData.lengthMm) : null,
        widthMm: formData.widthMm ? parseInt(formData.widthMm) : null,
        heightMm: formData.heightMm ?  parseInt(formData.heightMm) : null,
      };

      // Only include initialStock and isDefault for create mode
      if (! isEdit) {
        payload.initialStock = formData.initialStock
          ? parseInt(formData.initialStock)
          : 0;
        payload.isDefault = formData.isDefault;
      }

      await onSubmit(payload);
      onClose();
    } catch (err) {
      console.error('SKU form error:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b sticky top-0 bg-white">
          <h2 className="text-2xl font-bold text-gray-800">
            {isEdit ?  'Edit SKU' : 'Add New SKU'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            disabled={isSubmitting}
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* SKU & Format */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                SKU Code <span className="text-red-600">*</span>
              </label>
              <input
                type="text"
                name="sku"
                value={formData. sku}
                onChange={handleChange}
                placeholder="BOOK-001-PB"
                required
                maxLength={100}
                className="input-field w-full"
                disabled={isSubmitting}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Format <span className="text-red-600">*</span>
              </label>
              <input
                type="text"
                name="format"
                value={formData.format}
                onChange={handleChange}
                placeholder="Paperback, Hardcover, eBook..."
                required
                maxLength={100}
                className="input-field w-full"
                disabled={isSubmitting}
              />
            </div>
          </div>

          {/* Price Override & Initial Stock */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Custom Price (optional)
              </label>
              <input
                type="number"
                name="priceOverride"
                value={formData.priceOverride}
                onChange={handleChange}
                placeholder="Leave empty = use base price"
                min="0"
                step="0.01"
                className="input-field w-full"
                disabled={isSubmitting}
              />
              <p className="text-xs text-gray-500 mt-1">
                If left empty, will use the book's base price
              </p>
            </div>

            {! isEdit && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Initial Stock
                </label>
                <input
                  type="number"
                  name="initialStock"
                  value={formData.initialStock}
                  onChange={handleChange}
                  min="0"
                  className="input-field w-full"
                  disabled={isSubmitting}
                />
                <p className="text-xs text-gray-500 mt-1">
                  Quantity of books in stock when creating SKU
                </p>
              </div>
            )}
          </div>

          {/* Dimensions */}
          <div>
            <h3 className="text-sm font-semibold text-gray-800 mb-3">
              Dimensions & Weight (optional)
            </h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Weight (g)
                </label>
                <input
                  type="number"
                  name="weightGrams"
                  value={formData.weightGrams}
                  onChange={handleChange}
                  min="0"
                  placeholder="500"
                  className="input-field w-full text-sm"
                  disabled={isSubmitting}
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Length (mm)
                </label>
                <input
                  type="number"
                  name="lengthMm"
                  value={formData.lengthMm}
                  onChange={handleChange}
                  min="0"
                  placeholder="240"
                  className="input-field w-full text-sm"
                  disabled={isSubmitting}
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Width (mm)
                </label>
                <input
                  type="number"
                  name="widthMm"
                  value={formData.widthMm}
                  onChange={handleChange}
                  min="0"
                  placeholder="170"
                  className="input-field w-full text-sm"
                  disabled={isSubmitting}
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Height (mm)
                </label>
                <input
                  type="number"
                  name="heightMm"
                  value={formData.heightMm}
                  onChange={handleChange}
                  min="0"
                  placeholder="25"
                  className="input-field w-full text-sm"
                  disabled={isSubmitting}
                />
              </div>
            </div>
          </div>

          {/* Is Default (only for create) */}
          {!isEdit && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  name="isDefault"
                  checked={formData.isDefault}
                  onChange={handleChange}
                  className="w-5 h-5"
                  disabled={isSubmitting}
                />
                <div>
                  <p className="text-sm font-medium text-gray-800">
                    Set as Default SKU
                  </p>
                  <p className="text-xs text-gray-600 mt-0.5">
                    Default SKU will be displayed on public product page
                  </p>
                </div>
              </label>
            </div>
          )}

          {isEdit && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
              <p className="text-sm text-yellow-800">
                <strong>Note:</strong> Cannot change "Default SKU" here.  
                Use the "Set Default" button in the SKU table to change. 
              </p>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-end gap-4 pt-4 border-t">
            <button
              type="button"
              onClick={onClose}
              className="btn-secondary"
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary inline-flex items-center gap-2"
            >
              <Save className="w-4 h-4" />
              {isSubmitting ? 'Saving...' : isEdit ? 'Update' : 'Create SKU'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default SkuFormModal;