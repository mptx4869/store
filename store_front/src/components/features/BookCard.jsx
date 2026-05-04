import { Link } from 'react-router-dom';
import { ShoppingCart } from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { formatCurrency } from '../../utils/format';

function BookCard({ book }) {
  const { addToCart } = useCart();

  const handleAddToCart = async (e) => {
    e.preventDefault(); // prevent double click on Link
    await addToCart(book, 1);
  };

  return (
    <Link
      to={`/books/${book.id}`}
      className="group bg-white rounded-xl shadow-sm hover:shadow-md 
                 transition-shadow flex flex-col overflow-hidden h-full"
    >
      {/* Image: fixed height */}
      <div className="relative h-64 bg-gray-100 overflow-hidden flex-shrink-0">
        <img
          src={book.image || book.imageUrl}
          alt={book.title}
          className="absolute inset-0 w-full h-full object-contain p-1
                     group-hover:scale-105 transition-transform duration-200"
        />
      </div>

      {/* Content: fixed height, no overflow */}
      <div className="p-3 flex flex-col" style={{ height: '128px' }}>
        <h3 className="font-semibold text-gray-800 text-sm line-clamp-2 mb-auto leading-tight">
          {book.title}
        </h3>
        <div className="flex items-center justify-between gap-2 mt-2 flex-shrink-0">
          <span className="text-red-600 font-bold text-sm">
            {formatCurrency(book.price ?? book.basePrice ?? 0)}
          </span>
          <button
            onClick={handleAddToCart}
            className="p-2 rounded-full bg-blue-50 text-blue-600 
                       hover:bg-blue-100 transition-colors flex-shrink-0"
          >
            <ShoppingCart className="w-4 h-4" />
          </button>
        </div>
      </div>
    </Link>
  );
}

export default BookCard;