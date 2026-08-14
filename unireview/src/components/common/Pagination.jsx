import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import './Pagination.css';

export default function Pagination({ currentPage = 0, totalPages = 1, onPageChange }) {
  if (totalPages <= 1) return null;

  return (
    <div className="pagination">
      <button
        className="pagination-btn"
        disabled={currentPage === 0}
        onClick={() => onPageChange?.(currentPage - 1)}
        aria-label="Trang trước"
      >
        <FiChevronLeft />
      </button>

      <span className="pagination-info">
        Trang <strong>{currentPage + 1}</strong> / {totalPages}
      </span>

      <button
        className="pagination-btn"
        disabled={currentPage === totalPages - 1}
        onClick={() => onPageChange?.(currentPage + 1)}
        aria-label="Trang sau"
      >
        <FiChevronRight />
      </button>
    </div>
  );
}
