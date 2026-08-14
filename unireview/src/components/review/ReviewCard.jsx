import { FaStar } from 'react-icons/fa';
import { FiThumbsUp, FiThumbsDown, FiFlag, FiClock } from 'react-icons/fi';
import StarRating from './StarRating';
import './ReviewCard.css';

const DIFFICULTY_LABELS = {
  VERY_EASY: 'Rất dễ', EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó', VERY_HARD: 'Rất khó'
};
const ATTENDANCE_LABELS = {
  NEVER: 'Không bao giờ', SOMETIMES: 'Thỉnh thoảng', OFTEN: 'Thường xuyên', STRICT: 'Rất gắt'
};
const MATERIALS_LABELS = { YES: 'Có', NO: 'Không', DEPENDS: 'Tùy kỳ thi' };
const RECOMMEND_LABELS = { YES: 'Có', NO: 'Không', MAYBE: 'Tùy' };
const WORKLOAD_LABELS = { LIGHT: 'Ít', MODERATE: 'Vừa phải', HEAVY: 'Nhiều', VERY_HEAVY: 'Rất nhiều' };

export default function ReviewCard({ review, onVote, onReport }) {
  return (
    <div className="review-card card animate-fade-in">
      <div className="review-card-header">
        <div className="review-meta">
          <span className="review-author">Ẩn danh</span>
          <span className="review-dot">·</span>
          <span className="review-semester">{review.semester}</span>
          {review.status === 'FLAGGED' && (
            <span className="badge badge-warning"><FiClock /> Đang kiểm tra</span>
          )}
        </div>
        <div className="review-overall-rating">
          <FaStar style={{ color: 'var(--color-star-filled)' }} />
          <span>{review.ratingOverall}/5</span>
        </div>
      </div>

      <div className="review-ratings-row">
        <div className="review-rating-item">
          <span className="review-rating-label">Giảng dạy</span>
          <StarRating value={review.ratingTeaching} readonly size={14} />
        </div>
        <div className="review-rating-item">
          <span className="review-rating-label">Chấm điểm</span>
          <StarRating value={review.ratingGrading} readonly size={14} />
        </div>
        <div className="review-rating-item">
          <span className="review-rating-label">Tính cách</span>
          <StarRating value={review.ratingPersonality} readonly size={14} />
        </div>
      </div>

      <div className="review-tags">
        <span className="review-tag">📚 {DIFFICULTY_LABELS[review.difficulty] || review.difficulty}</span>
        <span className="review-tag">📋 Điểm danh: {ATTENDANCE_LABELS[review.attendance] || review.attendance}</span>
        <span className="review-tag">📖 Tài liệu: {MATERIALS_LABELS[review.materialsAllowed] || review.materialsAllowed}</span>
        <span className="review-tag">📝 Bài tập: {WORKLOAD_LABELS[review.workload] || review.workload}</span>
        {review.wouldRecommend === 'YES' && <span className="review-tag recommend">👍 Recommend</span>}
        {review.wouldRecommend === 'NO' && <span className="review-tag not-recommend">👎 Không recommend</span>}
      </div>

      <p className="review-content">{review.content}</p>

      <div className="review-card-footer">
        <div className="review-actions">
          <button className="review-action-btn" onClick={() => onVote?.(review.id, 'UPVOTE')}>
            <FiThumbsUp /> <span>{review.upvoteCount || 0}</span>
          </button>
          <button className="review-action-btn" onClick={() => onVote?.(review.id, 'DOWNVOTE')}>
            <FiThumbsDown /> <span>{review.downvoteCount || 0}</span>
          </button>
        </div>
        <button className="review-action-btn report" onClick={() => onReport?.(review.id)}>
          <FiFlag /> Report
        </button>
      </div>
    </div>
  );
}
