import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import bookService from '../services/bookService';
import { BookCard } from '../components/features';

const PAGE_SIZE = 32;

function BooksPage() {
  const [books, setBooks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const searchKeyword = searchParams.get('q');
  const filter = searchParams.get('filter');
  const isBestSellersRoute = location.pathname === '/bestsellers';

  // Reset list when search or filter changes
  useEffect(() => {
    setBooks([]);
    setPage(0);
    setHasMore(true);
  }, [searchKeyword, filter, isBestSellersRoute]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      const isFirstPage = page === 0;
      if (isFirstPage) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }
      setError('');
      try {
        let pageData;
        if (searchKeyword) {
          pageData = await bookService.searchBooks(searchKeyword, page, PAGE_SIZE);
        } else if (filter === 'new') {
          pageData = await bookService.getNewBooks({ page, size: PAGE_SIZE });
        } else if (isBestSellersRoute) {
          pageData = await bookService.getBestSellers({ days: 0, limit: PAGE_SIZE });
        } else {
          pageData = await bookService.getBooks({
            page,
            size: PAGE_SIZE,
            sortBy: 'id',
            sortDirection: 'DESC',
          });
        }

        if (cancelled) return;
        if (isBestSellersRoute) {
          setBooks(pageData);
          setHasMore(false);
        } else {
          setBooks((prev) => (isFirstPage ? pageData.content : [...prev, ...pageData.content]));
          setHasMore(!!pageData.hasMore);
        }
      } catch (err) {
        if (cancelled) return;
        setError(err.message || 'Could not load book list');
      } finally {
        if (!cancelled) {
          setIsLoading(false);
          setIsLoadingMore(false);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [searchKeyword, filter, isBestSellersRoute, page]);

  const handleLoadMore = () => {
    if (isLoadingMore || !hasMore) return;
    setPage((prev) => prev + 1);
  };

  if (isLoading && books.length === 0) {
    return (
      <div className="container mx-auto px-4 py-16 text-center text-gray-600">
        Loading book list...
      </div>
    );
  }

  if (error && books.length === 0) {
    return (
      <div className="container mx-auto px-4 py-16">
        <div className="bg-red-50 border border-red-100 text-red-700 rounded-xl p-4 text-center">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-800 mb-8">
        {searchKeyword
          ? `Search Results for "${searchKeyword}"`
          : (filter === 'new'
            ? 'New Books'
            : (isBestSellersRoute ? 'Bestsellers' : 'Books'))}
      </h1>

      {books.length === 0 ? (
        <div className="text-center py-16 text-gray-600">
          No books available.
        </div>
      ) : (
        <>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {books.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>

          {error && (
            <div className="mt-8 bg-red-50 border border-red-100 text-red-700 rounded-xl p-4 text-center">
              {error}
            </div>
          )}

          {hasMore && (
            <div className="mt-10 flex items-center justify-center">
              <button
                onClick={handleLoadMore}
                disabled={isLoadingMore}
                className="btn-secondary px-6 py-3 text-sm disabled:opacity-60"
              >
                {isLoadingMore ? 'Loading more...' : 'Load more'}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default BooksPage;