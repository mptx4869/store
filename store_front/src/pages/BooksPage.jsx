import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import bookService from '../services/bookService';
import { BookCard } from '../components/features';
import { ChevronLeft, ChevronRight } from 'lucide-react';

function BooksPage() {
  const [books, setBooks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [pageInput, setPageInput] = useState('');

  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const searchKeyword = searchParams.get('q');
  const filter = searchParams.get('filter');

  // Reset page when search or filter changes
  useEffect(() => {
    setCurrentPage(0);
  }, [searchKeyword, filter]);

  // Sync page input with current page
  useEffect(() => {
    setPageInput(String(currentPage + 1));
  }, [currentPage]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setError('');
      try {
        let pageData;
        if (searchKeyword) {
          pageData = await bookService.searchBooks(searchKeyword, currentPage, 20);
        } else if (filter === 'new') {
          pageData = await bookService.getNewBooks({ page: currentPage, size: 20 });
        } else {
          pageData = await bookService.getBooks({ page: currentPage, size: 20 });
        }

        if (cancelled) return;
        setBooks(pageData.content);
        setTotalPages(pageData.totalPages);
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
  }, [searchKeyword, filter, currentPage]);

  const handlePrevPage = () => {
    if (currentPage > 0) setCurrentPage(p => p - 1);
  };

  const handleNextPage = () => {
    if (currentPage < totalPages - 1) setCurrentPage(p => p + 1);
  };

  const handlePageInputChange = (e) => {
    setPageInput(e.target.value);
  };

  const handlePageInputSubmit = (e) => {
    if (e) e.preventDefault();
    const pageNum = parseInt(pageInput, 10);
    if (!isNaN(pageNum) && pageNum > 0 && pageNum <= totalPages) {
      setCurrentPage(pageNum - 1);
    } else {
      setPageInput(String(currentPage + 1)); // reset to valid
    }
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
        {searchKeyword ? `Search Results for "${searchKeyword}"` : (filter === 'new' ? 'New Books' : 'Books')}
      </h1>

      {!books.length ? (
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

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="mt-12 flex items-center justify-center gap-4">
              <button
                onClick={handlePrevPage}
                disabled={currentPage === 0}
                className={`p-2 rounded-lg flex items-center justify-center transition-colors ${currentPage === 0
                    ? 'text-gray-400 bg-gray-100 cursor-not-allowed'
                    : 'text-blue-600 bg-blue-50 hover:bg-blue-100'
                  }`}
              >
                <ChevronLeft className="w-5 h-5" />
              </button>

              <form onSubmit={handlePageInputSubmit} className="flex items-center gap-2">
                <span className="text-gray-600 font-medium">Page</span>
                <input
                  type="number"
                  value={pageInput}
                  onChange={handlePageInputChange}
                  onBlur={handlePageInputSubmit}
                  min={1}
                  max={totalPages}
                  className="w-16 px-2 py-1 text-center border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <span className="text-gray-600 font-medium">of {totalPages}</span>
              </form>

              <button
                onClick={handleNextPage}
                disabled={currentPage >= totalPages - 1}
                className={`p-2 rounded-lg flex items-center justify-center transition-colors ${currentPage >= totalPages - 1
                    ? 'text-gray-400 bg-gray-100 cursor-not-allowed'
                    : 'text-blue-600 bg-blue-50 hover:bg-blue-100'
                  }`}
              >
                <ChevronRight className="w-5 h-5" />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default BooksPage;