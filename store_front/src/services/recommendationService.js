import api from './api';

const recommendationService = {
  async getRecommendations(userId = 'feacb58b496bae6c38aa1b07651de21a', k = 10) {
    try {
      // api interceptor already unwraps response.data, so the result is the array directly
      const books = await api.get(`/recommendation/${userId}?k=${k}`);
      console.log('Recommendation API Result:', books);
      return Array.isArray(books) ? books : [];
    } catch (error) {
      console.error('Recommendation API error:', error);
      throw new Error(error.response?.data?.message || 'Unable to load recommendations.');
    }
  }
};

export default recommendationService;
