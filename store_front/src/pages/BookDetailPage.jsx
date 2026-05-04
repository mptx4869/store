import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import bookService from '../services/bookService';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { formatCurrency } from '../utils/format';
import { ArrowLeft, ShoppingCart } from 'lucide-react';

function BookDetailPage() {
  const { id } = useParams();
  const toast = useToast();
  const { addToCart } = useCart();

  const [book, setBook] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const [selectedSkuId, setSelectedSkuId] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [isAdding, setIsAdding] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setError('');
      try {
        const data = await bookService.getBookById(id);
        if (cancelled) return;

        setBook(data);

        const defaultSku = data.skus.find((s) => s.isDefault) || data.skus[0];
        if (defaultSku) setSelectedSkuId(defaultSku.id);
      } catch (err) {
        if (cancelled) return;
        setError(err.message || 'Could not load book details');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleAddToCart = async () => {
    if (!selectedSkuId) {
      toast.error('Please select a book version.');
      return;
    }

    const selectedSku = book.skus.find((s) => s.id === selectedSkuId);
    if (!selectedSku || !selectedSku.inStock) {
      toast.error('This version is currently out of stock.');
      return;
    }

    setIsAdding(true);
    try {
      await addToCart(selectedSkuId, quantity);
      toast.success(`Added "${book.title}" to cart.`);
      setQuantity(1);
    } catch (err) {
      toast.error(err.message || 'Could not add to cart.');
    } finally {
      setIsAdding(false);
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-16 text-center text-gray-600">
        Loading book details...
      </div>
    );
  }

  if (error || !book) {
    return (
      <div className="container mx-auto px-4 py-16">
        <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl p-4 text-center mb-4">
          {error || 'Book not found.'}
        </div>
        <Link to="/books" className="btn-secondary inline-flex items-center gap-2">
          <ArrowLeft className="w-5 h-5" />
          Back to list
        </Link>
      </div>
    );
  }

  const selectedSku = book.skus. find((s) => s.id === selectedSkuId);
  const canAddToCart = selectedSku && selectedSku.inStock && quantity > 0 && quantity <= selectedSku.availableStock;

  return (
    <div className="container mx-auto px-4 py-8">
      <Link to="/books" className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700 mb-6">
        <ArrowLeft className="w-5 h-5" />
        Back to list
      </Link>

      <div className="grid lg:grid-cols-2 gap-8">
        {/* Image */}
        <div className="bg-white border rounded-xl p-6 flex items-center justify-center" style={{ height: '420px' }}>
          {book.imageUrl ? (
            <img
              src={book.imageUrl}
              alt={book.title}
              className="h-full w-auto max-w-full object-contain rounded-lg"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400 bg-gray-50 rounded-lg">
              <span>No image</span>
            </div>
          )}
        </div>

        {/* Info */}
        <div>
          <h1 className="text-3xl font-bold text-gray-800 mb-2">{book.title}</h1>

          {book.subtitle && (
            <p className="text-lg text-gray-600 mb-4">{book.subtitle}</p>
          )}

          {/* SKU Selection (Button Group) */}
          <div className="mb-6">
            <p className="text-sm font-medium text-gray-700 mb-2">Select version: </p>
            <div className="flex flex-wrap gap-2">
              {book.skus.map((sku) => {
                const isSelected = sku.id === selectedSkuId;
                const isOutOfStock = ! sku.inStock;

                return (
                  <button
                    key={sku.id}
                    onClick={() => setSelectedSkuId(sku. id)}
                    disabled={isOutOfStock}
                    className={`
                      px-4 py-2 rounded-lg border font-medium transition-colors
                      ${isSelected
                        ? 'border-blue-600 bg-blue-50 text-blue-700'
                        : 'border-gray-300 bg-white text-gray-700 hover:border-gray-400'
                      }
                      ${isOutOfStock ? 'opacity-50 cursor-not-allowed' : ''}
                    `}
                  >
                    <div className="text-sm">{sku.format}</div>
                    <div className="text-xs text-gray-500">{formatCurrency(sku.price)}</div>
                    {isOutOfStock && <div className="text-xs text-red-600">Out of stock</div>}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Price */}
          {selectedSku && (
            <div className="mb-6">
              <p className="text-3xl font-bold text-red-600">
                {formatCurrency(selectedSku.price)}
              </p>

              {selectedSku.inStock && selectedSku.availableStock > 0 && (
                <p className="text-sm text-gray-600 mt-1">
                  <span className="font-semibold">{selectedSku.availableStock}</span> left in stock
                </p>
              )}

              {!selectedSku.inStock && (
                <p className="text-sm text-red-600 font-semibold mt-1">Out of stock</p>
              )}
            </div>
          )}

          {/* Quantity */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Quantity:
            </label>
            <div className="flex items-center gap-3">
              <button
                className="btn-secondary"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                disabled={isAdding || ! selectedSku || ! selectedSku.inStock}
              >
                -
              </button>
              <input
                type="number"
                min="1"
                max={selectedSku?. availableStock || 1}
                value={quantity}
                onChange={(e) => {
                  const val = Math.max(1, Math.min(selectedSku?.availableStock || 1, Number(e.target.value)));
                  setQuantity(val);
                }}
                className="input-field w-20 text-center"
                disabled={isAdding || !selectedSku || !selectedSku.inStock}
              />
              <button
                className="btn-secondary"
                onClick={() => setQuantity((q) => Math.min(selectedSku?.availableStock || 1, q + 1))}
                disabled={isAdding || !selectedSku || !selectedSku. inStock}
              >
                +
              </button>
            </div>
          </div>

          {/* Add to cart */}
          <button
            className="btn-primary w-full py-3 text-lg flex items-center justify-center gap-2"
            onClick={handleAddToCart}
            disabled={! canAddToCart || isAdding}
          >
            <ShoppingCart className="w-5 h-5" />
            {isAdding ? 'Adding...' : 'Add to cart'}
          </button>

          {/* Description */}
          <div className="mt-8 border-t pt-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-3">Description</h2>
            <p className="text-gray-700 whitespace-pre-line">
              {book.description || 'No description.'}
            </p>
          </div>

          {/* Details */}
          <div className="mt-6 border-t pt-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-3">Details</h2>
            <div className="space-y-2 text-sm text-gray-700">
              <p><span className="font-medium">Language:</span> {book.language || '—'}</p>
              <p><span className="font-medium">Pages:</span> {book.pages || '—'}</p>
              <p><span className="font-medium">Published:</span> {book.publishedDate || '—'}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default BookDetailPage;