import api from './axiosConfig';

export async function submitReview(reviewData) {
  const { data } = await api.post('/reviews', reviewData);
  return data;
}

export async function voteReview(reviewId, voteType, captchaToken) {
  const { data } = await api.post(`/reviews/${reviewId}/vote`, { voteType, captchaToken });
  return data;
}

export async function reportReview(reviewId, reason, description, captchaToken) {
  const { data } = await api.post(`/reviews/${reviewId}/report`, { reason, description, captchaToken });
  return data;
}
