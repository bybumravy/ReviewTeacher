import { useState } from 'react';
import { useTeachers } from '../hooks/useTeachers';
import TeacherSearch from '../components/teacher/TeacherSearch';
import TeacherCard from '../components/teacher/TeacherCard';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';

export default function TeacherListPage() {
  const [filters, setFilters] = useState({
    search: '',
    faculty: '',
    minRating: '',
    sortBy: 'name',
    sortDir: 'asc'
  });

  const { teachers, loading } = useTeachers(filters);

  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
  };

  return (
    <div className="teacher-list-page page container">
      <div className="page-header animate-slide-up">
        <h1 className="page-title">Danh sách Giảng viên</h1>
        <p className="page-subtitle">
          Tìm kiếm giảng viên theo tên, khoa, bộ môn hoặc lọc theo đánh giá
        </p>
      </div>

      <div className="animate-slide-up">
        <TeacherSearch onFilterChange={handleFilterChange} />
      </div>

      {loading ? (
        <LoadingSpinner message="Đang tải danh sách giảng viên..." />
      ) : teachers.length === 0 ? (
        <EmptyState
          title="Không tìm thấy giảng viên nào"
          message="Vui lòng thử tìm kiếm lại bằng từ khóa khác hoặc xóa bộ lọc hiện tại."
        />
      ) : (
        <div className="grid grid-cols-4 animate-fade-in">
          {teachers.map(teacher => (
            <TeacherCard key={teacher.id} teacher={teacher} />
          ))}
        </div>
      )}
    </div>
  );
}
