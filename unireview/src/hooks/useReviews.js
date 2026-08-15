import { useState, useCallback, useEffect } from 'react';
import { getTeacherReviews } from '../api/teacherApi';
import { useGate } from './useGate';

const LOCKED_ERROR_CODES = new Set(['INSUFFICIENT_CREDIT', 'NO_REVIEWER_TOKEN']);

export function useReviews(teacherId) {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [locked, setLocked] = useState(false);
  const { refresh } = useGate();

  const fetchReviews = useCallback(async () => {
    if (!teacherId) return;
    setLoading(true);
    setError(null);
    setLocked(false);
    try {
      const data = await getTeacherReviews(teacherId);
      setReviews(data);
      await refresh();
    } catch (err) {
      if (LOCKED_ERROR_CODES.has(err.code)) {
        setLocked(true);
        setReviews([]);
      } else {
        setError(err);
        setReviews([]);
      }
    } finally {
      setLoading(false);
    }
  }, [teacherId, refresh]);

  useEffect(() => {
    fetchReviews();
  }, [fetchReviews]);

  return { reviews, loading, error, locked, refetch: fetchReviews };
}
