import api from './axiosConfig';

export async function submitReview(reviewData) {
  const { data } = await api.post('/reviews', reviewData);
  return data;
}

export async function voteReview(reviewId, voteType) {
  const { data } = await api.post(`/reviews/${reviewId}/vote`, { voteType });
  return data;
}

export async function reportReview(reviewId, reason, description) {
  const { data } = await api.post(`/reviews/${reviewId}/report`, { reason, description });
  return data;
}
