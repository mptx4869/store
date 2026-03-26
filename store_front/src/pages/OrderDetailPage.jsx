import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import orderService from '../services/orderService';
import { formatCurrency } from '../utils/format';

function formatDateTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleString();
}

function canCancel(status) {
  return status === 'PLACED' || status === 'CONFIRMED';
}

function OrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCancelling, setIsCancelling] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setError('');
      try {
        const data = await orderService.getOrderById(orderId);
        if (cancelled) return;
        setOrder(data);
      } catch (err) {
        if (cancelled) return;
        setError(err.message || 'Could not load order details');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [orderId]);

  const canCancelOrder = useMemo(() => canCancel(order?.status), [order?.status]);

  const handleCancel = async () => {
    if (!order) return;
    setIsCancelling(true);
    setError('');

    try {
      const updated = await orderService.cancelOrder(order.orderId);
      setOrder(updated);
    } catch (err) {
      setError(err.message || 'Could not cancel order');
    } finally {
      setIsCancelling(false);
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="bg-white rounded-xl shadow-sm p-6 text-gray-600">
          Loading order details...
        </div>
      </div>
    );
  }

  if (error && !order) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl p-4 mb-4">
          {error}
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>
          Back
        </button>
      </div>
    );
  }

  if (!order) return null;

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <Link to="/orders" className="text-blue-600 hover:text-blue-700 text-sm">
            ← Back to order list
          </Link>
          <h1 className="text-3xl font-bold text-gray-800 mt-2">
            Order #{order.orderId}
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Placed at: <span className="font-medium">{formatDateTime(order.placedAt)}</span>
          </p>
        </div>

        <div className="text-right">
          <p className="text-sm text-gray-500">Status</p>
          <p className="text-lg font-bold text-gray-800">{order.status}</p>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl p-4 mb-4">
          {error}
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-8">
        {/* Items */}
        <div className="lg:col-span-2">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-xl font-bold text-gray-800 mb-4">Items</h2>

            <div className="space-y-4">
              {order.items.map((it) => (
                <div
                  key={`${it.bookId}-${it.sku}`}
                  className="flex items-start justify-between gap-4 border-b last:border-b-0 pb-4 last:pb-0"
                >
                  <div className="min-w-0">
                    <p className="font-semibold text-gray-800 line-clamp-2">
                      {it.title}
                    </p>
                    <p className="text-sm text-gray-500">SKU: {it.sku}</p>
                    <p className="text-sm text-gray-600">
                      {it.quantity} × {formatCurrency(it.unitPrice)}
                    </p>
                  </div>

                  <div className="text-right">
                    <p className="text-sm text-gray-500">Subtotal</p>
                    <p className="font-bold text-gray-800">
                      {formatCurrency(it.lineTotal)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Summary */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-xl shadow-sm p-6 sticky top-24">
            <h2 className="text-xl font-bold text-gray-800 mb-4">Summary</h2>

            <div className="space-y-2 text-gray-600 text-sm">
              <div className="flex justify-between">
                <span>Total</span>
                <span className="font-semibold text-gray-800">
                  {formatCurrency(order.totalAmount)} {order.currency}
                </span>
              </div>
              <div className="pt-2">
                <p className="font-medium text-gray-800 mb-1">Shipping</p>
                <p className="text-gray-600 whitespace-pre-line">
                  {order.shippingAddress || 'Not provided'}
                </p>
                <p className="text-gray-600">
                  {order.shippingPhone || ''}
                </p>
              </div>

              <div className="pt-2">
                <p className="font-medium text-gray-800 mb-1">Billing</p>
                <p className="text-gray-600 whitespace-pre-line">
                  {order.billingAddress || 'Not provided'}
                </p>
                <p className="text-gray-600">
                  {order.billingPhone || ''}
                </p>
              </div>
            </div>

            {canCancelOrder && (
              <button
                className="w-full mt-6 py-3 rounded-lg bg-red-50 text-red-700 font-semibold hover:bg-red-100 disabled:opacity-50"
                disabled={isCancelling}
                onClick={handleCancel}
              >
                {isCancelling ? 'Cancelling...' : 'Cancel Order'}
              </button>
            )}

            {!canCancelOrder && (
              <p className="text-xs text-gray-500 mt-6">
                Orders can only be cancelled when status is PLACED or CONFIRMED.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default OrderDetailPage;