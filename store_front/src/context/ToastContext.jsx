import { createContext, useContext, useCallback, useMemo, useRef, useState } from 'react';

const ToastContext = createContext(null);

const DEFAULT_DURATION = 3500;

function createToast({ type = 'info', title = '', message = '', duration = DEFAULT_DURATION }) {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    type, // 'success' | 'error' | 'info'
    title,
    message,
    duration,
    createdAt: Date.now(),
  };
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef(new Map());

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
    const timer = timersRef.current.get(id);
    if (timer) {
      clearTimeout(timer);
      timersRef.current.delete(id);
    }
  }, []);

  const pushToast = useCallback((toastInput) => {
    const toast = createToast(toastInput);
    setToasts((prev) => [toast, ...prev].slice(0, 5)); // limit 5 toasts

    if (toast.duration && toast.duration > 0) {
      const timer = setTimeout(() => removeToast(toast.id), toast.duration);
      timersRef.current.set(toast.id, timer);
    }

    return toast.id;
  }, [removeToast]);

  const api = useMemo(() => {
    return {
      toasts,
      removeToast,
      pushToast,
      success: (message, opts = {}) =>
        pushToast({ type: 'success', title: opts.title || 'Success', message, duration: opts.duration }),
      error: (message, opts = {}) =>
        pushToast({ type: 'error', title: opts.title || 'Error Occurred', message, duration: opts.duration }),
      info: (message, opts = {}) =>
        pushToast({ type: 'info', title: opts.title || 'Notification', message, duration: opts.duration }),
      clearAll: () => {
        // clear timers
        timersRef.current.forEach((timer) => clearTimeout(timer));
        timersRef.current.clear();
        setToasts([]);
      },
    };
  }, [pushToast, removeToast, toasts]);

  return <ToastContext.Provider value={api}>{children}</ToastContext.Provider>;
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
}