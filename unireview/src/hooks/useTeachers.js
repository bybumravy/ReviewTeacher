import { useState, useEffect } from 'react';
import { getTeachers, getTeacherById } from '../api/teacherApi';

export function useTeachers(filters = {}) {
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchData() {
      setLoading(true);
      setError(null);
      try {
        const data = await getTeachers(filters);
        setTeachers(data.content || data);
      } catch (err) {
        setError(err.message || 'Không thể tải danh sách giảng viên');
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, [filters.search, filters.faculty, filters.minRating, filters.sortBy, filters.sortDir]);

  return { teachers, loading, error };
}

export function useTeacherDetails(id) {
  const [teacher, setTeacher] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!id) return;
    async function fetchData() {
      setLoading(true);
      setError(null);
      try {
        const data = await getTeacherById(id);
        setTeacher(data);
      } catch (err) {
        setError(err.message || 'Không tìm thấy giảng viên');
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [id]);

  return { teacher, loading, error, setTeacher };
}
