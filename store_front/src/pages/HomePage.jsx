import { useEffect, useState } from 'react';
import { BookCard } from '../components/features';
import bookService from '../services/bookService';
import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useCart } from '../context';

function HomePage() {
  const { addToCart } = useCart();

  const [newBooks, setNewBooks] = useState([]);
  const [isLoadingNew, setIsLoadingNew] = useState(true);
  const [newError, setNewError] = useState('');

  useEffect(() => {
    let isMounted = true;

    const loadNewBooks = async () => {
      setIsLoadingNew(true);
      setNewError('');
      try {
        const result = await bookService.getNewBooks({
          page: 0,
          size: 6,
          sortBy: 'publishedDate',
          sortDirection: 'DESC',
        });
        if (isMounted) setNewBooks(result.content);
      } catch (err) {
        if (isMounted) setNewError(err.message || 'Could not load new books');
      } finally {
        if (isMounted) setIsLoadingNew(false);
      }
    };

    loadNewBooks();

    return () => {
      isMounted = false;
    };
  }, []);

  const handleAddToCart = (book) => {
    addToCart(book);
    alert(`Added "${book.title}" to cart!`);
  };

  return (
    <div>
      {/* Hero Section */}
      <section className="bg-gradient-to-r from-blue-600 to-blue-800 text-white">
        <div className="container mx-auto px-4 py-16">
          <div className="max-w-2xl">
            <h1 className="text-4xl md:text-5xl font-bold mb-6">
              Discover the World of Books 📚
            </h1>
            {/* <p className="text-xl text-blue-100 mb-8">
              Thousands of great books are waiting for you. Free shipping for orders from 300,000đ.
            </p> */}
            <div className="flex flex-wrap gap-4">
              <Link
                to="/books"
                className="bg-white text-blue-600 px-8 py-3 rounded-lg 
                         font-semibold hover:bg-blue-50 transition-colors"
              >
                Explore now
              </Link>
              <Link
                to="/bestsellers"
                className="border-2 border-white text-white px-8 py-3 rounded-lg 
                         font-semibold hover:bg-white/10 transition-colors"
              >
                Bestsellers
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Best Sellers Section - temporarily reuse New Books API */}
      <section className="py-12">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-2xl md:text-3xl font-bold text-gray-800">
                Bestsellers
              </h2>
              <p className="text-gray-500 mt-1">
                Most popular books
              </p>
            </div>
            <Link
              to="/bestsellers"
              className="flex items-center gap-1 text-blue-600 hover:text-blue-700 
                       font-medium"
            >
              View All
              <ChevronRight className="w-5 h-5" />
            </Link>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 
                        lg:grid-cols-5 xl:grid-cols-6 gap-4 md:gap-6">
            {isLoadingNew ? (
              Array.from({ length: 6 }).map((_, idx) => (
                <div
                  key={idx}
                  className="animate-pulse bg-white rounded-xl shadow-sm p-3 h-56"
                >
                  <div className="bg-gray-200 h-32 rounded mb-3" />
                  <div className="bg-gray-200 h-4 rounded mb-2" />
                  <div className="bg-gray-200 h-4 w-1/2 rounded" />
                </div>
              ))
            ) : newError ? (
              <div className="col-span-full bg-red-50 text-red-700 border border-red-100 rounded-lg p-4">
                {newError}
              </div>
            ) : newBooks.length === 0 ? (
              <div className="col-span-full bg-white rounded-xl shadow-sm p-8 text-center text-gray-600">
                No books available.
              </div>
            ) : (
              newBooks.map((book) => (
                <BookCard
                  key={book.id}
                  book={book}
                  onAddToCart={handleAddToCart}
                />
              ))
            )}
          </div>
        </div>
      </section>

      {/* New Books Section */}
      <section className="py-12 bg-gray-50">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-2xl md:text-3xl font-bold text-gray-800">
                New Books
              </h2>
              <p className="text-gray-500 mt-1">
                Just released this week
              </p>
            </div>
            <Link
              to="/books?filter=new"
              className="flex items-center gap-1 text-blue-600 hover:text-blue-700 
                       font-medium"
            >
              View All
              <ChevronRight className="w-5 h-5" />
            </Link>
          </div>

          {newError && (
            <div className="bg-red-50 text-red-700 border border-red-100 rounded-lg p-4 mb-4">
              {newError}
            </div>
          )}

          {isLoadingNew ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 md:gap-6">
              {Array.from({ length: 6 }).map((_, idx) => (
                <div
                  key={idx}
                  className="animate-pulse bg-white rounded-xl shadow-sm p-3 h-56"
                >
                  <div className="bg-gray-200 h-32 rounded mb-3" />
                  <div className="bg-gray-200 h-4 rounded mb-2" />
                  <div className="bg-gray-200 h-4 w-1/2 rounded" />
                </div>
              ))}
            </div>
          ) : newBooks.length === 0 ? (
            <div className="bg-white rounded-xl shadow-sm p-8 text-center text-gray-600">
              No new books available.
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 
                          lg:grid-cols-5 xl:grid-cols-6 gap-4 md:gap-6">
              {newBooks.map((book) => (
                <BookCard key={book.id} book={book} onAddToCart={handleAddToCart} />
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Promotion Banner */}
      {/* <section className="py-12">
        <div className="container mx-auto px-4">
          <div className="bg-gradient-to-r from-orange-500 to-red-500 
                        rounded-2xl p-8 md:p-12 text-white text-center">
            <h2 className="text-3xl md:text-4xl font-bold mb-4">
              🎁 Special Offer
            </h2>
            <p className="text-xl mb-6 text-orange-100">
              Extra 10% off your first order with code: <strong>WELCOME10</strong>
            </p>
            <Link
              to="/books"
              className="inline-block bg-white text-orange-600 px-8 py-3 
                       rounded-lg font-semibold hover:bg-orange-50 transition-colors"
            >
              Shop Now
            </Link>
          </div>
        </div>
      </section> */}
    </div>
  );
}

export default HomePage;