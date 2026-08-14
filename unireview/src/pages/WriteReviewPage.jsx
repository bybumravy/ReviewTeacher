import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useTeachers } from '../hooks/useTeachers';
import ReviewForm from '../components/review/ReviewForm';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { FiChevronLeft, FiEdit } from 'react-icons/fi';
import './WriteReviewPage.css';

export default function WriteReviewPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const teacherId = searchParams.get('teacherId');

  const { teachers, loading } = useTeachers();
  const [selectedTeacher, setSelectedTeacher] = useState(null);

  useEffect(() => {
    if (teacherId && teachers.length > 0) {
      const found = teachers.find(t => t.id === Number(teacherId));
      setSelectedTeacher(found || null);
    }
  }, [teacherId, teachers]);

  const handleSelectTeacherChange = (e) => {
    const id = e.target.value;
    if (id) {
      const found = teachers.find(t => t.id === Number(id));
      setSelectedTeacher(found);
    } else {
      setSelectedTeacher(null);
    }
  };

  const handleSuccess = () => {
    if (selectedTeacher) {
      navigate(`/teachers/${selectedTeacher.id}`);
    } else {
      navigate('/teachers');
    }
  };

  return (
    <div className="write-review-page page container">
      {/* Header */}
      <div className="page-header write-review-header animate-slide-up">
        <button className="btn btn-ghost btn-sm back-btn" onClick={() => navigate(-1)}>
          <FiChevronLeft /> Quay lại
        </button>
        <h1 className="page-title write-title">
          <FiEdit /> {selectedTeacher ? `Đánh giá Giảng viên: ${selectedTeacher.title} ${selectedTeacher.fullName}` : 'Đánh giá Giảng viên'}
        </h1>
        <p className="page-subtitle">
          Chia sẻ đánh giá chân thực của bạn để mở khóa xem nhận xét của các giảng viên khác.
        </p>
      </div>

      {loading ? (
        <LoadingSpinner message="Đang tải danh sách giảng viên..." />
      ) : (
        <div className="write-review-content card animate-fade-in">
          {/* Teacher Selection Dropdown if not pre-selected */}
          {!teacherId && (
            <div className="teacher-select-group input-group">
              <label htmlFor="teacher-select">Chọn Giảng viên bạn muốn đánh giá *</label>
              <select
                id="teacher-select"
                className="select"
                onChange={handleSelectTeacherChange}
                value={selectedTeacher?.id || ''}
              >
                <option value="">-- Chọn giảng viên từ danh sách --</option>
                {teachers.map(t => (
                  <option key={t.id} value={t.id}>
                    {t.title} {t.fullName} ({t.faculty})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Review Form */}
          {selectedTeacher ? (
            <div className="form-wrapper animate-slide-up">
              <ReviewForm teacher={selectedTeacher} onSuccess={handleSuccess} />
            </div>
          ) : (
            <div className="select-prompt">
              <p>Vui lòng chọn một giảng viên để bắt đầu viết đánh giá.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
