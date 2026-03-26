import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import orderService from '../services/orderService';
import { formatCurrency } from '../utils/format';
import { useAuth } from '../context/AuthContext';

function formatDateTime(iso) {
  if (!iso) return '';
  // iso from backend: "2025-12-15T14:30:00"
  const d = new Date(iso);
  return d.toLocaleString();
}

function statusBadgeClass(status) {
  switch (status) {
    case 'PLACED':
      return 'bg-blue-50 text-blue-700 border-blue-100';
    case 'CONFIRMED':
      return 'bg-indigo-50 text-indigo-700 border-indigo-100';
    case 'PROCESSING':
      return 'bg-yellow-50 text-yellow-700 border-yellow-100';
    case 'SHIPPED':
      return 'bg-purple-50 text-purple-700 border-purple-100';
    case 'DELIVERED':
      return 'bg-green-50 text-green-700 border-green-100';
    case 'CANCELLED':
    case 'RETURNED':
      return 'bg-red-50 text-red-700 border-red-100';
    default:
      return 'bg-gray-50 text-gray-700 border-gray-100';
  }
}

function OrdersPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setError('');
      try {
        const data = await orderService.getOrders();
        if (cancelled) return;
        setOrders(data);
      } catch (err) {
        if (cancelled) return;
        setError(err.message || 'Could not load orders');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Your Orders</h1>
        {user?.username && (
          <p className="text-sm text-gray-500 mt-1">
            Account: <span className="font-medium">{user.username}</span>
          </p>
        )}
      </div>

      {isLoading && (
        <div className="bg-white rounded-xl shadow-sm p-6 text-gray-600">
          Loading orders...
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl p-4 mb-4">
          {error}
        </div>
      )}

      {!isLoading && !error && orders.length === 0 && (
        <div className="bg-white rounded-xl shadow-sm p-8 text-center">
          <h2 className="text-xl font-semibold text-gray-800 mb-2">
            You don't have any orders yet
          </h2>
          <p className="text-gray-500 mb-4">
            Add products to your cart and checkout.
          </p>
          <Link to="/books" className="btn-primary inline-block">
            Shop Now
          </Link>
        </div>
      )}

      {!isLoading && !error && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((o) => (
            <div key={o.orderId} className="bg-white rounded-xl shadow-sm p-5">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <div>
                  <p className="text-sm text-gray-500">
                    Order: <span className="font-medium">#{o.orderId}</span>
                  </p>
                  <p className="text-sm text-gray-500">
                    Date: <span className="font-medium">{formatDateTime(o.placedAt)}</span>
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <span
                    className={`text-xs px-3 py-1 rounded-full border ${statusBadgeClass(
                      o.status
                    )}`}
                  >
                    {o.status}
                  </span>
                  <p className="text-lg font-bold text-gray-800">
                    {formatCurrency(o.totalAmount)}{' '}
                    <span className="text-sm font-medium text-gray-500">
                      {o.currency}
                    </span>
                  </p>
                </div>
              </div>

              <div className="mt-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <p className="text-sm text-gray-600">
                  {o.items?.length || 0} items
                </p>
                <Link
                  to={`/orders/${o.orderId}`}
                  className="text-blue-600 hover:text-blue-700 font-medium"
                >
                  View Details →
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default OrdersPage;