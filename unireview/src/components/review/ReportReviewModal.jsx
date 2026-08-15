import { useState } from 'react';
import { FiFlag } from 'react-icons/fi';
import './ReportReviewModal.css';

const REASONS = [
  'Nội dung xúc phạm, thô tục',
  'Thông tin sai sự thật',
  'Spam hoặc quảng cáo',
  'Vi phạm quyền riêng tư',
  'Khác',
];

export default function ReportReviewModal({ onClose, onSubmit }) {
  const [reason, setReason] = useState(REASONS[0]);
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await onSubmit({ reason, description });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content report-modal" onClick={(e) => e.stopPropagation()}>
        <div className="report-modal-icon">
          <FiFlag />
        </div>

        <h2 className="report-modal-title">Báo cáo review</h2>
        <p className="report-modal-desc">
          Cho chúng tôi biết vì sao review này không phù hợp. Admin sẽ xem xét báo cáo của bạn.
        </p>

        <form onSubmit={handleSubmit} className="report-modal-form">
          <div className="input-group">
            <label htmlFor="report-reason">Lý do *</label>
            <select
              id="report-reason"
              className="select"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            >
              {REASONS.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>

          <div className="input-group">
            <label htmlFor="report-description">Mô tả thêm (không bắt buộc)</label>
            <textarea
              id="report-description"
              className="textarea"
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Mô tả chi tiết vấn đề bạn gặp phải..."
            />
          </div>

          <div className="report-modal-actions">
            <button type="submit" className="btn btn-primary btn-lg" disabled={submitting}>
              {submitting ? 'Đang gửi...' : 'Gửi báo cáo'}
            </button>
            <button type="button" className="btn btn-ghost" onClick={onClose} disabled={submitting}>
              Hủy
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
