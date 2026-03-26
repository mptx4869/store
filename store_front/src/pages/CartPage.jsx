import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context';
import { formatCurrency } from '../utils/format';
import orderService from '../services/orderService';
import { useToast } from '../context/ToastContext';
import {
  Trash2,
  Minus,
  Plus,
  ShoppingBag,
  ArrowLeft,
  AlertTriangle,
} from 'lucide-react';

function CartPage() {
  const navigate = useNavigate();
  const toast = useToast();

  const {
    items,
    totalPrice,
    isLoading,
    error,
    updateQuantity,
    removeFromCart,
    clearCart,
    refreshCart,
    clearError,
  } = useCart();

  const [shippingAddress, setShippingAddress] = useState('');
  const [shippingPhone, setShippingPhone] = useState('');
  const [validationError, setValidationError] = useState('');

  const total = totalPrice;

  // Validate shipping info
  const isShippingValid = () => {
    if (!shippingAddress.trim()) {
      setValidationError('Shipping address is required');
      return false;
    }
    if (shippingAddress.trim().length < 10) {
      setValidationError('Shipping address must be at least 10 characters');
      return false;
    }
    if (!shippingPhone.trim()) {
      setValidationError('Shipping phone is required');
      return false;
    }
    const phoneRegex = /^[0-9]{10,11}$/;
    if (!phoneRegex.test(shippingPhone.trim())) {
      setValidationError('Phone number must be 10-11 digits');
      return false;
    }
    setValidationError('');
    return true;
  };

  const handleDecrease = (item) => {
    updateQuantity(item.itemId, item.quantity - 1);
  };

  const handleIncrease = (item) => {
    if (item.quantity >= 99) return;
    updateQuantity(item.itemId, item.quantity + 1);
  };

  const handleRemove = (item) => {
    removeFromCart(item.itemId);
  };

  const handleClearCart = () => {
    clearCart();
  };

  const handleCheckout = async () => {
    if (!isShippingValid()) {
      toast.error(validationError, { title: 'Validation Error' });
      return;
    }

    try {
      const payload = {
        shippingAddress: shippingAddress.trim(),
        shippingPhone: shippingPhone.trim(),
      };
      
      const created = await orderService.createOrder(payload);
     
      // Dispatch event for CartContext to refresh (backend has cleared cart)
      window.dispatchEvent(new Event('checkout:success'));
      toast.success(`Order placed successfully. Order #${created.orderId}.`, {
        title: 'Order Successful',
      });
      navigate(`/orders/${created.orderId}`);
    } catch (err) {
      console.error('Checkout error:', err);
      toast.error(err.message || 'Could not create order.', {
        title: 'Order Failed',
      });
    }
  };
  if (!items || items.length === 0) {
    return (
      <div className="container mx-auto px-4 py-16 text-center">
        <ShoppingBag className="w-24 h-24 text-gray-300 mx-auto mb-6" />
        <h1 className="text-2xl font-bold text-gray-800 mb-4">
          Cart is empty
        </h1>
        <p className="text-gray-500 mb-6">
          You have no items in your cart
        </p>
        <Link to="/books" className="btn-primary inline-flex items-center gap-2">
          <ArrowLeft className="w-5 h-5" />
          Continue shopping
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold text-gray-800">
          Cart ({items.length} items)
        </h1>
        <button
          onClick={handleClearCart}
          disabled={isLoading}
          className="text-red-500 hover:text-red-600 text-sm font-medium disabled:opacity-50"
        >
          Clear all
        </button>
      </div>

      {error && (
        <div className="mb-4 flex items-start gap-2 p-3 rounded-lg bg-red-50 text-red-700">
          <AlertTriangle className="w-5 h-5 mt-0.5 flex-shrink-0" />
          <div className="flex-1 text-sm">
            <p className="font-medium mb-1">An error occurred</p>
            <p>{error}</p>
          </div>
          <button
            onClick={clearError}
            className="text-xs font-medium underline"
          >
            Close
          </button>
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-8">
        {/* Cart Items */}
        <div className="lg:col-span-2 space-y-4">
          {items.map((item) => (
            <div
              key={item.itemId}
              className="bg-white rounded-xl shadow-sm p-4 flex gap-4"
            >
              <Link to={`/books/${item.bookId}`} className="flex-shrink-0">
                {item.image ? (
                  <img
                    src={item.image}
                    alt={item.title}
                    className="w-24 h-32 object-contain rounded-lg bg-gray-50"
                  />
                ) : (
                  <div className="w-24 h-32 rounded-lg bg-gray-100 flex items-center justify-center text-xs text-gray-400">
                    No image
                  </div>
                )}
              </Link>

              <div className="flex-1 min-w-0">
                <Link
                  to={`/books/${item.bookId}`}
                  className="font-semibold text-gray-800 hover:text-blue-600 line-clamp-2"
                >
                  {item.title}
                </Link>

                <div className="mt-1 text-sm text-gray-500">
                  <p>SKU: {item.sku}</p>
                </div>

                <div className="mt-2 flex items-center gap-2">
                  <p className="text-lg font-bold text-red-600">
                    {formatCurrency(item.unitPrice)}
                  </p>
                  {item.priceChanged && (
                    <span className="text-xs px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-800">
                      Price changed
                    </span>
                  )}
                </div>

                {item.priceChanged && (
                  <p className="text-xs text-gray-500 mt-1">
                    Old price:{' '}
                    <span className="line-through">
                      {formatCurrency(item.originalPrice)}
                    </span>{' '}
                    {item.priceDiff < 0 ? (
                      <span className="text-green-600">
                        (Decreased {formatCurrency(Math.abs(item.priceDiff))})
                      </span>
                    ) : (
                      <span className="text-red-600">
                        (Increased {formatCurrency(item.priceDiff)})
                      </span>
                    )}
                  </p>
                )}

                <div className="flex items-center justify-between mt-4">
                  <div className="flex items-center border rounded-lg">
                    <button
                      onClick={() => handleDecrease(item)}
                      disabled={isLoading}
                      className="p-2 hover:bg-gray-100 transition-colors disabled:opacity-50"
                    >
                      <Minus className="w-4 h-4" />
                    </button>
                    <span className="px-4 py-1 font-medium">
                      {item.quantity}
                    </span>
                    <button
                      onClick={() => handleIncrease(item)}
                      disabled={isLoading || item.quantity >= 99}
                      className="p-2 hover:bg-gray-100 transition-colors disabled:opacity-50"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>

                  <button
                    onClick={() => handleRemove(item)}
                    disabled={isLoading}
                    className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
                  >
                    <Trash2 className="w-5 h-5" />
                  </button>
                </div>
              </div>

              <div className="hidden sm:block text-right">
                <p className="text-sm text-gray-500">Subtotal</p>
                <p className="text-lg font-bold text-gray-800">
                  {formatCurrency(item.lineTotal)}
                </p>
              </div>
            </div>
          ))}

          <Link
            to="/books"
            className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700 font-medium mt-4"
          >
            <ArrowLeft className="w-5 h-5" />
            Continue shopping
          </Link>
        </div>

        {/* Summary */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-xl shadow-sm p-6 sticky top-24">
            <h2 className="text-xl font-bold text-gray-800 mb-6">
              Order Summary
            </h2>

            {/* Shipping Information */}
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">
                Shipping Information
              </h3>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Shipping Address <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    value={shippingAddress}
                    onChange={(e) => setShippingAddress(e.target.value)}
                    placeholder="Enter your shipping address"
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none text-sm"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Shipping Phone <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="tel"
                    value={shippingPhone}
                    onChange={(e) => setShippingPhone(e.target.value.replace(/[^0-9]/g, ''))}
                    placeholder="0123456789"
                    maxLength={11}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    10-11 digits required
                  </p>
                </div>
              </div>

              {validationError && (
                <div className="mt-3 p-2 bg-red-50 border border-red-200 rounded text-xs text-red-600">
                  {validationError}
                </div>
              )}
            </div>

            <div className="border-t mb-4"></div>

            <div className="space-y-3 text-gray-600">
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span>{formatCurrency(totalPrice)}</span>
              </div>
            </div>

            <div className="border-t my-4"></div>

            <div className="flex justify-between text-lg font-bold">
              <span>Total</span>
              <span className="text-red-600">{formatCurrency(total)}</span>
            </div>

            <button
              className="w-full btn-primary mt-6 py-3 text-lg disabled:opacity-50"
              disabled={isLoading || !shippingAddress.trim() || !shippingPhone.trim()}
              onClick={handleCheckout}
            >
              Proceed to Checkout
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CartPage;