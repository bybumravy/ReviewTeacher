import { Link } from 'react-router-dom';
import { FaStar } from 'react-icons/fa';
import { FiMessageSquare, FiLock, FiUnlock } from 'react-icons/fi';
import { useGate } from '../../hooks/useGate';
import './TeacherCard.css';

export default function TeacherCard({ teacher }) {
  const { isUnlocked } = useGate();
  const unlocked = isUnlocked(teacher.id);

  const ratingColor = teacher.avgRating >= 4 ? '#10b981' :
                      teacher.avgRating >= 3 ? '#f59e0b' :
                      teacher.avgRating >= 2 ? '#f97316' : '#ef4444';

  return (
    <Link to={`/teachers/${teacher.id}`} className="teacher-card card card-interactive">
      <div className="teacher-card-header">
        <div className="teacher-avatar" style={{ background: `linear-gradient(135deg, ${ratingColor}33, ${ratingColor}11)` }}>
          <span className="teacher-avatar-letter">{teacher.fullName.charAt(0)}</span>
        </div>
        <div className="teacher-card-lock">
          {unlocked ? (
            <FiUnlock className="lock-icon unlocked" />
          ) : (
            <FiLock className="lock-icon locked" />
          )}
        </div>
      </div>

      <div className="teacher-card-body">
        <h3 className="teacher-card-name">{teacher.title} {teacher.fullName}</h3>
        <p className="teacher-card-faculty">{teacher.faculty}</p>
        {teacher.department && (
          <p className="teacher-card-dept">{teacher.department}</p>
        )}
      </div>

      <div className="teacher-card-footer">
        <div className="teacher-card-rating" style={{ color: ratingColor }}>
          <FaStar />
          <span>{teacher.avgRating > 0 ? teacher.avgRating.toFixed(1) : '—'}</span>
        </div>
        <div className="teacher-card-reviews">
          <FiMessageSquare />
          <span>{teacher.totalReviews} reviews</span>
        </div>
      </div>
    </Link>
  );
}
