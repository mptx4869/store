import api from './api';

function buildAuthError(error) {
  const { status, code, backendMessage } = error.customError || {};
  const fallback = 'An unexpected error occurred. Please try again.';

  if (backendMessage) {
    return new Error(backendMessage);
  }

  // LOGIN + REGISTER
  if (status === 400 && code === 'VALIDATION_ERROR') {
    return new Error('Invalid input. Please check and try again.');
  }

  if (status === 401 && code === 'AUTHENTICATION_FAILED') {
    return new Error('Incorrect username or password, or the account is not activated.');
  }

  if (status === 409 && code === 'BUSINESS_RULE_VIOLATION') {
    // Depending on backend message, but if not available use common message
    return new Error('Username or email already exists. Please use a different value.');
  }

  return new Error(fallback);
}

const authService = {
  async login(username, password) {
    try {
      const data = await api.post('/login', { username, password });
      return data; // { username, role, token }
    } catch (error) {
      throw buildAuthError(error);
    }
  },

  async register({ username, password, email }) {
    try {
      const data = await api.post('/register', {
        username,
        password,
        email,
        roleName: 'CUSTOMER',
      });
      return data;
    } catch (error) {
      throw buildAuthError(error);
    }
  },

  async logout() {
    return Promise.resolve();
  },

  async changePassword(oldPassword, newPassword) {
    try {
      await api.post('/users/change-password', { oldPassword, newPassword });
    } catch (error) {
      throw buildAuthError(error);
    }
  },
};

export default authService;