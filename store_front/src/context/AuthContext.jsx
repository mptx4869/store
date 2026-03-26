import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useRef,
} from 'react';
import { useLocation } from 'react-router-dom';
import authService from '../services/authService';

const AuthContext = createContext(null);

function normalizeRole(role) {
  if (! role) return '';
  const r = String(role).trim().toUpperCase();
  return r.startsWith('ROLE_') ? r. slice('ROLE_'.length) : r;
}

function decodeJwtPayload(token) {
  if (!token) return null;
  const parts = String(token).split('.');
  if (parts.length < 2) return null;

  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);

  try {
    const raw = atob(padded);
    const utf8Payload = new TextDecoder().decode(
      Uint8Array.from(raw, (c) => c.charCodeAt(0))
    );
    return JSON.parse(utf8Payload);
  } catch {
    return null;
  }
}

function getTokenExpirationMs(token) {
  const payload = decodeJwtPayload(token);
  const exp = payload?.exp;
  if (!exp || Number.isNaN(exp)) return null;
  return Number(exp) * 1000;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const tokenExpiryTimer = useRef(null);
  const location = useLocation();

  const clearExpiryTimer = useCallback(() => {
    if (tokenExpiryTimer.current) {
      clearTimeout(tokenExpiryTimer.current);
      tokenExpiryTimer.current = null;
    }
  }, []);

  const scheduleExpiryCheck = useCallback(
    (token) => {
      clearExpiryTimer();
      const expMs = getTokenExpirationMs(token);
      if (!expMs) return;

      const msUntilExpiry = expMs - Date.now();

      if (msUntilExpiry <= 0) {
        window.dispatchEvent(new Event('auth:expired'));
        return;
      }

      tokenExpiryTimer.current = window.setTimeout(() => {
        window.dispatchEvent(new Event('auth:expired'));
      }, msUntilExpiry);
    },
    [clearExpiryTimer]
  );

  const hardLogout = useCallback(() => {
    clearExpiryTimer();
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    window.dispatchEvent(new Event('auth:logout'));
  }, [clearExpiryTimer]);

  const ensureFreshAuthState = useCallback(() => {
    try {
      const savedUser = localStorage.getItem('user');
      const token = localStorage.getItem('token');

      if (!token || !savedUser) {
        hardLogout();
        return false;
      }

      const expMs = getTokenExpirationMs(token);
      if (!expMs || expMs <= Date.now()) {
        window.dispatchEvent(new Event('auth:expired'));
        return false;
      }

      const parsed = JSON.parse(savedUser);
      const normalized = { ...parsed, role: normalizeRole(parsed?. role) };
      setUser(normalized);
      scheduleExpiryCheck(token);
      return true;
    } catch {
      hardLogout();
      return false;
    }
  }, [hardLogout, scheduleExpiryCheck]);

  useEffect(() => {
    ensureFreshAuthState();
    setIsLoading(false);
  }, [ensureFreshAuthState]);

  useEffect(() => {
    ensureFreshAuthState();
  }, [ensureFreshAuthState, location]);

  useEffect(() => {
    const handler = () => ensureFreshAuthState();
    window.addEventListener('visibilitychange', handler);
    window.addEventListener('focus', handler);
    return () => {
      window.removeEventListener('visibilitychange', handler);
      window.removeEventListener('focus', handler);
    };
  }, [ensureFreshAuthState]);

  useEffect(() => () => clearExpiryTimer(), [clearExpiryTimer]);

  useEffect(() => {
    const handler = () => {
      hardLogout();
    };

    window.addEventListener('auth:expired', handler);
    return () => window.removeEventListener('auth:expired', handler);
  }, [hardLogout]);

  const login = useCallback(async (username, password) => {
    setIsLoading(true);
    setError('');
    try {
      const data = await authService.login(username, password);
      const userData = {
        username:  data.username,
        role: normalizeRole(data.role),
        avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(
          data.username
        )}&background=3b82f6&color=fff`,
      };

      localStorage.setItem('token', data.token);
      localStorage. setItem('user', JSON.stringify(userData));
      setUser(userData);
      scheduleExpiryCheck(data.token);

      // Fire login success event → CartContext refresh
      window.dispatchEvent(new Event('auth:login'));

      return userData;
    } catch (err) {
      setError(err. message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, [scheduleExpiryCheck]);

  const register = useCallback(async ({ username, email, password }) => {
    setIsLoading(true);
    setError('');
    try {
      const data = await authService.register({ username, email, password });
      const userData = {
        username:  data.username,
        role: normalizeRole(data.role),
        avatar: `https://ui-avatars.com/api/? name=${encodeURIComponent(
          data.username
        )}&background=3b82f6&color=fff`,
      };

      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON. stringify(userData));
      setUser(userData);
      scheduleExpiryCheck(data.token);

      // Phát event login thành công → CartContext refresh
      window.dispatchEvent(new Event('auth:login'));

      return userData;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, [scheduleExpiryCheck]);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      hardLogout();
    }
  }, [hardLogout]);

  const value = {
    user,
    isLoading,
    error,
    isAuthenticated: !!user,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}