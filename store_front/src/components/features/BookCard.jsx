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
                 transition-shadow flex flex-col overflow-hidden"
    >
      <div className="relative pt-[140%] bg-gray-100 overflow-hidden">
        <img
          src={book.image || book.imageUrl}
          alt={book.title}
          className="absolute inset-0 w-full h-full object-cover 
                     group-hover:scale-105 transition-transform duration-200"
        />
      </div>

      <div className="p-3 flex flex-col flex-1">
        <h3 className="font-semibold text-gray-800 text-sm line-clamp-2 mb-1">
          {book.title}
        </h3>
        {book.subtitle && (
          <p className="text-xs text-gray-500 line-clamp-1 mb-1">
            {book.subtitle}
          </p>
        )}

        <div className="mt-auto flex items-center justify-between gap-2">
          <span className="text-red-600 font-bold text-base">
            {formatCurrency(book.price)}
          </span>
          <button
            onClick={handleAddToCart}
            className="p-2 rounded-full bg-blue-50 text-blue-600 
                       hover:bg-blue-100 transition-colors"
          >
            <ShoppingCart className="w-4 h-4" />
          </button>
        </div>
      </div>
    </Link>
  );
}

export default BookCard;