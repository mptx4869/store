import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from 'react';
import cartService from '../services/cartService';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [items, setItems] = useState([]);
  const [totalPrice, setTotalPrice] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { isAuthenticated } = useAuth();

  const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);

  const refreshCart = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const data = await cartService.getCart();
      setItems(data.items);
      setTotalPrice(data.totalPrice);
    } catch (err) {
      if (err.message. includes('log in')) {
        setItems([]);
        setTotalPrice(0);
      } else {
        setError(err.message);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Load cart on first mount
  useEffect(() => {
    refreshCart();
  }, [refreshCart]);

  // Listen to logout event → reset cart
  useEffect(() => {
    const handleLogout = () => {
      setItems([]);
      setTotalPrice(0);
      setError('');
    };

    window.addEventListener('auth:logout', handleLogout);
    return () => window.removeEventListener('auth:logout', handleLogout);
  }, []);

  // Listen to login event → refresh cart
  useEffect(() => {
    const handleLogin = () => {
      refreshCart();
    };

    window.addEventListener('auth:login', handleLogin);
    return () => window.removeEventListener('auth:login', handleLogin);
  }, [refreshCart]);

  // Listen to checkout success event → refresh cart (or clear)
  useEffect(() => {
    const handleCheckoutSuccess = () => {
      refreshCart(); // backend already cleared cart → refresh to sync UI
    };

    window.addEventListener('checkout:success', handleCheckoutSuccess);
    return () => window.removeEventListener('checkout:success', handleCheckoutSuccess);
  }, [refreshCart]);

  const addToCart = useCallback(
    async (skuId, quantity = 1) => {
      if (!isAuthenticated) {
        window.dispatchEvent(
          new CustomEvent('auth:login-required', {
            detail: { reason: 'add-to-cart' },
          })
        );
        throw new Error('Please sign in to add items to your cart.');
      }

      setIsLoading(true);
      setError('');
      try {
        const data = await cartService. addToCart(skuId, quantity);
        setItems(data.items);
        setTotalPrice(data.totalPrice);
      } catch (err) {
        setError(err.message);
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [isAuthenticated]
  );

  const updateQuantity = useCallback(async (itemId, quantity) => {
    if (quantity <= 0) return;

    setIsLoading(true);
    setError('');
    try {
      const data = await cartService.updateQuantity(itemId, quantity);
      setItems(data.items);
      setTotalPrice(data.totalPrice);
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const removeFromCart = useCallback(async (itemId) => {
    setIsLoading(true);
    setError('');
    try {
      const data = await cartService. removeFromCart(itemId);
      setItems(data.items);
      setTotalPrice(data.totalPrice);
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearCart = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      await cartService.clearCart();
      setItems([]);
      setTotalPrice(0);
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(''), []);

  const value = {
    items,
    totalPrice,
    itemCount,
    isLoading,
    error,
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    clearError,
    refreshCart,
  };

  return <CartContext.Provider value={value}>{children}</CartContext. Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within a CartProvider');
  return ctx;
}