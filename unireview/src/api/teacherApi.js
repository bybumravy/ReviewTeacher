import api from './axiosConfig';

export async function getTeachers({ search, faculty, minRating, sortBy, sortDir, page, size } = {}) {
  const params = {};
  if (search) params.search = search;
  if (faculty) params.faculty = faculty;
  if (minRating) params.minRating = minRating;
  if (sortBy) params.sortBy = sortBy;
  if (sortDir) params.sortDir = sortDir;
  if (page !== undefined) params.page = page;
  if (size) params.size = size;

  const { data } = await api.get('/teachers', { params });
  return data;
}

export async function getTeacherById(id) {
  const { data } = await api.get(`/teachers/${id}`);
  return data;
}

export async function getTeacherReviews(id) {
  const { data } = await api.get(`/teachers/${id}/reviews`);
  return data;
}
