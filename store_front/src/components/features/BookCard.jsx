import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ShoppingCart } from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { formatCurrency } from '../../utils/format';

function BookCard({ book }) {
  const { addToCart } = useCart();
  const [imgLoaded, setImgLoaded] = useState(false);
  const [imgError, setImgError] = useState(false);

  const handleAddToCart = async (e) => {
    e.preventDefault();
    await addToCart(book, 1);
  };

  const imageSrc = book.image || book.imageUrl;

  return (
    <Link
      to={`/books/${book.id}`}
      className="group bg-white rounded-xl shadow-sm hover:shadow-md 
                 transition-shadow flex flex-col overflow-hidden h-full"
    >
      {/* Image area — text content is NOT inside here, so it renders immediately */}
      <div className="relative h-64 bg-gray-100 overflow-hidden flex-shrink-0">
        {/* Shimmer placeholder shown while image is loading */}
        {!imgLoaded && !imgError && (
          <div className="absolute inset-0 bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 animate-pulse" />
        )}

        {/* Fallback icon when image fails */}
        {imgError && (
          <div className="absolute inset-0 flex items-center justify-center text-gray-300">
            <svg xmlns="http://www.w3.org/2000/svg" className="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
        )}

        {imageSrc && (
          <img
            src={imageSrc}
            alt={book.title}
            loading="lazy"
            onLoad={() => setImgLoaded(true)}
            onError={() => setImgError(true)}
            className={`absolute inset-0 w-full h-full object-contain p-1
                        group-hover:scale-105 transition-all duration-300
                        ${imgLoaded ? 'opacity-100' : 'opacity-0'}`}
          />
        )}
      </div>

      {/* Text content — renders immediately, independent of image load */}
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