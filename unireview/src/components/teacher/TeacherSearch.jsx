import { useState } from 'react';
import { FiSearch, FiSliders, FiArrowUp, FiArrowDown } from 'react-icons/fi';
import './TeacherSearch.css';

const FACULTIES = [
  'Tất cả khoa',
  'Công nghệ thông tin',
  'Điện tử viễn thông',
  'Kinh tế & Quản lý',
  'Điện - Điện tử',
  'Cơ khí'
];

const DEFAULT_SORT_DIR = {
  name: 'asc',
  rating: 'desc',
  reviews: 'desc',
};

export default function TeacherSearch({ onFilterChange }) {
  const [search, setSearch] = useState('');
  const [faculty, setFaculty] = useState('');
  const [minRating, setMinRating] = useState('');
  const [sortBy, setSortBy] = useState('name');
  const [sortDir, setSortDir] = useState(DEFAULT_SORT_DIR.name);
  const [showFilters, setShowFilters] = useState(false);

  const emitChange = (next) => {
    onFilterChange?.({ search, faculty, minRating, sortBy, sortDir, ...next });
  };

  const handleSearchChange = (e) => {
    const val = e.target.value;
    setSearch(val);
    emitChange({ search: val });
  };

  const handleFacultyChange = (e) => {
    const val = e.target.value === 'Tất cả khoa' ? '' : e.target.value;
    setFaculty(val);
    emitChange({ faculty: val });
  };

  const handleRatingChange = (e) => {
    const val = e.target.value;
    setMinRating(val);
    emitChange({ minRating: val });
  };

  const handleSortChange = (e) => {
    const val = e.target.value;
    const nextDir = DEFAULT_SORT_DIR[val] || 'asc';
    setSortBy(val);
    setSortDir(nextDir);
    emitChange({ sortBy: val, sortDir: nextDir });
  };

  const toggleSortDir = () => {
    const nextDir = sortDir === 'asc' ? 'desc' : 'asc';
    setSortDir(nextDir);
    emitChange({ sortDir: nextDir });
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
            <div className="sort-control-row">
              <select className="select" value={sortBy} onChange={handleSortChange}>
                <option value="name">Tên</option>
                <option value="rating">Rating</option>
                <option value="reviews">Số reviews</option>
              </select>
              <button
                type="button"
                className="btn btn-secondary sort-dir-btn"
                onClick={toggleSortDir}
                title={sortDir === 'asc' ? 'Tăng dần' : 'Giảm dần'}
                aria-label={sortDir === 'asc' ? 'Sắp xếp tăng dần' : 'Sắp xếp giảm dần'}
              >
                {sortDir === 'asc' ? <FiArrowUp /> : <FiArrowDown />}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
