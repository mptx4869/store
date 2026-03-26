import { useState, useEffect } from 'react';
import { X, Save, Package } from 'lucide-react';

function UpdateStockModal({ isOpen, onClose, onSubmit, inventory }) {
  const [action, setAction] = useState('ADD');
  const [stock, setStock] = useState('');
  const [reserved, setReserved] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen && inventory) {
      setAction('ADD');
      setStock('');
      setReserved('');
    }
  }, [isOpen, inventory]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    const stockValue = stock ?  parseInt(stock) : null;
    const reservedValue = reserved ? parseInt(reserved) : null;

    if (stockValue === null && reservedValue === null) {
      alert('Please enter at least one value');
      return;
    }

    if (stockValue !== null && stockValue < 0) {
      alert('Stock quantity cannot be negative');
      return;
    }

    if (reservedValue !== null && reservedValue < 0) {
      alert('Reserved quantity cannot be negative');
      return;
    }

    // Check if SET stock < reserved
    if (action === 'SET' && stockValue !== null && inventory) {
      const finalReserved = reservedValue !== null ? reservedValue : inventory.reservedStock;
      if (stockValue < finalReserved) {
        alert(`Cannot set stock to ${stockValue}. Reserved quantity is ${finalReserved}.`);
        return;
      }
    }

    setIsSubmitting(true);

    try {
      const payload = {
        action,
      };

      if (stockValue !== null) {
        payload.stock = stockValue;
      }

      if (reservedValue !== null) {
        payload.reserved = reservedValue;
      }

      await onSubmit(payload);
      onClose();
    } catch (err) {
      console.error('Update stock error:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const calculateNewTotal = () => {
    if (! inventory) return null;
    
    const stockValue = stock ? parseInt(stock) : 0;
    
    if (action === 'ADD') {
      return inventory.totalStock + stockValue;
    } else {
      return stockValue;
    }
  };

  const calculateNewAvailable = () => {
    if (!inventory) return null;
    
    const newTotal = calculateNewTotal();
    const newReserved = reserved ? parseInt(reserved) : inventory.reservedStock;
    
    return Math.max(0, newTotal - newReserved);
  };

  if (!isOpen || !inventory) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b sticky top-0 bg-white">
          <div>
            <h2 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
              <Package className="w-6 h-6" />
              Update Stock
            </h2>
            <p className="text-sm text-gray-600 mt-1">
              {inventory.bookTitle} - {inventory.format}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            disabled={isSubmitting}
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Current Status */}
        <div className="p-6 bg-gray-50 border-b">
          <h3 className="text-sm font-semibold text-gray-700 mb-3">Current Status</h3>
          <div className="grid grid-cols-3 gap-4">
            <div className="bg-white rounded-lg p-3 border">
              <p className="text-xs text-gray-500 mb-1">Total Stock</p>
              <p className="text-2xl font-bold text-gray-800">{inventory.totalStock}</p>
            </div>
            <div className="bg-white rounded-lg p-3 border">
              <p className="text-xs text-gray-500 mb-1">Reserved</p>
              <p className="text-2xl font-bold text-orange-600">{inventory.reservedStock}</p>
            </div>
            <div className="bg-white rounded-lg p-3 border">
              <p className="text-xs text-gray-500 mb-1">Available</p>
              <p className="text-2xl font-bold text-green-600">{inventory.availableStock}</p>
            </div>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Action Mode */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-3">
              Update Mode
            </label>
            <div className="grid grid-cols-2 gap-4">
              <label
                className={`flex items-center justify-center gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                  action === 'ADD'
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <input
                  type="radio"
                  value="ADD"
                  checked={action === 'ADD'}
                  onChange={(e) => setAction(e.target.value)}
                  className="w-5 h-5"
                  disabled={isSubmitting}
                />
                <div>
                  <p className="font-semibold text-gray-800">ADD - Increment</p>
                  <p className="text-xs text-gray-600">Add new stock</p>
                </div>
              </label>

              <label
                className={`flex items-center justify-center gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                  action === 'SET'
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <input
                  type="radio"
                  value="SET"
                  checked={action === 'SET'}
                  onChange={(e) => setAction(e.target. value)}
                  className="w-5 h-5"
                  disabled={isSubmitting}
                />
                <div>
                  <p className="font-semibold text-gray-800">SET - Set Value</p>
                  <p className="text-xs text-gray-600">Inventory audit</p>
                </div>
              </label>
            </div>
          </div>

          {/* Stock Input */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              {action === 'ADD' ? 'Quantity to Add' : 'New Stock Total'}
            </label>
            <input
              type="number"
              value={stock}
              onChange={(e) => setStock(e.target.value)}
              placeholder={action === 'ADD' ? 'e.g., 100' : 'e.g., 250'}
              min="0"
              className="input-field w-full"
              disabled={isSubmitting}
            />
            
            {stock && (
              <div className="mt-2 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                <p className="text-sm text-blue-800">
                  {action === 'ADD' ?  (
                    <>
                      <strong>Stock after update:</strong> {inventory. totalStock} + {stock} = {calculateNewTotal()}
                    </>
                  ) : (
                    <>
                      <strong>Stock after update:</strong> {stock}
                    </>
                  )}
                </p>
              </div>
            )}
          </div>

          {/* Reserved Input */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Set Reserved Quantity (optional)
            </label>
            <input
              type="number"
              value={reserved}
              onChange={(e) => setReserved(e.target.value)}
              placeholder={`Current: ${inventory.reservedStock}`}
              min="0"
              className="input-field w-full"
              disabled={isSubmitting}
            />
            <p className="text-xs text-gray-500 mt-1">
              Only change when adjusting stuck or expired orders
            </p>
          </div>

          {/* Preview */}
          {(stock || reserved) && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <h4 className="text-sm font-semibold text-green-800 mb-2">Status After Update</h4>
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <p className="text-xs text-green-700">Total Stock</p>
                  <p className="text-xl font-bold text-green-800">{calculateNewTotal()}</p>
                </div>
                <div>
                  <p className="text-xs text-green-700">Reserved</p>
                  <p className="text-xl font-bold text-green-800">
                    {reserved ? parseInt(reserved) : inventory.reservedStock}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-green-700">Available</p>
                  <p className="text-xl font-bold text-green-800">{calculateNewAvailable()}</p>
                </div>
              </div>
            </div>
          )}

          {/* Warning */}
          {action === 'SET' && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
              <p className="text-sm text-yellow-800">
                <strong>⚠️ Warning:</strong> SET mode will overwrite the old value.  
                Use this mode when auditing actual inventory. 
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
              disabled={isSubmitting || (! stock && !reserved)}
              className="btn-primary inline-flex items-center gap-2"
            >
              <Save className="w-4 h-4" />
              {isSubmitting ? 'Updating...' : 'Update'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default UpdateStockModal;