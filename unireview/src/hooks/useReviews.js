import { useState, useEffect, useCallback } from 'react';
import { getTeacherReviews } from '../api/teacherApi';
import { MOCK_REVIEWS } from '../api/mockData';
import { useGate } from './useGate';
import toast from 'react-hot-toast';

export function useReviews(teacherId) {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { isUnlocked, hasCredit, spendCredit } = useGate();

  const fetchReviews = useCallback(async () => {
    if (!teacherId) return;
    setLoading(true);
    setError(null);
    try {
      const unlocked = isUnlocked(Number(teacherId));

      if (!unlocked) {
        // If not unlocked and has credit, let's auto-spend credit to unlock
        if (hasCredit) {
          spendCredit(Number(teacherId));
          toast.success('Đã tự động sử dụng 1 credit để mở khóa reviews! 🔓');
        } else {
          // Locked and no credit
          setReviews([]);
          setLoading(false);
          return;
        }
      }

      // In production:
      // const data = await getTeacherReviews(teacherId);
      // setReviews(data);

      // Offline mock data
      const mockReviews = MOCK_REVIEWS[Number(teacherId)] || [];
      await new Promise(resolve => setTimeout(resolve, 200));
      setReviews(mockReviews);
    } catch (err) {
      setError(err.message || 'Không thể tải reviews');
    } finally {
      setLoading(false);
    }
  }, [teacherId, isUnlocked, hasCredit, spendCredit]);

  useEffect(() => {
    fetchReviews();
  }, [fetchReviews]);

  return { reviews, loading, error, refetch: fetchReviews };
}
