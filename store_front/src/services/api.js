import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  withCredentials: false,
});

// Dedupe expired auth notifications to avoid spam
let authExpiredNotified = false;
function notifyAuthExpired(info) {
  if (authExpiredNotified) return;
  authExpiredNotified = true;

  // Fire event for AuthContext to listen -> hardLogout
  window.dispatchEvent(new CustomEvent('auth:expired', { detail: info }));

  // After 2s allow receiving again (avoid spam)
  setTimeout(() => {
    authExpiredNotified = false;
  }, 2000);
}

// Attach token to Authorization header
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token'); // must match AuthContext
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Helper: normalize error from backend
function extractBackendError(error) {
  const status = error.response?.status;
  const data = error.response?.data || {};
  const code = data.code;
  const backendMessage = data.message;

  return {
    status,
    code,
    backendMessage,
    path: data.path,
  };
}

// Response interceptor
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const { status } = error.response || {};
    const info = extractBackendError(error);

    if (status === 401) {
      notifyAuthExpired(info);
      console.error('Authentication failed / token expired');
    } else if (status === 403) {
      console.error('Access denied');
    }

    // Gắn thông tin đã chuẩn hoá lên error để các service dùng
    error.customError = info;
    return Promise.reject(error);
  }
);

export default api;