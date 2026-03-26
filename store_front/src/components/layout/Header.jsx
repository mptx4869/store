import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Search,
  ShoppingCart,
  User,
  Menu,
  X,
  Book,
  LogOut,
  Settings,
  Package,
  Heart,
  ChevronDown,
} from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';

function Header() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showUserMenu, setShowUserMenu] = useState(false);
  const userMenuRef = useRef(null);
  const navigate = useNavigate();

  const { totalItems } = useCart();
  const { user, isAuthenticated, logout } = useAuth();

  // Close menu when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target)) {
        setShowUserMenu(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/books?q=${encodeURIComponent(searchQuery. trim())}`);
      setSearchQuery('');
      setIsMenuOpen(false);
    }
  };

  const handleLogout = () => {
    logout();
    setShowUserMenu(false);
    setIsMenuOpen(false);
    navigate('/');
  };

  const closeMenu = () => setIsMenuOpen(false);

  return (
    <header className="bg-white shadow-md sticky top-0 z-50">
      {/* Top bar */}
      <div className="bg-blue-600 text-white text-sm py-2">
        <div className="container mx-auto px-4 text-center">
        </div>
      </div>

      {/* Main header */}
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between gap-4">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 flex-shrink-0">
            <Book className="w-8 h-8 text-blue-600" />
            <span className="text-2xl font-bold text-gray-800">
              Book<span className="text-blue-600">Store</span>
            </span>
          </Link>

          {/* Search bar - Hidden on mobile */}
          <form onSubmit={handleSearch} className="hidden md:flex flex-1 max-w-xl">
            <div className="relative w-full">
              <input
                type="text"
                placeholder="Search books, authors..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e. target.value)}
                className="input-field pr-12"
              />
              <button
                type="submit"
                className="absolute right-2 top-1/2 -translate-y-1/2 p-2 
                         text-gray-500 hover:text-blue-600 transition-colors"
              >
                <Search className="w-5 h-5" />
              </button>
            </div>
          </form>

          {/* Actions */}
          <div className="flex items-center gap-4">
            {/* User Menu */}
            {isAuthenticated ?  (
              <div className="relative" ref={userMenuRef}>
                <button
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  className="hidden sm:flex items-center gap-2 text-gray-600 
                           hover:text-blue-600 transition-colors"
                >
                  <img
                    src={user.avatar}
                    alt={user.username}
                    className="w-8 h-8 rounded-full border-2 border-gray-200"
                  />
                  <span className="text-sm font-medium max-w-24 truncate">
                    {user.username}
                  </span>
                  <ChevronDown
                    className={`w-4 h-4 transition-transform ${
                      showUserMenu ?  'rotate-180' : ''
                    }`}
                  />
                </button>

                {/* User dropdown menu */}
                {showUserMenu && (
                  <div
                    className="absolute right-0 mt-2 w-56 bg-white rounded-xl 
                              shadow-lg border py-2 z-50"
                  >
                    {/* User info */}
                    <div className="px-4 py-3 border-b">
                      <p className="font-medium text-gray-800 truncate">
                        {user.username}
                      </p>
                      <p className="text-sm text-gray-500 truncate">
                        Role: {user.role || 'USER'}
                      </p>
                    </div>

                    {/* Menu items */}
                    <div className="py-2">
                      <Link
                        to="/profile"
                        onClick={() => setShowUserMenu(false)}
                        className="flex items-center gap-3 px-4 py-2 text-gray-700 
                                 hover:bg-gray-50 transition-colors"
                      >
                        <User className="w-4 h-4" />
                        My Account
                      </Link>
                      <Link
                        to="/orders"
                        onClick={() => setShowUserMenu(false)}
                        className="flex items-center gap-3 px-4 py-2 text-gray-700 
                                 hover:bg-gray-50 transition-colors"
                      >
                        <Package className="w-4 h-4" />
                        Orders
                      </Link>
                      <Link
                        to="/wishlist"
                        onClick={() => setShowUserMenu(false)}
                        className="flex items-center gap-3 px-4 py-2 text-gray-700 
                                 hover:bg-gray-50 transition-colors"
                      >
                        <Heart className="w-4 h-4" />
                        Wishlist
                      </Link>
                      <Link
                          to="/settings/change-password"
                          onClick={() => setShowUserMenu(false)}
                          className="flex items-center gap-3 px-4 py-2 text-gray-700 
                                   hover:bg-gray-50 transition-colors"
                        >
                          <Settings className="w-4 h-4" />
                          Change Password
                        </Link>
                    </div>

                    {/* Logout */}
                    <div className="border-t pt-2">
                      <button
                        onClick={handleLogout}
                        className="flex items-center gap-3 px-4 py-2 text-red-600 
                                 hover:bg-red-50 transition-colors w-full"
                      >
                        <LogOut className="w-4 h-4" />
                        Logout
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <Link
                to="/login"
                className="hidden sm:flex items-center gap-2 text-gray-600 
                         hover:text-blue-600 transition-colors"
              >
                <User className="w-6 h-6" />
                <span className="text-sm font-medium">Login</span>
              </Link>
            )}

            {/* Cart */}
            <Link
              to="/cart"
              className="relative flex items-center gap-2 text-gray-600 
                       hover:text-blue-600 transition-colors"
            >
              <ShoppingCart className="w-6 h-6" />
              <span className="hidden sm:inline text-sm font-medium">
                Cart
              </span>
              {totalItems > 0 && (
                <span
                  className="absolute -top-2 -right-2 bg-red-500 text-white 
                               text-xs w-5 h-5 rounded-full flex items-center 
                               justify-center font-bold"
                >
                  {totalItems > 99 ? '99+' : totalItems}
                </span>
              )}
            </Link>

            {/* Mobile menu button */}
            <button
              onClick={() => setIsMenuOpen(!isMenuOpen)}
              className="md:hidden p-2 text-gray-600 hover:text-blue-600"
            >
              {isMenuOpen ?  (
                <X className="w-6 h-6" />
              ) : (
                <Menu className="w-6 h-6" />
              )}
            </button>
          </div>
        </div>

        {/* Mobile search */}
        <form onSubmit={handleSearch} className="mt-4 md:hidden">
          <div className="relative">
            <input
              type="text"
              placeholder="Search books, authors..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e. target.value)}
              className="input-field pr-12"
            />
            <button
              type="submit"
              className="absolute right-2 top-1/2 -translate-y-1/2 p-2 
                       text-gray-500 hover:text-blue-600"
            >
              <Search className="w-5 h-5" />
            </button>
          </div>
        </form>
      </div>

      {/* Navigation */}
      <nav className="bg-gray-50 border-t">
        <div className="container mx-auto px-4">
          <ul className="hidden md:flex items-center gap-8 py-3">
            <li>
              <Link
                to="/"
                className="text-gray-700 hover:text-blue-600 font-medium transition-colors"
              >
                Home
              </Link>
            </li>
            <li>
              <Link
                to="/books? filter=new"
                className="text-gray-700 hover:text-blue-600 font-medium transition-colors"
              >
                New Books
              </Link>
            </li>
            <li>
              <Link
                to="/bestsellers"
                className="text-gray-700 hover:text-blue-600 font-medium transition-colors"
              >
                Bestsellers
              </Link>
            </li>
            <li>
              <Link
                to="/books"
                className="text-gray-700 hover:text-blue-600 font-medium transition-colors"
              >
                All Books
              </Link>
            </li>
            <li>
              <Link
                to="/books? discount=true"
                className="text-red-500 hover:text-red-600 font-medium transition-colors"
              >
                🔥 Promotions
              </Link>
            </li>
          </ul>
        </div>
      </nav>

      {/* Mobile menu */}
      {isMenuOpen && (
        <div className="md:hidden bg-white border-t">
          <ul className="container mx-auto px-4 py-4 space-y-2">
            <li>
              <Link
                to="/"
                onClick={closeMenu}
                className="block text-gray-700 hover:text-blue-600 font-medium py-2"
              >
                Home
              </Link>
            </li>
            <li>
              <Link
                to="/books? filter=new"
                onClick={closeMenu}
                className="block text-gray-700 hover:text-blue-600 font-medium py-2"
              >
                New Books
              </Link>
            </li>
            <li>
              <Link
                to="/bestsellers"
                onClick={closeMenu}
                className="block text-gray-700 hover:text-blue-600 font-medium py-2"
              >
                Bestsellers
              </Link>
            </li>
            <li>
              <Link
                to="/books"
                onClick={closeMenu}
                className="block text-gray-700 hover:text-blue-600 font-medium py-2"
              >
                All Books
              </Link>
            </li>
            <li>
              <Link
                to="/books?discount=true"
                onClick={closeMenu}
                className="block text-red-500 hover:text-red-600 font-medium py-2"
              >
                🔥 Promotions
              </Link>
            </li>

            {/* Mobile User Section */}
            <li className="pt-4 border-t mt-4">
              {isAuthenticated ? (
                <div className="space-y-2">
                  {/* User Info */}
                  <div className="flex items-center gap-3 py-2">
                    <img
                      src={user.avatar}
                      alt={user.username}
                      className="w-10 h-10 rounded-full border-2 border-gray-200"
                    />
                    <div>
                      <p className="font-medium text-gray-800">{user.username}</p>
                      <p className="text-sm text-gray-500">
                        Role: {user.role || 'USER'}
                      </p>
                    </div>
                  </div>

                  {/* Menu Links */}
                  <Link
                    to="/profile"
                    onClick={closeMenu}
                    className="flex items-center gap-2 text-gray-700 hover:text-blue-600 py-2"
                  >
                    <User className="w-5 h-5" />
                    Account
                  </Link>
                  <Link
                    to="/orders"
                    onClick={closeMenu}
                    className="flex items-center gap-2 text-gray-700 hover:text-blue-600 py-2"
                  >
                    <Package className="w-5 h-5" />
                    Orders
                  </Link>
                  <Link
                    to="/wishlist"
                    onClick={closeMenu}
                    className="flex items-center gap-2 text-gray-700 hover:text-blue-600 py-2"
                  >
                    <Heart className="w-5 h-5" />
                    Wishlist
                  </Link>
                  <Link
                      to="/settings/change-password"
                      onClick={closeMenu}
                      className="flex items-center gap-2 text-gray-700 hover:text-blue-600 py-2"
                    >
                      <Settings className="w-5 h-5" />
                      Change Password
                    </Link>

                  {/* Logout Button */}
                  <button
                    onClick={handleLogout}
                    className="flex items-center gap-2 text-red-600 py-2 w-full"
                  >
                    <LogOut className="w-5 h-5" />
                    Logout
                  </button>
                </div>
              ) : (
                <div className="space-y-2">
                  <Link
                    to="/login"
                    onClick={closeMenu}
                    className="block w-full btn-primary text-center py-2"
                  >
                    Login
                  </Link>
                  <Link
                    to="/register"
                    onClick={closeMenu}
                    className="block w-full btn-secondary text-center py-2"
                  >
                    Register
                  </Link>
                </div>
              )}
            </li>
          </ul>
        </div>
      )}
    </header>
  );
}

export default Header;