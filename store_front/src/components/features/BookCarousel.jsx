import { useRef } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import BookCard from './BookCard';
import { Link } from 'react-router-dom';

export function BookCarousel({ 
  title, 
  subtitle, 
  books, 
  isLoading, 
  error, 
  onAddToCart,
  viewAllLink 
}) {
  const scrollRef = useRef(null);

  const scrollLeft = () => {
    if (scrollRef.current) {
      scrollRef.current.scrollBy({ left: -400, behavior: 'smooth' });
    }
  };

  const scrollRight = () => {
    if (scrollRef.current) {
      scrollRef.current.scrollBy({ left: 400, behavior: 'smooth' });
    }
  };

  // Do not render section if there is an error and no books
  if (error && (!books || books.length === 0)) {
    return null;
  }

  // Do not render section if it's done loading and there are no books
  if (!isLoading && (!books || books.length === 0)) {
    return null;
  }

  return (
    <section className="py-12 bg-gray-50 even:bg-white">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-2xl md:text-3xl font-bold text-gray-800">
              {title}
            </h2>
            {subtitle && (
              <p className="text-gray-500 mt-1">
                {subtitle}
              </p>
            )}
          </div>
          {viewAllLink && (
            <Link
              to={viewAllLink}
              className="flex items-center gap-1 text-blue-600 hover:text-blue-700 font-medium"
            >
              View All
              <ChevronRight className="w-5 h-5" />
            </Link>
          )}
        </div>

        <div className="relative group">
          <button 
            onClick={scrollLeft} 
            className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-4 z-10 hidden group-hover:flex items-center justify-center w-10 h-10 bg-white/90 rounded-full shadow-lg border text-gray-700 hover:text-blue-600 hover:bg-white transition-all focus:outline-none"
            aria-label="Scroll left"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>

          {/* Hide scrollbar with inline styles */}
          <div 
            ref={scrollRef} 
            className="flex gap-4 md:gap-6 overflow-x-auto snap-x snap-mandatory scroll-smooth pb-4"
            style={{ msOverflowStyle: 'none', scrollbarWidth: 'none', WebkitOverflowScrolling: 'touch' }}
          >
            {isLoading ? (
              Array.from({ length: 6 }).map((_, idx) => (
                <div
                  key={`skeleton-${idx}`}
                  className="min-w-[200px] sm:min-w-[220px] md:min-w-[240px] snap-start flex-shrink-0 animate-pulse bg-white rounded-xl shadow-sm p-3 h-72"
                >
                  <div className="bg-gray-200 h-44 rounded mb-3" />
                  <div className="bg-gray-200 h-4 rounded mb-2" />
                  <div className="bg-gray-200 h-4 w-1/2 rounded" />
                </div>
              ))
            ) : books && books.length > 0 ? (
              books.map((book, idx) => (
                <div key={book?.id || `fallback-${idx}`} className="min-w-[200px] sm:min-w-[210px] w-[200px] sm:w-[210px] snap-start flex-shrink-0 h-[352px]">
                  <BookCard
                    book={book}
                    onAddToCart={onAddToCart}
                  />
                </div>
              ))
            ) : null}
          </div>

          <button 
            onClick={scrollRight} 
            className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-4 z-10 hidden group-hover:flex items-center justify-center w-10 h-10 bg-white/90 rounded-full shadow-lg border text-gray-700 hover:text-blue-600 hover:bg-white transition-all focus:outline-none"
            aria-label="Scroll right"
          >
            <ChevronRight className="w-6 h-6" />
          </button>
        </div>
      </div>
    </section>
  );
}
