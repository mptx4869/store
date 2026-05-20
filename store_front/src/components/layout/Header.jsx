import { useState, useRef, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  Search,
  ShoppingCart,
  User,
  Menu,
  X,
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
  const location = useLocation();

  const { totalItems } = useCart();
  const { user, isAuthenticated, logout } = useAuth();

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
      navigate(`/books?q=${encodeURIComponent(searchQuery.trim())}`);
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

  const isActiveLink = (to) => {
    const current = `${location.pathname}${location.search}`;
    return current === to;
  };

  return (
    <header className="bg-white border-b border-slate-100 sticky top-0 z-50 shadow-sm">
      {/* ── Main header row ── */}
      <div className="container mx-auto px-4 py-3">
        <div className="flex items-center justify-between gap-4">

          {/* Logo */}
          <Link to="/" className="flex items-center flex-shrink-0">
            <div className="bg-white rounded-xl p-1.5 shadow-sm border border-slate-100">
              <img src="/NH_Store_logo.png" alt="BookStore" className="h-20 w-auto object-contain" />
            </div>
          </Link>

          {/* Search bar — hidden on mobile */}
          <form onSubmit={handleSearch} className="hidden md:flex flex-1 max-w-xl">
            <div className="relative w-full">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
              <input
                type="text"
                placeholder="Search books, authors..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-11 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-full
                           text-slate-900 placeholder:text-slate-400 text-sm
                           focus:bg-white focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500
                           outline-none transition-all duration-200"
              />
            </div>
          </form>

          {/* Actions */}
          <div className="flex items-center gap-2">

            {/* User menu */}
            {isAuthenticated ? (
              <div className="relative" ref={userMenuRef}>
                <button
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  className="hidden sm:flex items-center gap-2 pl-2 pr-3 py-1.5 rounded-full
                             hover:bg-slate-100 transition-colors duration-200"
                >
                  <img
                    src={user.avatar}
                    alt={user.username}
                    className="w-7 h-7 rounded-full ring-2 ring-blue-100 object-cover"
                  />
                  <span className="text-sm font-medium text-slate-700 max-w-[6rem] truncate">
                    {user.username}
                  </span>
                  <ChevronDown
                    className={`w-3.5 h-3.5 text-slate-500 transition-transform duration-200 ${showUserMenu ? 'rotate-180' : ''}`}
                  />
                </button>

                {/* Dropdown */}
                {showUserMenu && (
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-2xl shadow-xl border border-slate-100 py-1.5 z-50 animate-slide-up">
                    <div className="px-4 py-3 border-b border-slate-100">
                      <p className="font-semibold text-slate-800 truncate text-sm">{user.username}</p>
                      <p className="text-xs text-slate-500 mt-0.5">{user.role || 'USER'}</p>
                    </div>

                    <div className="py-1">
                      {/*
                      <Link
                        to="/profile"
                        onClick={() => setShowUserMenu(false)}
                        className="flex items-center gap-3 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
                      >
                        <User className="w-4 h-4 text-slate-400" />
                        My Account
                      </Link>
                      */}
                      <Link
                        to="/orders"
                        onClick={() => setShowUserMenu(false)}
                        className="flex items-center gap-3 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
                      >
                        <Package className="w-4 h-4 text-slate-400" />
                        Orders
                      </Link>
                      {/*
                      <Link
                        to="/wishlist"
                        onClick={() => setShowUserMenu(false)}
                        className="flex items-center gap-3 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
                      >
                        <Heart className="w-4 h-4 text-slate-400" />
                        Wishlist
                      </Link>
                      */}
                      <Link
                        to="/settings/change-password"
                        onClick={() => setShowUserMenu(false)}
                        className="flex items-center gap-3 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
                      >
                        <Settings className="w-4 h-4 text-slate-400" />
                        Change Password
                      </Link>
                    </div>

                    <div className="border-t border-slate-100 pt-1">
                      <button
                        onClick={handleLogout}
                        className="flex items-center gap-3 px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors w-full"
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
                className="hidden sm:flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium
                           text-slate-600 hover:bg-slate-100 transition-colors duration-200"
              >
                <User className="w-4 h-4" />
                Login
              </Link>
            )}

            {/* Cart */}
            <Link
              to="/cart"
              className="relative flex items-center gap-2 px-3 py-2 rounded-full
                         text-slate-600 hover:bg-slate-100 transition-colors duration-200"
            >
              <ShoppingCart className="w-5 h-5" />
              <span className="hidden sm:inline text-sm font-medium">Cart</span>
              {totalItems > 0 && (
                <span className="absolute -top-0.5 -right-0.5 w-5 h-5 bg-gradient-to-br from-blue-500 to-indigo-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center shadow-sm">
                  {totalItems > 99 ? '99+' : totalItems}
                </span>
              )}
            </Link>

            {/* Mobile menu toggle */}
            <button
              onClick={() => setIsMenuOpen(!isMenuOpen)}
              className="md:hidden p-2 rounded-full text-slate-600 hover:bg-slate-100 transition-colors"
            >
              {isMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Mobile search */}
        <form onSubmit={handleSearch} className="mt-3 md:hidden">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
            <input
              type="text"
              placeholder="Search books, authors..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-11 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-full
                         text-slate-900 placeholder:text-slate-400 text-sm
                         focus:bg-white focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500
                         outline-none transition-all duration-200"
            />
          </div>
        </form>
      </div>

      {/* ── Navigation bar ── */}
      <nav className="border-t border-slate-100">
        <div className="container mx-auto px-4">
          <ul className="hidden md:flex items-center gap-1 py-1">
            {[
              { to: '/', label: 'Home' },
              { to: '/books?filter=new', label: 'New Books' },
              { to: '/bestsellers', label: 'Bestsellers' },
              { to: '/books', label: 'All Books' },
            ].map(({ to, label }) => (
              <li key={to}>
                <Link
                  to={to}
                  className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 ${
                    isActiveLink(to)
                      ? 'text-blue-700 bg-blue-50 ring-1 ring-blue-200'
                      : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50'
                  }`}
                >
                  {label}
                </Link>
              </li>
            ))}
            <li>
              <Link
                to="/books?discount=true"
                className="px-4 py-2 rounded-full text-sm font-semibold text-rose-600
                           hover:bg-rose-50 transition-all duration-200"
              >
                
              </Link>
            </li>
          </ul>
        </div>
      </nav>

      {/* ── Mobile slide-down menu ── */}
      {isMenuOpen && (
        <div className="md:hidden bg-white border-t border-slate-100 animate-slide-up">
          <div className="container mx-auto px-4 py-4 space-y-1">
            {[
              { to: '/', label: 'Home' },
              { to: '/books?filter=new', label: 'New Books' },
              { to: '/bestsellers', label: 'Bestsellers' },
              { to: '/books', label: 'All Books' },
            ].map(({ to, label }) => (
              <Link
                key={to}
                to={to}
                onClick={closeMenu}
                className={`block px-3 py-2.5 rounded-xl font-medium text-sm transition-colors ${
                  isActiveLink(to)
                    ? 'text-blue-700 bg-blue-50 ring-1 ring-blue-200'
                    : 'text-slate-700 hover:bg-slate-50'
                }`}
              >
                {label}
              </Link>
            ))}
            <Link
              to="/books?discount=true"
              onClick={closeMenu}
              className="block px-3 py-2.5 rounded-xl text-rose-600 hover:bg-rose-50 font-semibold text-sm transition-colors"
            >
             
            </Link>

            {/* Mobile user section */}
            <div className="pt-3 mt-3 border-t border-slate-100">
              {isAuthenticated ? (
                <div className="space-y-1">
                  <div className="flex items-center gap-3 px-3 py-3 bg-slate-50 rounded-xl mb-2">
                    <img src={user.avatar} alt={user.username} className="w-10 h-10 rounded-full ring-2 ring-blue-100" />
                    <div>
                      <p className="font-semibold text-slate-800 text-sm">{user.username}</p>
                      <p className="text-xs text-slate-500">{user.role || 'USER'}</p>
                    </div>
                  </div>
                  {[
                    { to: '/profile', icon: <User className="w-4 h-4" />, label: 'My Account' },
                    { to: '/orders', icon: <Package className="w-4 h-4" />, label: 'Orders' },
                    { to: '/wishlist', icon: <Heart className="w-4 h-4" />, label: 'Wishlist' },
                    { to: '/settings/change-password', icon: <Settings className="w-4 h-4" />, label: 'Change Password' },
                  ].map(({ to, icon, label }) => (
                    <Link
                      key={to}
                      to={to}
                      onClick={closeMenu}
                      className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-700 hover:bg-slate-50 text-sm transition-colors"
                    >
                      <span className="text-slate-400">{icon}</span>
                      {label}
                    </Link>
                  ))}
                  <button
                    onClick={handleLogout}
                    className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-red-600 hover:bg-red-50 text-sm w-full transition-colors"
                  >
                    <LogOut className="w-4 h-4" />
                    Logout
                  </button>
                </div>
              ) : (
                <div className="flex flex-col gap-2">
                  <Link to="/login" onClick={closeMenu} className="btn-primary w-full justify-center py-2.5 text-sm">
                    Login
                  </Link>
                  <Link to="/register" onClick={closeMenu} className="btn-secondary w-full justify-center py-2.5 text-sm">
                    Register
                  </Link>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}

export default Header;
