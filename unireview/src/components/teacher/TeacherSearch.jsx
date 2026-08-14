import { useState } from 'react';
import { FiSearch, FiSliders } from 'react-icons/fi';
import './TeacherSearch.css';

const FACULTIES = [
  'Tất cả khoa',
  'Công nghệ thông tin',
  'Điện tử viễn thông',
  'Kinh tế & Quản lý',
  'Điện - Điện tử',
  'Cơ khí'
];

export default function TeacherSearch({ onFilterChange }) {
  const [search, setSearch] = useState('');
  const [faculty, setFaculty] = useState('');
  const [minRating, setMinRating] = useState('');
  const [sortBy, setSortBy] = useState('name');
  const [showFilters, setShowFilters] = useState(false);

  const handleSearchChange = (e) => {
    const val = e.target.value;
    setSearch(val);
    onFilterChange?.({ search: val, faculty, minRating, sortBy });
  };

  const handleFacultyChange = (e) => {
    const val = e.target.value === 'Tất cả khoa' ? '' : e.target.value;
    setFaculty(val);
    onFilterChange?.({ search, faculty: val, minRating, sortBy });
  };

  const handleRatingChange = (e) => {
    const val = e.target.value;
    setMinRating(val);
    onFilterChange?.({ search, faculty, minRating: val, sortBy });
  };

  const handleSortChange = (e) => {
    const val = e.target.value;
    setSortBy(val);
    onFilterChange?.({ search, faculty, minRating, sortBy: val });
  };

  return (
    <div className="teacher-search-wrapper">
      <div className="search-row">
        <div className="search-input-container">
          <FiSearch className="search-icon" />
          <input
            type="text"
            className="search-input-field"
            placeholder="Tìm theo tên giảng viên, môn học, bộ môn..."
            value={search}
            onChange={handleSearchChange}
          />
        </div>
        <button
          className={`btn btn-secondary filter-toggle-btn ${showFilters ? 'active' : ''}`}
          onClick={() => setShowFilters(!showFilters)}
        >
          <FiSliders /> Bộ lọc
        </button>
      </div>

      {showFilters && (
        <div className="filters-panel card animate-slide-up">
          <div className="filter-group">
            <label>Khoa</label>
            <select className="select" value={faculty || 'Tất cả khoa'} onChange={handleFacultyChange}>
              {FACULTIES.map(f => <option key={f} value={f}>{f}</option>)}
            </select>
          </div>

          <div className="filter-group">
            <label>Rating tối thiểu</label>
            <select className="select" value={minRating} onChange={handleRatingChange}>
              <option value="">Tất cả mức</option>
              <option value="4">⭐ 4+ sao</option>
              <option value="3">⭐ 3+ sao</option>
              <option value="2">⭐ 2+ sao</option>
            </select>
          </div>

          <div className="filter-group">
            <label>Sắp xếp theo</label>
            <select className="select" value={sortBy} onChange={handleSortChange}>
              <option value="name">Tên (A-Z)</option>
              <option value="rating">Rating cao nhất</option>
              <option value="reviews">Nhiều reviews nhất</option>
            </select>
          </div>
        </div>
      )}
    </div>
  );
}
