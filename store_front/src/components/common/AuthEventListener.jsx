import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useToast } from '../../context/ToastContext';

function AuthEventListener() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const lastHandledAtRef = useRef(0);

  useEffect(() => {
    const handler = (event) => {
      const now = Date.now();
      if (now - lastHandledAtRef.current < 1500) return; // dedupe
      lastHandledAtRef.current = now;

      const isExpired = event?.type === 'auth:expired';
      const message = isExpired
        ? 'Your session has expired. Please sign in again.'
        : 'Please sign in to continue.';
      const title = isExpired ? 'Session expired' : 'Sign in required';

      toast.info(message, { title });

      navigate('/login', {
        replace: true,
        state: {
          from: location,
          message,
        },
      });
    };

    window.addEventListener('auth:expired', handler);
    window.addEventListener('auth:login-required', handler);
    return () => {
      window.removeEventListener('auth:expired', handler);
      window.removeEventListener('auth:login-required', handler);
    };
  }, [location, navigate, toast]);

  return null;
}

export default AuthEventListener;