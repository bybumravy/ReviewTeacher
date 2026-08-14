import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTeacherDetails } from '../hooks/useTeachers';
import { useReviews } from '../hooks/useReviews';
import { useGate } from '../hooks/useGate';
import { FaStar } from 'react-icons/fa';
import { FiMessageSquare, FiUnlock, FiLock, FiPlusCircle } from 'react-icons/fi';
import MultiChoiceStats from '../components/teacher/MultiChoiceStats';
import ReviewCard from '../components/review/ReviewCard';
import ReviewBlurred from '../components/review/ReviewBlurred';
import GateModal from '../components/review/GateModal';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import './TeacherDetailPage.css';
import toast from 'react-hot-toast';

export default function TeacherDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { teacher, loading: teacherLoading } = useTeacherDetails(id);
  const { reviews, loading: reviewsLoading, refetch } = useReviews(id);
  const { isUnlocked, hasCredit, spendCredit } = useGate();

  const [showGateModal, setShowGateModal] = useState(false);

  const unlocked = isUnlocked(Number(id));

  const handleUnlockClick = () => {
    if (hasCredit) {
      spendCredit(Number(id));
      toast.success('Mở khóa thành công! 🔓');
      refetch();
    } else {
      setShowGateModal(true);
    }
  };

  if (teacherLoading) {
    return <LoadingSpinner size="lg" message="Đang tải thông tin giảng viên..." />;
  }

  if (!teacher) {
    return (
      <div className="container page">
        <EmptyState title="Không tìm thấy giảng viên" message="Giảng viên này không tồn tại hoặc đã bị xóa khỏi hệ thống." />
      </div>
    );
  }

  const ratingColor = teacher.avgRating >= 4 ? '#10b981' :
                      teacher.avgRating >= 3 ? '#f59e0b' :
                      teacher.avgRating >= 2 ? '#f97316' : '#ef4444';

  return (
    <div className="teacher-detail-page page container">
      {/* Header Profile Section */}
      <div className="teacher-profile-header card animate-slide-up">
        <div className="teacher-profile-avatar" style={{ background: `linear-gradient(135deg, ${ratingColor}33, ${ratingColor}11)` }}>
          <span>{teacher.fullName.charAt(0)}</span>
        </div>

        <div className="teacher-profile-info">
          <h1 className="teacher-profile-name">{teacher.title} {teacher.fullName}</h1>
          <p className="teacher-profile-sub">{teacher.faculty} {teacher.department ? `· ${teacher.department}` : ''}</p>

          <div className="teacher-profile-stats">
            <div className="profile-stat" style={{ color: ratingColor }}>
              <FaStar />
              <strong>{teacher.avgRating > 0 ? teacher.avgRating.toFixed(1) : '—'}</strong> / 5.0
            </div>
            <div className="profile-stat-divider"></div>
            <div className="profile-stat">
              <FiMessageSquare />
              <strong>{teacher.totalReviews}</strong> reviews
            </div>
          </div>
        </div>

        <div className="teacher-profile-actions">
          <button
            className="btn btn-primary"
            onClick={() => navigate(`/write-review?teacherId=${teacher.id}`)}
          >
            <FiPlusCircle /> Viết Review
          </button>
        </div>
      </div>

      {/* Grid: Stats and Reviews */}
      <div className="teacher-detail-grid">
        {/* Left column: Public aggregate stats */}
        <div className="teacher-detail-stats-col">
          <MultiChoiceStats stats={teacher.multipleChoiceStats} />
        </div>

        {/* Right column: Gated reviews list */}
        <div className="teacher-detail-reviews-col">
          <div className="reviews-section-header">
            <h2 className="reviews-section-title">Nhận xét từ sinh viên</h2>
            {unlocked ? (
              <span className="badge badge-success"><FiUnlock /> Đã mở khóa</span>
            ) : (
              <span className="badge badge-danger"><FiLock /> Đã khóa</span>
            )}
          </div>

          {reviewsLoading ? (
            <LoadingSpinner message="Đang tải danh sách reviews..." />
          ) : unlocked ? (
            reviews.length === 0 ? (
              <EmptyState
                title="Chưa có nhận xét nào"
                message="Giảng viên này chưa có nhận xét chi tiết. Hãy viết review đầu tiên!"
                icon={<FiMessageSquare />}
              />
            ) : (
              <div className="reviews-list">
                {reviews.map(review => (
                  <ReviewCard key={review.id} review={review} />
                ))}
              </div>
            )
          ) : (
            <ReviewBlurred
              teacherName={teacher.fullName}
              onUnlockClick={handleUnlockClick}
            />
          )}
        </div>
      </div>

      {/* Gate Modal */}
      {showGateModal && (
        <GateModal
          teacherName={teacher.fullName}
          onClose={() => setShowGateModal(false)}
        />
      )}
    </div>
  );
}
