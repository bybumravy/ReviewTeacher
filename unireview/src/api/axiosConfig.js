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

// Fallback copy for error codes where the backend message might not be present/friendly
const ERROR_CODE_MESSAGES = {
  RATE_LIMIT_EXCEEDED: 'Bạn đã gửi quá nhiều review hôm nay. Vui lòng thử lại vào ngày mai.',
};

// Global error handler
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const code = error.response?.data?.error;
    const message = error.response?.data?.message || ERROR_CODE_MESSAGES[code] || 'Đã xảy ra lỗi. Vui lòng thử lại.';
    const errorData = {
      status: error.response?.status,
      code,
      message,
    };
    return Promise.reject(errorData);
  }
);

export default api;
