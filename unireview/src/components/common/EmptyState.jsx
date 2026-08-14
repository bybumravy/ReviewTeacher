import { FiInbox } from 'react-icons/fi';
import './EmptyState.css';

export default function EmptyState({ title = 'Không tìm thấy dữ liệu', message = 'Vui lòng kiểm tra lại bộ lọc hoặc thử từ khóa khác.', icon }) {
  return (
    <div className="empty-state card animate-fade-in">
      <div className="empty-state-icon">
        {icon || <FiInbox />}
      </div>
      <h3 className="empty-state-title">{title}</h3>
      <p className="empty-state-message">{message}</p>
    </div>
  );
}
