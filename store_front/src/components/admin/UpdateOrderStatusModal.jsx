import { useState, useEffect } from 'react';
import { X, Save, AlertTriangle } from 'lucide-react';

function UpdateOrderStatusModal({ isOpen, onClose, onSubmit, currentStatus }) {
  const [selectedStatus, setSelectedStatus] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Status flow definitions
  const statusFlow = {
    PLACED: ['CONFIRMED', 'CANCELLED'],
    CONFIRMED: ['PROCESSING', 'CANCELLED'],
    PROCESSING: ['SHIPPED', 'CANCELLED'],
    SHIPPED: ['DELIVERED'],
    DELIVERED: [],
    CANCELLED: [],
    RETURNED: [],
  };

  const statusLabels = {
    PLACED: 'Placed',
    CONFIRMED: 'Confirmed',
    PROCESSING: 'Processing',
    SHIPPED: 'Shipped',
    DELIVERED: 'Delivered',
    CANCELLED:  'Cancelled',
    RETURNED: 'Returned',
  };

  const statusDescriptions = {
    CONFIRMED: 'Confirm order and payment valid',
    PROCESSING: 'Start preparing order',
    SHIPPED: 'Order has been shipped',
    DELIVERED: 'Delivered successfully (Inventory will be deducted)',
    CANCELLED: 'Cancel order (Reserved stock will be returned)',
  };

  const warningMessages = {
    DELIVERED: 'Warning: This action will deduct actual inventory. Cannot be undone!',
    CANCELLED: 'Warning: Order will be cancelled and cannot be changed later.',
    SHIPPED: 'Note: After shipment, order cannot be cancelled.',
  };

  useEffect(() => {
    if (isOpen && currentStatus) {
      setSelectedStatus('');
    }
  }, [isOpen, currentStatus]);

  const getAvailableStatuses = () => {
    return statusFlow[currentStatus] || [];
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedStatus) {
      alert('Please select a new status');
      return;
    }

    const confirmed = window.confirm(
      `Confirm changing status to "${statusLabels[selectedStatus]}"?\n\n${
        warningMessages[selectedStatus] || ''
      }`
    );

    if (!confirmed) return;

    setIsSubmitting(true);

    try {
      await onSubmit(selectedStatus);
      onClose();
    } catch (err) {
      console.error('Update status error:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  const availableStatuses = getAvailableStatuses();

  if (availableStatuses.length === 0) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-bold text-gray-800">Update Status</h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X className="w-6 h-6" />
            </button>
          </div>

          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <p className="text-sm text-yellow-800">
              Order is in final status and cannot be changed.
            </p>
          </div>

          <button onClick={onClose} className="btn-secondary w-full mt-4">
            Close
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b sticky top-0 bg-white">
          <h2 className="text-2xl font-bold text-gray-800">Update Order Status</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover: text-gray-600 transition-colors"
            disabled={isSubmitting}
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Current Status */}
        <div className="p-6 bg-gray-50 border-b">
          <p className="text-sm text-gray-600 mb-2">Current Status:</p>
          <span className="inline-block px-3 py-1 text-sm font-medium rounded-lg bg-blue-100 text-blue-800">
            {statusLabels[currentStatus]}
          </span>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-3">
              Select New Status <span className="text-red-600">*</span>
            </label>

            <div className="space-y-3">
              {availableStatuses.map((status) => (
                <label
                  key={status}
                  className={`flex items-start gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    selectedStatus === status
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio"
                    value={status}
                    checked={selectedStatus === status}
                    onChange={(e) => setSelectedStatus(e.target.value)}
                    className="w-5 h-5 mt-0.5"
                    disabled={isSubmitting}
                  />
                  <div className="flex-1">
                    <p className="font-semibold text-gray-800">{statusLabels[status]}</p>
                    {statusDescriptions[status] && (
                      <p className="text-sm text-gray-600 mt-1">
                        {statusDescriptions[status]}
                      </p>
                    )}
                  </div>
                </label>
              ))}
            </div>
          </div>

          {/* Warning */}
          {selectedStatus && warningMessages[selectedStatus] && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-yellow-800">{warningMessages[selectedStatus]}</p>
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
              disabled={isSubmitting || !selectedStatus}
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

export default UpdateOrderStatusModal;