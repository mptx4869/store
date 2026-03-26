import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import adminOrderService from '../../services/adminOrderService';
import UpdateOrderStatusModal from '../../components/admin/UpdateOrderStatusModal';
import { useToast } from '../../context/ToastContext';
import { 
  ArrowLeft, 
  ShoppingCart, 
  Package, 
  MapPin, 
  Phone,
  Calendar,
  DollarSign,
  Edit
} from 'lucide-react';
import { formatCurrency } from '../../utils/format';

function AdminOrderDetailPage() {
  const { orderId } = useParams();
  const toast = useToast();

  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // Update status modal
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);

  useEffect(() => {
    loadOrder();
  }, [orderId]);

  const loadOrder = async () => {
    setIsLoading(true);
    setError('');
    try {
      const data = await adminOrderService.getOrderById(orderId);
      setOrder(data);
    } catch (err) {
      setError(err.message || 'Could not load order information');
      toast.error(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleStatusUpdate = async (newStatus) => {
    try {
      await adminOrderService.updateOrderStatus(orderId, newStatus);
      toast.success('Status updated successfully');
      loadOrder(); // Refresh order data
    } catch (err) {
      toast.error(err.message || 'Could not update status');
      throw err;
    }
  };

  const getStatusBadge = (status) => {
    const styles = {
      PLACED: 'bg-blue-100 text-blue-800',
      CONFIRMED: 'bg-green-100 text-green-800',
      PROCESSING: 'bg-yellow-100 text-yellow-800',
      SHIPPED: 'bg-purple-100 text-purple-800',
      DELIVERED: 'bg-green-200 text-green-900',
      CANCELLED: 'bg-red-100 text-red-800',
      RETURNED:  'bg-gray-100 text-gray-800',
    };

    const labels = {
      PLACED: 'Placed',
      CONFIRMED: 'Confirmed',
      PROCESSING: 'Processing',
      SHIPPED: 'Shipped',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
      RETURNED: 'Returned',
    };

    return (
      <span
        className={`inline-block px-3 py-1 text-sm font-medium rounded-lg ${
          styles[status] || 'bg-gray-100 text-gray-800'
        }`}
      >
        {labels[status] || status}
      </span>
    );
  };

  const isFinalState = (status) => {
    return ['DELIVERED', 'CANCELLED', 'RETURNED'].includes(status);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading order information...</div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="space-y-4">
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          {error || 'Order not found'}
        </div>
        <Link
          to="/admin/orders"
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
        to="/admin/orders"
        className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700"
      >
        <ArrowLeft className="w-5 h-5" />
        Back to order list
      </Link>

      {/* Header */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-3">
              <ShoppingCart className="w-8 h-8" />
              Order #{order. orderId}
            </h1>
            <div className="flex items-center gap-3 mt-3">
              {getStatusBadge(order. status)}
              {! isFinalState(order.status) && (
                <button
                  onClick={() => setIsStatusModalOpen(true)}
                  className="text-blue-600 hover:text-blue-700 text-sm font-medium inline-flex items-center gap-1"
                >
                  <Edit className="w-4 h-4" />
                  Update status
                </button>
              )}
            </div>
          </div>

          <div className="text-right">
            <p className="text-sm text-gray-500">Total Amount</p>
            <p className="text-2xl font-bold text-gray-800">
              {formatCurrency(order.totalAmount)} {order.currency}
            </p>
          </div>
        </div>

        {/* Metadata */}
        <div className="grid grid-cols-2 gap-6 mt-6 pt-6 border-t">
          <div className="flex items-center gap-3">
            <Calendar className="w-5 h-5 text-gray-400" />
            <div>
              <p className="text-sm text-gray-500">Order Date</p>
              <p className="text-sm font-medium text-gray-800">
                {order.placedAt
                  ?  new Date(order.placedAt).toLocaleString('en-US')
                  : '—'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Package className="w-5 h-5 text-gray-400" />
            <div>
              <p className="text-sm text-gray-500">Cart ID</p>
              <p className="text-sm font-medium text-gray-800">
                {order.cartId || '—'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Shipping & Billing Info */}
      <div className="grid md:grid-cols-2 gap-6">
        {/* Shipping Address */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <MapPin className="w-5 h-5 text-blue-600" />
            Shipping Address
          </h2>
          <div className="space-y-2">
            <p className="text-gray-700">{order.shippingAddress || 'No address'}</p>
            {order.shippingPhone && (
              <div className="flex items-center gap-2 text-gray-600">
                <Phone className="w-4 h-4" />
                <span>{order.shippingPhone}</span>
              </div>
            )}
          </div>
        </div>

        {/* Billing Address */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <DollarSign className="w-5 h-5 text-green-600" />
            Billing Address
          </h2>
          <div className="space-y-2">
            <p className="text-gray-700">{order.billingAddress || 'No address'}</p>
            {order.billingPhone && (
              <div className="flex items-center gap-2 text-gray-600">
                <Phone className="w-4 h-4" />
                <span>{order. billingPhone}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Order Items */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h2 className="text-xl font-bold text-gray-800 mb-4">
          Products ({order.items.length})
        </h2>

        {order.items.length === 0 ? (
          <p className="text-gray-500 text-center py-8">No products</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Product
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    SKU
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Unit Price
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Quantity
                  </th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                    Subtotal
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {order.items.map((item, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <Link
                        to={`/admin/books/${item.bookId}`}
                        className="text-sm font-medium text-blue-600 hover:text-blue-700"
                      >
                        {item.title}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-sm font-mono text-gray-600">
                      {item. sku}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-800">
                      {formatCurrency(item.unitPrice)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-800">
                      {item.quantity}
                    </td>
                    <td className="px-4 py-3 text-sm font-semibold text-gray-800 text-right">
                      {formatCurrency(item.lineTotal)}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="bg-gray-50">
                <tr>
                  <td colSpan="4" className="px-4 py-3 text-right font-semibold text-gray-800">
                    Total:
                  </td>
                  <td className="px-4 py-3 text-right text-lg font-bold text-gray-900">
                    {formatCurrency(order.totalAmount)} {order.currency}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </div>

      {/* Final State Notice */}
      {isFinalState(order.status) && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <p className="text-sm text-yellow-800">
            <strong>Note:</strong> Order is in final state and cannot be changed.
          </p>
        </div>
      )}

      {/* Update Status Modal */}
      <UpdateOrderStatusModal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        onSubmit={handleStatusUpdate}
        currentStatus={order.status}
      />
    </div>
  );
}

export default AdminOrderDetailPage;