import { useGate } from '../../hooks/useGate';
import { FiZap, FiClock } from 'react-icons/fi';
import './CreditBadge.css';

export default function CreditBadge() {
  const { gate } = useGate();

  return (
    <div className="credit-badge-group">
      <div className="credit-badge" data-tooltip="Credits để xem review">
        <FiZap className="credit-icon" />
        <span className="credit-count">{gate.creditBalance}</span>
      </div>
      {gate.pendingReviews > 0 && (
        <div className="credit-badge pending" data-tooltip="Review đang chờ kiểm tra">
          <FiClock className="credit-icon" />
          <span className="credit-count">{gate.pendingReviews}</span>
        </div>
      )}
    </div>
  );
}
