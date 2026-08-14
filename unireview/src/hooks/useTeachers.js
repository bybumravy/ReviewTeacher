import { useState, useEffect } from 'react';
import { getTeachers, getTeacherById } from '../api/teacherApi';
import { MOCK_TEACHERS } from '../api/mockData';

export function useTeachers(filters = {}) {
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchData() {
      setLoading(true);
      try {
        // In production:
        // const data = await getTeachers(filters);
        // setTeachers(data.content || data);

        // Offline mock implementation of filters
        let result = [...MOCK_TEACHERS];

        if (filters.search) {
          const query = filters.search.toLowerCase();
          result = result.filter(t =>
            t.fullName.toLowerCase().includes(query) ||
            t.faculty.toLowerCase().includes(query) ||
            (t.department && t.department.toLowerCase().includes(query))
          );
        }

        if (filters.faculty) {
          result = result.filter(t => t.faculty === filters.faculty);
        }

        if (filters.minRating) {
          result = result.filter(t => t.avgRating >= Number(filters.minRating));
        }

        if (filters.sortBy) {
          result.sort((a, b) => {
            if (filters.sortBy === 'rating') return b.avgRating - a.avgRating;
            if (filters.sortBy === 'reviews') return b.totalReviews - a.totalReviews;
            return a.fullName.localeCompare(b.fullName);
          });
        }

        // Simulate network delay
        await new Promise(resolve => setTimeout(resolve, 300));
        setTeachers(result);
      } catch (err) {
        setError(err.message || 'Không thể tải danh sách giảng viên');
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, [filters.search, filters.faculty, filters.minRating, filters.sortBy]);

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
      try {
        // In production:
        // const data = await getTeacherById(id);
        // setTeacher(data);

        const found = MOCK_TEACHERS.find(t => t.id === Number(id));
        if (!found) throw new Error('Không tìm thấy giảng viên');

        await new Promise(resolve => setTimeout(resolve, 200));
        setTeacher(found);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [id]);

  return { teacher, loading, error, setTeacher };
}
