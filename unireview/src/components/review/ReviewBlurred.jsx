import { FiLock, FiEdit3 } from 'react-icons/fi';
import './ReviewBlurred.css';

export default function ReviewBlurred({ teacherName, onUnlockClick }) {
  // Mock blurred reviews text to simulate a blurred list behind the card
  const mockBlurredData = [
    { id: 1, length: 'short' },
    { id: 2, length: 'long' },
    { id: 3, length: 'medium' }
  ];

  return (
    <div className="reviews-blurred-container">
      {/* Blurred background reviews */}
      <div className="blurred-list content-blurred">
        {mockBlurredData.map(item => (
          <div key={item.id} className="blurred-card card">
            <div className="blurred-card-header">
              <div className="blurred-meta"></div>
              <div className="blurred-rating"></div>
            </div>
            <div className="blurred-line title"></div>
            <div className={`blurred-line content ${item.length}`}></div>
            <div className="blurred-line footer"></div>
          </div>
        ))}
      </div>

      {/* Unlock CTA overlay */}
      <div className="lock-overlay animate-fade-in">
        <div className="lock-card glass-panel animate-slide-up">
          <div className="lock-icon-wrapper">
            <FiLock />
          </div>
          <h3 className="lock-title">Nội dung đã khóa</h3>
          <p className="lock-desc">
            Để xem nhận xét chi tiết về <strong>{teacherName}</strong>, bạn cần viết 1 review giảng viên khác trước.
          </p>
          <button className="btn btn-primary btn-lg" onClick={onUnlockClick}>
            <FiEdit3 /> Viết Review để mở khóa
          </button>
        </div>
      </div>
    </div>
  );
}
