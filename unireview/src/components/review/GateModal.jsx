import { useNavigate } from 'react-router-dom';
import { FiLock, FiEdit3, FiZap } from 'react-icons/fi';
import './GateModal.css';

export default function GateModal({ teacherName, onClose }) {
  const navigate = useNavigate();

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content gate-modal" onClick={(e) => e.stopPropagation()}>
        <div className="gate-modal-icon">
          <FiLock />
        </div>

        <h2 className="gate-modal-title">Mở khóa Reviews</h2>

        <p className="gate-modal-desc">
          Để xem reviews của <strong>{teacherName}</strong>, bạn cần viết 1 review cho giảng viên khác trước.
        </p>

        <div className="gate-modal-steps">
          <div className="gate-step">
            <div className="gate-step-num">1</div>
            <div>
              <strong>Viết review</strong>
              <p>Chọn 1 giảng viên bạn đã học và viết đánh giá</p>
            </div>
          </div>
          <div className="gate-step">
            <div className="gate-step-num">2</div>
            <div>
              <strong>Nhận credit</strong>
              <p>Review qua kiểm tra AI → nhận 1 credit ngay</p>
            </div>
          </div>
          <div className="gate-step">
            <div className="gate-step-num">3</div>
            <div>
              <strong>Mở khóa</strong>
              <p>Dùng credit để xem reviews của giảng viên bạn muốn</p>
            </div>
          </div>
        </div>

        <div className="gate-modal-actions">
          <button
            className="btn btn-primary btn-lg"
            onClick={() => navigate('/write-review')}
          >
            <FiEdit3 /> Viết Review Ngay
          </button>
          <button className="btn btn-ghost" onClick={onClose}>
            Để sau
          </button>
        </div>
      </div>
    </div>
  );
}
