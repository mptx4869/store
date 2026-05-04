import { useEffect, useState } from 'react';
import { BookCarousel } from '../components/features';
import bookService from '../services/bookService';
import recommendationService from '../services/recommendationService';
import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useCart, useAuth } from '../context';

function HomePage() {
  const { addToCart } = useCart();
  const { user } = useAuth();

  const [newBooks, setNewBooks] = useState([]);
  const [isLoadingNew, setIsLoadingNew] = useState(true);
  const [newError, setNewError] = useState('');

  const [recommendedBooks, setRecommendedBooks] = useState([]);
  const [isLoadingRec, setIsLoadingRec] = useState(true);
  const [recError, setRecError] = useState('');

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

    const loadRecommendations = async () => {
      setIsLoadingRec(true);
      setRecError('');
      try {
        const usernameToFetch = user?.username || 'feacb58b496bae6c38aa1b07651de21a';
        const validBooks = await recommendationService.getRecommendations(usernameToFetch, 10);

        console.log('Recommendation API Result:', validBooks);

        if (validBooks.length === 0) {
          throw new Error('No recommendations available');
        }

        if (isMounted) setRecommendedBooks(validBooks);
      } catch (err) {
        if (isMounted) {
          setRecError(err.message || 'Failed to fetch recommendations');
          console.error('Recommendation API error:', err);
          setRecommendedBooks([]);
        }
      } finally {
        if (isMounted) setIsLoadingRec(false);
      }
    };

    loadNewBooks();
    loadRecommendations();

    return () => {
      isMounted = false;
    };
  }, [user?.username]);

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
              Discover the World of Books
            </h1>
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

      {/* Recommended for You Section */}
      <BookCarousel
        title="Recommended for You"
        subtitle="Personalized book suggestions"
        books={recommendedBooks}
        isLoading={isLoadingRec}
        error={recError}
        onAddToCart={handleAddToCart}
      />

      {/* Best Sellers Section - temporarily reuse New Books API */}
      <BookCarousel
        title="Bestsellers"
        subtitle="Most popular books"
        books={newBooks}
        isLoading={isLoadingNew}
        error={newError}
        onAddToCart={handleAddToCart}
        viewAllLink="/bestsellers"
      />

      {/* New Books Section */}
      <BookCarousel
        title="New Books"
        subtitle="Just released this week"
        books={newBooks}
        isLoading={isLoadingNew}
        error={newError}
        onAddToCart={handleAddToCart}
        viewAllLink="/books?filter=new"
      />

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