import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import bookService from '../services/bookService';
import { formatCurrency } from '../utils/format';

function BooksPage() {
  const [books, setBooks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const searchKeyword = searchParams.get('q');
  const filter = searchParams.get('filter');

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setError('');
      try {
        let data;
        if (searchKeyword) {
          const pageData = await bookService.searchBooks(searchKeyword);
          data = pageData.content;
        } else if (filter === 'new') {
          const pageData = await bookService.getNewBooks();
          data = pageData.content;
        } else {
          data = await bookService.getBooks();
        }
        
        if (cancelled) return;
        setBooks(data);
      } catch (err) {
        if (cancelled) return;
        setError(err.message || 'Could not load book list');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [searchKeyword, filter]);

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-16 text-center text-gray-600">
        Loading book list...
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-16">
        <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl p-4 text-center">
          {error}
        </div>
      </div>
    );
  }

  // Removed early return for no books

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 mb-8">
        {searchKeyword ? `Search Results for "${searchKeyword}"` : (filter === 'new' ? 'New Books' : 'Books')}
      </h1>

      {!books.length ? (
        <div className="text-center py-16 text-gray-600">
          No books available.
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {books.map((book) => {
            const defaultSku = book.skus.find((s) => s.isDefault) || book.skus[0];
            const outOfStock = defaultSku && !defaultSku.inStock;

            return (
              <Link
                key={book.id}
                to={`/books/${book.id}`}
                className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow overflow-hidden"
              >
                {/* Image */}
                <div className="aspect-[3/4] bg-gray-100 relative">
                  {book.imageUrl ? (
                    <img
                      src={book.imageUrl}
                      alt={book.title}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-400">
                      <span className="text-sm">No image</span>
                    </div>
                  )}

                  {outOfStock && (
                    <div className="absolute top-2 right-2 px-2 py-1 bg-red-600 text-white text-xs font-semibold rounded">
                      Out of Stock
                    </div>
                  )}
                </div>

                {/* Info */}
                <div className="p-4">
                  <h2 className="font-semibold text-gray-800 line-clamp-2 mb-2">
                    {book.title}
                  </h2>

                  {book.subtitle && (
                    <p className="text-xs text-gray-500 line-clamp-1 mb-2">
                      {book.subtitle}
                    </p>
                  )}

                  <div className="flex items-center justify-between mt-3">
                    <p className="text-lg font-bold text-red-600">
                      {formatCurrency(book.price)}
                    </p>

                    {defaultSku && defaultSku.availableStock > 0 && defaultSku.availableStock < 10 && (
                      <p className="text-xs text-orange-600">
                        Only {defaultSku.availableStock} left
                      </p>
                    )}
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default BooksPage;