import { useEffect, useState } from 'react';
import { BookCarousel } from '../components/features';
import bookService from '../services/bookService';
import recommendationService from '../services/recommendationService';
import { Link } from 'react-router-dom';
import { useAuth } from '../context';

function HomePage() {
  const { user } = useAuth();

  const [newBooks, setNewBooks] = useState([]);
  const [isLoadingNew, setIsLoadingNew] = useState(true);
  const [newError, setNewError] = useState('');

  const [bestSellerBooks, setBestSellerBooks] = useState([]);
  const [isLoadingBest, setIsLoadingBest] = useState(true);
  const [bestError, setBestError] = useState('');

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
          size: 20,
          sortBy: 'createdAt',
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

        if (validBooks.length === 0) throw new Error('No recommendations available');

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

    const loadBestSellers = async () => {
      setIsLoadingBest(true);
      setBestError('');
      try {
        const result = await bookService.getBestSellers({
          days: 0,
          limit: 20,
        });
        if (isMounted) setBestSellerBooks(result);
      } catch (err) {
        if (isMounted) setBestError(err.message || 'Could not load bestsellers');
      } finally {
        if (isMounted) setIsLoadingBest(false);
      }
    };

    loadNewBooks();
    loadRecommendations();
    loadBestSellers();

    return () => { isMounted = false; };
  }, [user?.username]);

  return (
    <div>
      {/* ── Hero Section ── */}
      <section className="relative bg-gradient-to-br from-blue-900 via-blue-800 to-indigo-900 text-white overflow-hidden">
        {/* Background glows */}
        <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-blue-500/20 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-1/4 w-64 h-64 bg-indigo-500/20 rounded-full translate-y-1/2 blur-3xl pointer-events-none" />

        <div className="container mx-auto px-4 py-20 relative z-10">
          <div className="max-w-2xl">
            <h1 className="text-4xl md:text-5xl lg:text-6xl font-extrabold leading-tight mb-6 tracking-tight">
              Discover the{' '}
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-300 to-indigo-200">
                World of Books
              </span>
            </h1>

            <p className="text-blue-100 text-lg mb-8 max-w-lg leading-relaxed">
              Explore thousands of titles across every genre. Find your next favorite read today.
            </p>

            <div className="flex flex-wrap gap-3">
              <Link
                to="/books"
                className="bg-white text-blue-700 px-7 py-3 rounded-xl font-semibold
                           hover:bg-blue-50 hover:-translate-y-0.5 hover:shadow-lg
                           transition-all duration-200"
              >
                Explore Now
              </Link>
              <Link
                to="/bestsellers"
                className="bg-white/10 backdrop-blur-sm border border-white/30 text-white px-7 py-3 rounded-xl font-semibold
                           hover:bg-white/20 hover:-translate-y-0.5
                           transition-all duration-200"
              >
                Bestsellers
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* ── Book carousels ── */}
      {/* BookCard handles its own cart logic via useCart + getBookById internally */}
      <BookCarousel
        title="Recommended for You"
        subtitle="Personalized book suggestions"
        books={recommendedBooks}
        isLoading={isLoadingRec}
        error={recError}
      />

      <BookCarousel
        title="Bestsellers"
        subtitle="Most popular books right now"
        books={bestSellerBooks}
        isLoading={isLoadingBest}
        error={bestError}
        viewAllLink="/bestsellers"
      />

      <BookCarousel
        title="New Arrivals"
        subtitle="Just released this week"
        books={newBooks}
        isLoading={isLoadingNew}
        error={newError}
        viewAllLink="/books?filter=new"
      />

      {/* ── Promotion Banner (content was already in original code, just un-commented) ── */}
      <section className="py-14 bg-slate-50">
        <div className="container mx-auto px-4">
          <div className="relative overflow-hidden bg-gradient-to-r from-orange-500 to-rose-500 rounded-3xl p-10 md:p-14 text-white text-center">
            <div className="absolute -top-8 -right-8 w-48 h-48 bg-white/10 rounded-full blur-2xl pointer-events-none" />

            <div className="relative z-10">
              <h2 className="text-3xl md:text-4xl font-extrabold mb-3">
                Explore the world of books
              </h2>
              <p className="text-xl mb-2 text-orange-100">
                A reader lives a thousand lives before he dies. The man who never reads lives only {' '}
                <strong className="text-white bg-white/20 px-3 py-0.5 rounded-lg">ONE</strong>
              </p>
              <Link
                to="/books"
                className="inline-flex items-center gap-2 mt-6 bg-white text-orange-600 px-8 py-3
                           rounded-xl font-semibold hover:bg-orange-50 hover:-translate-y-0.5 hover:shadow-lg
                           transition-all duration-200"
              >
                Shop Now
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default HomePage;
