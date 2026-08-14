import api from './axiosConfig';

export async function getGateStatus() {
  const { data } = await api.get('/gate/status');
  return data;
}
