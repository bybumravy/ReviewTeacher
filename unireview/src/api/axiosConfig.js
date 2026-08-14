import axios from 'axios';
import { getReviewerToken } from '../utils/cookie';

const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach reviewer token to every request
api.interceptors.request.use((config) => {
  const token = getReviewerToken();
  if (token) {
    config.headers['X-Reviewer-Token'] = token;
  }
  return config;
});

// Global error handler
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || 'Đã xảy ra lỗi. Vui lòng thử lại.';
    const errorData = {
      status: error.response?.status,
      code: error.response?.data?.error,
      message,
    };
    return Promise.reject(errorData);
  }
);

export default api;
