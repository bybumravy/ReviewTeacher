import './LoadingSpinner.css';

export default function LoadingSpinner({ size = 'md', message = 'Đang tải dữ liệu...' }) {
  return (
    <div className="spinner-wrapper">
      <div className={`spinner spinner-${size}`} role="status">
        <span className="sr-only">Loading...</span>
      </div>
      {message && <p className="spinner-message">{message}</p>}
    </div>
  );
}
