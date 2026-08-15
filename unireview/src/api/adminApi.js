import axios from 'axios';

const ADMIN_TOKEN_KEY = 'admin_jwt';

export function getAdminToken() {
  return sessionStorage.getItem(ADMIN_TOKEN_KEY);
}

export function setAdminToken(token) {
  sessionStorage.setItem(ADMIN_TOKEN_KEY, token);
}

export function clearAdminToken() {
  sessionStorage.removeItem(ADMIN_TOKEN_KEY);
}

const adminApi = axios.create({
  baseURL: '/api/admin',
  headers: {
    'Content-Type': 'application/json',
  },
});

adminApi.interceptors.request.use((config) => {
  const token = getAdminToken();
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

adminApi.interceptors.response.use(
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

export async function login(username, password) {
  const { data } = await adminApi.post('/login', { username, password });
  return data;
}

export async function getFlaggedReviews(page = 0, size = 10) {
  const { data } = await adminApi.get('/reviews/flagged', { params: { page, size } });
  return data;
}

export async function approveReview(reviewId) {
  const { data } = await adminApi.put(`/reviews/${reviewId}/approve`);
  return data;
}

export async function rejectReview(reviewId) {
  const { data } = await adminApi.put(`/reviews/${reviewId}/reject`);
  return data;
}

export async function hideReview(reviewId) {
  const { data } = await adminApi.put(`/reviews/${reviewId}/hide`);
  return data;
}

export async function getReports(status = 'PENDING', page = 0, size = 10) {
  const { data } = await adminApi.get('/reports', { params: { status, page, size } });
  return data;
}

export async function dismissReport(reportId) {
  const { data } = await adminApi.put(`/reports/${reportId}/dismiss`);
  return data;
}

export async function importTeachersCsv(file) {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await adminApi.post('/teachers/import-csv', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}
